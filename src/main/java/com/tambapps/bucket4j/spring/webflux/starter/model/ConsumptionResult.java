package com.tambapps.bucket4j.spring.webflux.starter.model;

import static com.tambapps.bucket4j.spring.webflux.starter.service.RateLimitService.NO_LIMIT;

import lombok.Value;

@Value
public class ConsumptionResult {
  public enum Type {
    NOT_CONSUMED, CONSUMED, RATE_LIMITED
  }
  Type type;
  long remainingTokens;
  long retryAfterSeconds;

  public static ConsumptionResult notConsumed() {
    return new ConsumptionResult(Type.NOT_CONSUMED, NO_LIMIT, -1);
  }
  public static ConsumptionResult notConsumed(long remainingTokens) {
    return new ConsumptionResult(Type.NOT_CONSUMED, remainingTokens, -1);
  }

  public static ConsumptionResult consumed(long remainingTokens) {
    return new ConsumptionResult(Type.CONSUMED, remainingTokens, -1);
  }

  public static ConsumptionResult rateLimited(long retryAfterSeconds) {
    return new ConsumptionResult(Type.RATE_LIMITED, 0, retryAfterSeconds);
  }


}
