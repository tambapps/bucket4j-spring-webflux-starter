package com.tambapps.bucket4j.spring.webflux.starter.exception;

public class CacheNotFoundException extends RuntimeException {

  public CacheNotFoundException(String cacheName) {
    super(String.format("cache %s was not found", cacheName));
  }
}
