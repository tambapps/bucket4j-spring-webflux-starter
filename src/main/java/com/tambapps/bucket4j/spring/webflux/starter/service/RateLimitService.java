package com.tambapps.bucket4j.spring.webflux.starter.service;

import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimit;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimitMatchingStrategy;
import com.tambapps.bucket4j.spring.webflux.starter.util.ExpressionConfigurer;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RateLimitService {

  public static final Long NO_LIMIT = Long.MIN_VALUE;

  private final ExpressionParser webfluxFilterExpressionParser;
  private final ProxyManager<String> buckets;
  private final Bucket4JWebfluxProperties properties;
  private final Map<RateLimit, BucketConfiguration> rateLimitBucketConfigurationMap;
  private final Optional<ExpressionConfigurer> optExpressionConfigurer;

  public RateLimitService(
      ExpressionParser webfluxFilterExpressionParser,
      ProxyManager<String> buckets,
      Bucket4JWebfluxProperties properties,
      Map<RateLimit, BucketConfiguration> rateLimitBucketConfigurationMap,
      Optional<ExpressionConfigurer> optExpressionConfigurer) {
    this.webfluxFilterExpressionParser = webfluxFilterExpressionParser;
    this.buckets = buckets;
    this.properties = properties;
    this.rateLimitBucketConfigurationMap = rateLimitBucketConfigurationMap;
    this.optExpressionConfigurer = optExpressionConfigurer;
  }

  public Mono<Long> getRemaining(ServerHttpRequest request) {
    return consume(request, 0);
  }

  public Mono<Long> consume(ServerHttpRequest request, int nTokens) {
    for (Bucket4JConfiguration configuration : properties.getFilters()) {
      if (request.getURI().getPath().matches(configuration.getUrl())) {
        return consume(configuration, request, nTokens);
      }
    }
    return Mono.just(NO_LIMIT);
  }

  public Mono<Long> getRemaining(Bucket4JConfiguration bucket4JConfiguration,
      ServerHttpRequest request) {
    return consume(bucket4JConfiguration, request, 0);
  }

  public Mono<Long> consume(Bucket4JConfiguration bucket4JConfiguration,
      ServerHttpRequest request, int nTokens) {
    List<Mono<Long>> monos = bucket4JConfiguration.getRateLimits()
        .stream()
        .map(rateLimit -> consume(bucket4JConfiguration, rateLimit, request, nTokens))
        .collect(Collectors.toList());

    return Flux.concat(monos)
        .reduce((remaining1, remaining2) -> {
          if (NO_LIMIT.equals(remaining1)) {
            return remaining2;
          }
          if (NO_LIMIT.equals(remaining2)) {
            return remaining1;
          }
          // if we've reached this point, we know both remaining1 and remaining2 have limit
          if (RateLimitMatchingStrategy.FIRST.equals(bucket4JConfiguration.getStrategy())) {
            return remaining1;
          }
          return remaining1 < remaining2 ? remaining1 : remaining2;
        })
        .switchIfEmpty(Mono.just(NO_LIMIT));

  }
  private Mono<Long> consume(Bucket4JConfiguration bucket4JConfiguration, RateLimit rateLimit,
      ServerHttpRequest request, int nTokens) {
    Mono<String> keyMono = getKey(bucket4JConfiguration.getUrl(), rateLimit, request);
    BucketConfiguration bucketConfiguration = rateLimitBucketConfigurationMap.get(rateLimit);


    return keyMono.flatMap(key -> doConsume(key, bucketConfiguration, nTokens));
  }

  private Mono<Long> doConsume(String key, BucketConfiguration bucketConfiguration, int nTokens) {
    if (buckets.isAsyncModeSupported()) {
      AsyncBucketProxy asyncBucketProxy = buckets.asAsync().builder().build(key, bucketConfiguration);
      if (nTokens > 0) {
        return Mono.fromFuture(asyncBucketProxy.tryConsumeAndReturnRemaining(nTokens))
            .map(probe -> probe.isConsumed() ? probe.getRemainingTokens() : NO_LIMIT);
      } else {
        return Mono.fromFuture(asyncBucketProxy.getAvailableTokens());
      }
    } else {
      BucketProxy bucketProxy = buckets.builder().build(key, bucketConfiguration);
      ConsumptionProbe probe = bucketProxy.tryConsumeAndReturnRemaining(nTokens);
      if (nTokens > 0) {
        return Mono.just(probe.isConsumed() ? probe.getRemainingTokens() : NO_LIMIT);
      } else {
        return Mono.just(probe.getRemainingTokens());
      }
    }
  }

  private Mono<String> getKey(String url, RateLimit rateLimit, ServerHttpRequest request) {
    Expression expr = webfluxFilterExpressionParser.parseExpression(rateLimit.getExpression());
    StandardEvaluationContext context = new StandardEvaluationContext();
    Mono<Object> rootObjectMono = optExpressionConfigurer.map(expressionConfigurer -> expressionConfigurer.configure(context, request))
        .orElse(Mono.just(request));
    return rootObjectMono.map(rootObject -> evaluateKey(expr, context, url, rootObject));
  }

  private String evaluateKey(Expression expr, StandardEvaluationContext context, String url, Object rootObject) {
    final String value = expr.getValue(context, rootObject, String.class);
    return url + "-" + value;
  }
}
