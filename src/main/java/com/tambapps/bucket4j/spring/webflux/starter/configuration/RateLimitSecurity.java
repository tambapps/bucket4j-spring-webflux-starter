package com.tambapps.bucket4j.spring.webflux.starter.configuration;

import com.tambapps.bucket4j.spring.webflux.starter.filter.RateLimitWebFilter;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import com.tambapps.bucket4j.spring.webflux.starter.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

// use a security extending this class
public interface RateLimitSecurity {

  @Bean
  default SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http,
      Bucket4JWebfluxProperties properties,
      RateLimitService rateLimitService) {
    for (Bucket4JConfiguration filter : properties.getFilters()) {
      RateLimitWebFilter webFilter = new RateLimitWebFilter(filter, rateLimitService);
      http.addFilterAt(webFilter, filter.getSecurityFilterOrder());
    }
    return buildSecurityFilterChain(http);
  }

  SecurityWebFilterChain buildSecurityFilterChain(ServerHttpSecurity http);

}
