package com.tambapps.bucket4j.spring.webflux.starter.service;

import com.tambapps.bucket4j.spring.webflux.starter.properties.BandWidth;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimit;
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
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@ConditionalOnProperty(prefix = Bucket4JWebfluxProperties.PROPERTY_PREFIX, value = { "enabled" }, matchIfMissing = true)
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

  public Mono<Long> applyRateLimit(String path, RateLimit rateLimit,
      ServerHttpRequest request) {
    for (Bucket4JConfiguration configuration : properties.getFilters()) {
      if (path.matches(configuration.getUrl())) {
        return applyRateLimit(configuration, rateLimit, request);
      }
    }
    return Mono.just(NO_LIMIT);
  }

  public Mono<Long> applyRateLimit(Bucket4JConfiguration bucket4JConfiguration, RateLimit rateLimit,
      ServerHttpRequest request) {
    String key = getKey(bucket4JConfiguration, rateLimit, request);
    // TODO prepare this in a configuration bean
    BucketConfiguration bucketConfiguration = prepareBucket4jConfigurationBuilder(rateLimit).build();

    AsyncBucketProxy asyncBucketProxy = buckets.asAsync().builder().build(key, bucketConfiguration);
    return Mono.fromFuture(asyncBucketProxy.tryConsumeAndReturnRemaining(1))
        .map(probe -> probe.isConsumed() ? probe.getRemainingTokens() : NO_LIMIT);
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
