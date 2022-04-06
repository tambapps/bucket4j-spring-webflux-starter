package com.tambapps.bucket4j.spring.webflux.starter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RateLimitException extends ResponseStatusException {
  public RateLimitException() {
    super(HttpStatus.TOO_MANY_REQUESTS);
  }
}
