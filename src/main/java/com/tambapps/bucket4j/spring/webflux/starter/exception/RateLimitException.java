package com.tambapps.bucket4j.spring.webflux.starter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RateLimitException extends ResponseStatusException {

  @Getter
  private final long retryAfterSeconds;

  public RateLimitException(long retryAfterSeconds) {
    super(HttpStatus.TOO_MANY_REQUESTS);
    this.retryAfterSeconds = retryAfterSeconds;
  }
}
