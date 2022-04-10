package com.tambapps.bucket4j.spring.webflux.starter.properties;

import lombok.Data;
import org.springframework.expression.Expression;

import java.util.ArrayList;
import java.util.List;

@Data
public class RateLimit {

	/**
	 * SpEl condition to check if the rate limit should be executed. If null there is no check.
	 */
	private Expression executeCondition;

	/**
	 * SpEl condition to check if the rate-limit should apply. If null there is no check.
	 */
	private Expression skipCondition;

	// also nullable, but default to "1" (see RateLimitService)
	private Expression expression;

	private List<BandWidth> bandwidths = new ArrayList<>();
}