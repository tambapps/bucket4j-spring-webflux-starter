package com.tambapps.bucket4j.spring.webflux.starter.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;

@Data
public class Bucket4JConfiguration {

	/**
	 * The cache name. Should be provided or an exception is thrown
	 */
	private String cacheName = "buckets";
	
	/**
	 * The default strategy is {@link RateLimitMatchingStrategy#FIRST}.
	 */
	private RateLimitMatchingStrategy strategy = RateLimitMatchingStrategy.FIRST;
	
	/**
	 * The URL to which the filter should be registered
	 * 
	 */
	private String url = ".*";

	private SecurityWebFiltersOrder securityFilterOrder = SecurityWebFiltersOrder.FIRST;

	private List<RateLimit> rateLimits = new ArrayList<>();
	

	/**
	 * The HTTP content which should be used in case of rate limiting
	 */
	private String httpResponseBody = "{ \"message\": \"Too many requests!\" }";

	/**
	 * Hides the HTTP response headers 
	 * x-rate-limit-remaining
	 * x-rate-limit-retry-after-seconds
	 * 
	 * It does not affect custom defined httpResponseHeaders.
	 */
	private Boolean hideHttpResponseHeaders = Boolean.FALSE;
	
	private Map<String, String> httpResponseHeaders = new HashMap<>();

}