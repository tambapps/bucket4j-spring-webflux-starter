package com.tambapps.bucket4j.spring.webflux.starter.util;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import reactor.core.publisher.Mono;

/**
 * The CacheResolver is used to resolve Bucket4js {@link ProxyManager} by
 * a given cache name. Each cache implementation should implement this interface.
 * 
 */
public interface CacheResolver {

	Mono<ProxyManager<String>> resolve(String cacheName);
	
}
