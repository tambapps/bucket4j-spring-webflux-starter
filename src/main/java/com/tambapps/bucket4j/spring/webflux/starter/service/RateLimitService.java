package com.tambapps.bucket4j.spring.webflux.starter.service;

import com.tambapps.bucket4j.spring.webflux.starter.model.ConsumptionResult;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimit;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimitMatchingStrategy;
import com.tambapps.bucket4j.spring.webflux.starter.util.CacheResolver;
import com.tambapps.bucket4j.spring.webflux.starter.util.ExpressionConfigurer;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RateLimitService {

  public static final Long NO_LIMIT = Long.MIN_VALUE;

  private final CacheResolver cacheResolver;
  private final Bucket4JWebfluxProperties properties;
  private final Map<RateLimit, BucketConfiguration> rateLimitBucketConfigurationMap;
  private final Optional<ExpressionConfigurer> optExpressionConfigurer;

  public RateLimitService(
      CacheResolver cacheResolver,
      Bucket4JWebfluxProperties properties,
      Map<RateLimit, BucketConfiguration> rateLimitBucketConfigurationMap,
      Optional<ExpressionConfigurer> optExpressionConfigurer) {
    this.cacheResolver = cacheResolver;
    this.properties = properties;
    this.rateLimitBucketConfigurationMap = rateLimitBucketConfigurationMap;
    this.optExpressionConfigurer = optExpressionConfigurer;
  }

  public Mono<Long> getRemaining(ServerHttpRequest request) {
    return consume(request, 0).map(ConsumptionResult::getRemainingTokens);
  }

  public Mono<ConsumptionResult> consume(ServerHttpRequest request, int nTokens) {
    for (Bucket4JConfiguration configuration : properties.getFilters()) {
      if (request.getURI().getPath().matches(configuration.getUrl())) {
        return consume(configuration, request, nTokens);
      }
    }
    return Mono.just(ConsumptionResult.notConsumed());
  }

  public Mono<Long> getRemaining(Bucket4JConfiguration bucket4JConfiguration,
      ServerHttpRequest request) {
    return consume(bucket4JConfiguration, request, 0).map(ConsumptionResult::getRemainingTokens);
  }

  public Mono<ConsumptionResult> consume(Bucket4JConfiguration bucket4JConfiguration,
      ServerHttpRequest request, int nTokens) {
    List<Mono<ConsumptionResult>> monos = bucket4JConfiguration.getRateLimits()
        .stream()
        .map(rateLimit -> consume(bucket4JConfiguration, rateLimit, request, nTokens))
        .collect(Collectors.toList());

    return Flux.concat(monos)
        .reduce((result1, result2) -> {
          if (result1.getType() == ConsumptionResult.Type.NOT_CONSUMED) {
            return result2;
          }
          if (result2.getType() == ConsumptionResult.Type.NOT_CONSUMED) {
            return result1;
          }
          // if we've reached this point, we know both remaining1 and remaining2 have limit
          if (RateLimitMatchingStrategy.FIRST.equals(bucket4JConfiguration.getStrategy())) {
            return result1;
          }
          return result1.getRemainingTokens() < result2.getRemainingTokens() ? result1 : result2;
        })
        .switchIfEmpty(Mono.just(ConsumptionResult.notConsumed()));

  }
  private Mono<ConsumptionResult> consume(Bucket4JConfiguration bucket4JConfiguration, RateLimit rateLimit,
      ServerHttpRequest request, int nTokens) {
    Mono<Boolean> executeMono;

    if (rateLimit.getSkipCondition() != null && rateLimit.getExecuteCondition() != null) {
      executeMono = Mono.zip(executeCondition(rateLimit.getSkipCondition(), request),
          executeCondition(rateLimit.getExecuteCondition(), request))
          .map(bi ->
              // need to negate the skip
              !bi.getT1() && bi.getT2());
    } else if (rateLimit.getSkipCondition() != null) {
      executeMono = executeCondition(rateLimit.getSkipCondition(), request).map(skip -> !skip);
    } else if (rateLimit.getExecuteCondition() != null) {
      executeMono = executeCondition(rateLimit.getExecuteCondition(), request);
    } else {
      executeMono = Mono.just(true);
    }

    Mono<String> keyMono = getKey(bucket4JConfiguration.getUrl(), rateLimit, request);

    return executeMono.flatMap(execute -> execute ?
        Mono.zip(keyMono, cacheResolver.resolve(bucket4JConfiguration.getCacheName()))
            .flatMap(bi -> {
              String key = bi.getT1();
              ProxyManager<String> buckets = bi.getT2();
              return doConsume(buckets, key, rateLimitBucketConfigurationMap.get(rateLimit), nTokens);
            })
        : Mono.just(ConsumptionResult.notConsumed()));
  }

  private Mono<ConsumptionResult> doConsume(ProxyManager<String> buckets, String key, BucketConfiguration bucketConfiguration, int nTokens) {
    if (buckets.isAsyncModeSupported()) {
      AsyncBucketProxy asyncBucketProxy = buckets.asAsync().builder().build(key, bucketConfiguration);
      if (nTokens > 0) {
        return Mono.fromFuture(asyncBucketProxy.tryConsumeAndReturnRemaining(nTokens))
            .map(probe -> probe.isConsumed() ? ConsumptionResult.consumed(probe.getRemainingTokens()) : ConsumptionResult.rateLimited(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
      } else {
        return Mono.fromFuture(asyncBucketProxy.getAvailableTokens())
            .map(ConsumptionResult::notConsumed);
      }
    } else {
      BucketProxy bucketProxy = buckets.builder().build(key, bucketConfiguration);
      if (nTokens > 0) {
        ConsumptionProbe probe = bucketProxy.tryConsumeAndReturnRemaining(nTokens);
        return Mono.just(probe.isConsumed() ? ConsumptionResult.consumed(probe.getRemainingTokens()) : ConsumptionResult.rateLimited(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
      } else {
        return Mono.just(ConsumptionResult.notConsumed(bucketProxy.getAvailableTokens()));
      }
    }
  }

  private Mono<Boolean> executeCondition(Expression executeCondition, ServerHttpRequest request) {
    StandardEvaluationContext context = new StandardEvaluationContext();
    Mono<Object> rootObjectMono = optExpressionConfigurer.map(expressionConfigurer -> expressionConfigurer.configure(context, request))
        .orElse(Mono.just(request));
    return rootObjectMono.map(rootObject -> executeCondition.getValue(context, rootObject, Boolean.class));
  }

  private Mono<String> getKey(String url, RateLimit rateLimit, ServerHttpRequest request) {
    StandardEvaluationContext context = new StandardEvaluationContext();
    Mono<Object> rootObjectMono = optExpressionConfigurer.map(expressionConfigurer -> expressionConfigurer.configure(context, request))
        .orElse(Mono.just(request));
    return rootObjectMono.map(rootObject -> evaluateKey(rateLimit.getExpression(), context, url, rootObject));
  }

  private String evaluateKey(Expression expr, StandardEvaluationContext context, String url, Object rootObject) {
    final String value = expr != null ? expr.getValue(context, rootObject, String.class) : "1";
    return url + "-" + value;
  }

}
