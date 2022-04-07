
package com.tambapps.bucket4j.spring.webflux.starter.configuration.cache.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.tambapps.bucket4j.spring.webflux.starter.util.CacheResolver;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.hazelcast.HazelcastProxyManager;
import reactor.core.publisher.Mono;

/**
 * Creates the {@link ProxyManager} with Bucket4js {@link HazelcastProxyManager} class.
 * It uses the {@link HazelcastInstance} to retrieve the needed cache. 
 *
 */
public class HazelcastCacheResolver implements CacheResolver {

	private HazelcastInstance hazelcastInstance;

	public HazelcastCacheResolver(HazelcastInstance hazelcastInstance) {
		this.hazelcastInstance = hazelcastInstance;
	}
	
	@Override
	public Mono<ProxyManager<String>> resolve(String cacheName) {
		IMap<String, byte[]> map = hazelcastInstance.getMap(cacheName);
		return Mono.just(new HazelcastProxyManager<>(map));
	}

}
