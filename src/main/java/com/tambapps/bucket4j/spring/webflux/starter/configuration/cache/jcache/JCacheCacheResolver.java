package com.tambapps.bucket4j.spring.webflux.starter.configuration.cache.jcache;

import com.tambapps.bucket4j.spring.webflux.starter.exception.CacheNotFoundException;
import com.tambapps.bucket4j.spring.webflux.starter.util.CacheResolver;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import reactor.core.publisher.Mono;

import javax.cache.Cache;
import javax.cache.CacheManager;

/**
 * This class is the JCache (JSR-107) implementation of the {@link CacheResolver}.
 * It uses Bucket4Js {@link JCacheProxyManager} to implement the {@link ProxyManager}.
 *
 */
public class JCacheCacheResolver implements CacheResolver {
	
	private final CacheManager cacheManager;

	public JCacheCacheResolver(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}
	
	public Mono<ProxyManager<String>> resolve(String cacheName) {
		// unfortunately we can only get it synchronously
		Cache<String, byte[]> springCache = cacheManager.getCache(cacheName);
		if (springCache == null) {
			throw new CacheNotFoundException(cacheName);
		}

		return Mono.just(new JCacheProxyManager<>(springCache));
	}
	
}
