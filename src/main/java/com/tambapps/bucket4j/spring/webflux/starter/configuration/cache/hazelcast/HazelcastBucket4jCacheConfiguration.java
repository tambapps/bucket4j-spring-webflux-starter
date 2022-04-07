package com.tambapps.bucket4j.spring.webflux.starter.configuration.cache.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.tambapps.bucket4j.spring.webflux.starter.configuration.cache.jcache.JCacheBucket4jConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.util.CacheResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the asynchronous support for Hazelcast. The synchronous support of Hazelcast
 * is already provided by the {@link JCacheBucket4jConfiguration}. It uses the {@link HazelcastInstance}
 * to access the {@link HazelcastInstance} to retrieve the cache.
 */
@Configuration
@ConditionalOnClass({ HazelcastInstance.class })
@ConditionalOnBean(HazelcastInstance.class)
@ConditionalOnMissingBean(CacheResolver.class)
public class HazelcastBucket4jCacheConfiguration {
	
	private final HazelcastInstance hazelcastInstance;
	
	public HazelcastBucket4jCacheConfiguration(HazelcastInstance hazelcastInstance) {
		this.hazelcastInstance = hazelcastInstance;
	}
	
	@Bean
	public CacheResolver hazelcastCacheResolver() {
		return new HazelcastCacheResolver(hazelcastInstance);
	}
}
