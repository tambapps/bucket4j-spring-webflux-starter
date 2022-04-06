package com.tambapps.bucket4j.spring.webflux.starter.properties;

import lombok.Data;

import java.time.temporal.ChronoUnit;

/**
 * Configures the rate of data which should be transfered
 *
 */
@Data
public class BandWidth {

	private long capacity;
	private long time;
	private ChronoUnit unit;

	private long fixedRefillInterval = 0;;
	private ChronoUnit fixedRefillIntervalUnit = ChronoUnit.MINUTES;

}
