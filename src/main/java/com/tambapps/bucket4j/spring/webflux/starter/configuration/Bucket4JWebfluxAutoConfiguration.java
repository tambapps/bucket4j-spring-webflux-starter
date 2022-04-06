package com.tambapps.bucket4j.spring.webflux.starter.configuration;

import com.tambapps.bucket4j.spring.webflux.starter.filter.WebfluxWebFilter;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import com.tambapps.bucket4j.spring.webflux.starter.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.server.WebFilter;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = Bucket4JWebfluxProperties.PROPERTY_PREFIX, value = { "enabled" }, matchIfMissing = true)
@AutoConfigureAfter(value = { CacheAutoConfiguration.class })
@EnableConfigurationProperties({ Bucket4JWebfluxProperties.class})
public class Bucket4JWebfluxAutoConfiguration {

  private final Bucket4JWebfluxProperties properties;
  private final GenericApplicationContext context;
  private final RateLimitService rateLimitService;

  public Bucket4JWebfluxAutoConfiguration(Bucket4JWebfluxProperties properties,
      GenericApplicationContext context,
      RateLimitService rateLimitService) {
    this.properties = properties;
    this.context = context;
    this.rateLimitService = rateLimitService;
  }

  @PostConstruct
  public void initFilters() {
    AtomicInteger filterCount = new AtomicInteger(0);
    properties
        .getFilters()
        .stream()
        .map(filter -> {
          filterCount.incrementAndGet();

          WebFilter webFilter = new WebfluxWebFilter(filter, rateLimitService);
          LOGGER.info("create-webflux-filter;{};{};{}", filterCount, filter.getCacheName(), filter.getUrl());
          return webFilter;
        }).forEach(webFilter ->
            context.registerBean("bucket4JWebfluxFilter" + filterCount, WebFilter.class, () -> webFilter));
  }
}
