package com.tambapps.bucket4j.spring.webflux.starter.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.springframework.core.Ordered;

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
	
	/**
	 * The filter order has a default of the highest precedence reduced by 10
	 */
	private int filterOrder = Ordered.HIGHEST_PRECEDENCE + 10;

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