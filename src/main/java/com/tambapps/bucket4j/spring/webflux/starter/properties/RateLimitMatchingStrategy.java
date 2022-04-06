package com.tambapps.bucket4j.spring.webflux.starter.properties;

public enum RateLimitMatchingStrategy {

	/**
	 * All rate limits should be evaluated
	 */
	ALL,
	/**
	 * Only the first matching rate limit will be evaluated
	 */
	FIRST,
	
}
