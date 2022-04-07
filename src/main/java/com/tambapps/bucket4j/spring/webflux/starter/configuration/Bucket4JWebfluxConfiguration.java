package com.tambapps.bucket4j.spring.webflux.starter.configuration;

import com.tambapps.bucket4j.spring.webflux.starter.properties.BandWidth;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimit;
import com.tambapps.bucket4j.spring.webflux.starter.service.RateLimitService;
import com.tambapps.bucket4j.spring.webflux.starter.util.CacheResolver;
import com.tambapps.bucket4j.spring.webflux.starter.util.ExpressionConfigurer;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.Refill;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(prefix = Bucket4JWebfluxProperties.PROPERTY_PREFIX, value = { "enabled" }, matchIfMissing = true)
@EnableConfigurationProperties({ Bucket4JWebfluxProperties.class})
public class Bucket4JWebfluxConfiguration {

  @Bean
  public ExpressionParser webfluxFilterExpressionParser() {
    SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,
        this.getClass().getClassLoader());
    return new SpelExpressionParser(config);
  }

  @Bean
  public RateLimitService rateLimitService(
      ExpressionParser webfluxFilterExpressionParser,
      CacheResolver cacheResolver,
      Bucket4JWebfluxProperties properties,
      Map<RateLimit, BucketConfiguration> rateLimitBucketConfigurationMap,
      Optional<ExpressionConfigurer> optExpressionConfigurer) {
    return new RateLimitService(webfluxFilterExpressionParser, cacheResolver, properties,
        rateLimitBucketConfigurationMap,
        optExpressionConfigurer);
  }

  @Bean
  public Map<RateLimit, BucketConfiguration> rateLimitBucketConfigurationMap(Bucket4JWebfluxProperties properties) {
    Map<RateLimit, BucketConfiguration> map = properties.getFilters().stream().flatMap(f -> f.getRateLimits().stream())
        .collect(Collectors.toMap(Function.identity(), this::prepareBucket4jConfiguration));
      return Collections.unmodifiableMap(map);
  }

  private BucketConfiguration prepareBucket4jConfiguration(RateLimit rl) {
    ConfigurationBuilder configBuilder = BucketConfiguration.builder();
    for (BandWidth bandWidth : rl.getBandwidths()) {
      Bandwidth bucket4jBandWidth = Bandwidth.simple(bandWidth.getCapacity(), Duration.of(bandWidth.getTime(), bandWidth.getUnit()));
      if(bandWidth.getFixedRefillInterval() > 0) {
        bucket4jBandWidth = Bandwidth.classic(bandWidth.getCapacity(), Refill.intervally(bandWidth.getCapacity(), Duration.of(bandWidth.getFixedRefillInterval(), bandWidth.getFixedRefillIntervalUnit())));
      }
      configBuilder = configBuilder.addLimit(bucket4jBandWidth);
    };
    return configBuilder.build();
  }

}
