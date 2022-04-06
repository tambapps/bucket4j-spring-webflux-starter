package com.tambapps.bucket4j.spring.webflux.starter.properties;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RateLimit {

	/**
	 * SpEl condition to check if the rate limit should be executed. If null there is no check.
	 */
	private String executeCondition;

	/**
	 * SpEl condition to check if the rate-limit should apply. If null there is no check.
	 */
	private String skipCondition;

	private String expression = "1";

	private List<BandWidth> bandwidths = new ArrayList<>();
}