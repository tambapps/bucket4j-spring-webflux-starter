package com.tambapps.bucket4j.spring.webflux.starter.configuration;

import com.tambapps.bucket4j.spring.webflux.starter.filter.WebfluxWebFilter;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.server.WebFilter;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@ConditionalOnClass({ WebFilter.class })
@ConditionalOnProperty(prefix = Bucket4JWebfluxProperties.PROPERTY_PREFIX, value = { "enabled" }, matchIfMissing = true)
@AutoConfigureAfter(value = { CacheAutoConfiguration.class })
@EnableConfigurationProperties({ Bucket4JWebfluxProperties.class})
public class Bucket4JWebfluxAutoConfiguration {

  private final Bucket4JWebfluxProperties properties;
  private final GenericApplicationContext context;

  public Bucket4JWebfluxAutoConfiguration(Bucket4JWebfluxProperties properties, GenericApplicationContext context) {
    this.properties = properties;
    this.context = context;
  }

  @PostConstruct
  public void initFilters() {
    ExpressionParser expressionParser = webfluxFilterExpressionParser();
    AtomicInteger filterCount = new AtomicInteger(0);
    properties
        .getFilters()
        .stream()
        .map(filter -> {
          filterCount.incrementAndGet();

          WebFilter webFilter = new WebfluxWebFilter(filter, buckets, expressionParser);
          LOGGER.info("create-webflux-filter;{};{};{}", filterCount, filter.getCacheName(), filter.getUrl());
          return webFilter;
        }).forEach(webFilter ->
            context.registerBean("bucket4JWebfluxFilter" + filterCount, WebFilter.class, () -> webFilter));

  }

  private ExpressionParser webfluxFilterExpressionParser() {
    SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,
        this.getClass().getClassLoader());
    ExpressionParser parser = new SpelExpressionParser(config);

    return parser;
  }
}
