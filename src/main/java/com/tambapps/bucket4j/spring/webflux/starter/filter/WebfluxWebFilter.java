package com.tambapps.bucket4j.spring.webflux.starter.filter;

import com.tambapps.bucket4j.spring.webflux.starter.exception.RateLimitException;
import com.tambapps.bucket4j.spring.webflux.starter.properties.BandWidth;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimit;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimitMatchingStrategy;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.core.Ordered;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class WebfluxWebFilter implements WebFilter, Ordered {

  private static final Long NO_LIMIT = Long.MIN_VALUE;

  private final Bucket4JConfiguration bucket4JConfiguration;
  private final ProxyManager<String> buckets;
  private final ExpressionParser expressionParser;

  public WebfluxWebFilter(Bucket4JConfiguration bucket4JConfiguration,
      ProxyManager<String> buckets,
      ExpressionParser expressionParser) {
    this.bucket4JConfiguration = bucket4JConfiguration;
    this.buckets = buckets;
    this.expressionParser = expressionParser;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    if (!request.getURI().getPath().matches(bucket4JConfiguration.getUrl())) {
      return chain.filter(exchange);
    }

    List<Mono<Long>> monos = bucket4JConfiguration.getRateLimits()
        .stream()
        .map(rateLimit -> applyRateLimit(rateLimit, exchange.getRequest()))
        .collect(Collectors.toList());

    Mono<Long> reducedRateLimitsMono = Flux.concat(monos)
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

    return reducedRateLimitsMono.flatMap(remaining -> {
      if (NO_LIMIT.equals(remaining)) {
        return chain.filter(exchange);
      } else if (remaining <= 0) {
        return Mono.error(new RateLimitException());
      } else {
        if(!bucket4JConfiguration.getHideHttpResponseHeaders()) {
          response.getHeaders().set("X-Rate-Limit-Remaining", "" + remaining);
        }
        return chain.filter(exchange);
      }
    });
  }

  private Mono<Long> applyRateLimit(RateLimit rateLimit,
      ServerHttpRequest request) {
    String key = getKey(rateLimit, request);
    // TODO prepare this in a configuration bean
    BucketConfiguration bucketConfiguration = prepareBucket4jConfigurationBuilder(rateLimit).build();


    AsyncBucketProxy asyncBucketProxy = buckets.asAsync().builder().build(key, bucketConfiguration);
    return Mono.fromFuture(asyncBucketProxy.tryConsumeAndReturnRemaining(1))
        .map(probe -> probe.isConsumed() ? probe.getRemainingTokens() : NO_LIMIT);
  }

  private String getKey(RateLimit rateLimit, ServerHttpRequest request) {
    Expression expr = expressionParser.parseExpression(rateLimit.getExpression());
    StandardEvaluationContext context = new StandardEvaluationContext();
    // TODO allow to configure root object
    final String value = expr.getValue(context, request, String.class);
    return bucket4JConfiguration.getUrl() + "-" + value;
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

  @Override
  public int getOrder() {
    return bucket4JConfiguration.getFilterOrder();
  }
}
