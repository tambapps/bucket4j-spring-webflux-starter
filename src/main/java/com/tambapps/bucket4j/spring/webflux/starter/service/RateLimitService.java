package com.tambapps.bucket4j.spring.webflux.starter.service;

import com.tambapps.bucket4j.spring.webflux.starter.properties.BandWidth;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimit;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimitMatchingStrategy;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class RateLimitService {

  public static final Long NO_LIMIT = Long.MIN_VALUE;

  private final ExpressionParser webfluxFilterExpressionParser;
  private final ProxyManager<String> buckets;
  private final Bucket4JWebfluxProperties properties;

  public RateLimitService(
      ExpressionParser webfluxFilterExpressionParser,
      ProxyManager<String> buckets,
      Bucket4JWebfluxProperties properties) {
    this.webfluxFilterExpressionParser = webfluxFilterExpressionParser;
    this.buckets = buckets;
    this.properties = properties;
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
    String key = getKey(bucket4JConfiguration, rateLimit, request);
    // TODO prepare this in a configuration bean
    BucketConfiguration bucketConfiguration = prepareBucket4jConfigurationBuilder(rateLimit).build();

    AsyncBucketProxy asyncBucketProxy = buckets.asAsync().builder().build(key, bucketConfiguration);
    if (nTokens > 0) {
      return Mono.fromFuture(asyncBucketProxy.tryConsumeAndReturnRemaining(nTokens))
          .map(probe -> probe.isConsumed() ? probe.getRemainingTokens() : NO_LIMIT);
    } else {
      return Mono.fromFuture(asyncBucketProxy.getAvailableTokens());
    }
  }

  // TODO prepare this in a configuration bean
  private ConfigurationBuilder prepareBucket4jConfigurationBuilder(RateLimit rl) {
    ConfigurationBuilder configBuilder = BucketConfiguration.builder();
    for (BandWidth bandWidth : rl.getBandwidths()) {
      Bandwidth bucket4jBandWidth = Bandwidth.simple(bandWidth.getCapacity(), Duration.of(bandWidth.getTime(), bandWidth.getUnit()));
      if(bandWidth.getFixedRefillInterval() > 0) {
        bucket4jBandWidth = Bandwidth.classic(bandWidth.getCapacity(), Refill.intervally(bandWidth.getCapacity(), Duration.of(bandWidth.getFixedRefillInterval(), bandWidth.getFixedRefillIntervalUnit())));
      }
      configBuilder = configBuilder.addLimit(bucket4jBandWidth);
    };
    return configBuilder;
  }


  private String getKey(Bucket4JConfiguration bucket4JConfiguration, RateLimit rateLimit, ServerHttpRequest request) {
    Expression expr = webfluxFilterExpressionParser.parseExpression(rateLimit.getExpression());
    StandardEvaluationContext context = new StandardEvaluationContext();
    // TODO allow to configure root object
    final String value = expr.getValue(context, request, String.class);
    return bucket4JConfiguration.getUrl() + "-" + value;
  }
}
