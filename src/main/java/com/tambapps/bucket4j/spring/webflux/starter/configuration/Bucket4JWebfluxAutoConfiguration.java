package com.tambapps.bucket4j.spring.webflux.starter.configuration;

import com.tambapps.bucket4j.spring.webflux.starter.configuration.cache.Bucket4jCacheConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.filter.RateLimitWebFilter;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.server.WebFilter;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConditionalOnClass({ WebFilter.class })
@ConditionalOnProperty(prefix = Bucket4JWebfluxProperties.PROPERTY_PREFIX, value = { "enabled" }, matchIfMissing = true)
@AutoConfigureAfter(value = { CacheAutoConfiguration.class, Bucket4jCacheConfiguration.class })
@ConditionalOnBean(value = CacheResolver.class)
@Import(value = {Bucket4JWebfluxConfiguration.class, ExpressionConverter.class, RateLimitService.class})
@EnableConfigurationProperties({ Bucket4JWebfluxProperties.class})
public class Bucket4JWebfluxAutoConfiguration {

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
