package com.tambapps.bucket4j.spring.webflux.starter.filter;

import static com.tambapps.bucket4j.spring.webflux.starter.service.RateLimitService.NO_LIMIT;

import com.tambapps.bucket4j.spring.webflux.starter.exception.RateLimitException;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.service.RateLimitService;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

public class WebfluxWebFilter implements WebFilter, Ordered {

  private final Bucket4JConfiguration bucket4JConfiguration;
  private final RateLimitService rateLimitService;
  private final Pattern urlPattern;

  public WebfluxWebFilter(Bucket4JConfiguration bucket4JConfiguration,
      RateLimitService rateLimitService) {
    this.bucket4JConfiguration = bucket4JConfiguration;
    this.rateLimitService = rateLimitService;
    urlPattern = Pattern.compile(bucket4JConfiguration.getUrl());
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    String path = request.getURI().getPath();
    if (!urlPattern.matcher(path).matches()) {
      return chain.filter(exchange);
    }

    return rateLimitService.consume(bucket4JConfiguration, request, 1).flatMap(remaining -> {
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

  @Override
  public int getOrder() {
    return bucket4JConfiguration.getFilterOrder();
  }
}
