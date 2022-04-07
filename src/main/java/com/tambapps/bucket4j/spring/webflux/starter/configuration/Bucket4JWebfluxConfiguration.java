package com.tambapps.bucket4j.spring.webflux.starter.configuration;

import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import com.tambapps.bucket4j.spring.webflux.starter.service.RateLimitService;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

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
      ProxyManager<String> buckets,
      Bucket4JWebfluxProperties properties) {
    return new RateLimitService(webfluxFilterExpressionParser, buckets, properties);
  }

}
