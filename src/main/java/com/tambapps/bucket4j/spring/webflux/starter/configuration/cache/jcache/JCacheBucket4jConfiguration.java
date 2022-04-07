package com.tambapps.bucket4j.spring.webflux.starter.configuration.cache.jcache;

import com.tambapps.bucket4j.spring.webflux.starter.util.CacheResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;

@Configuration
@ConditionalOnClass({ Caching.class, JCacheCacheManager.class })
@ConditionalOnBean(CacheManager.class)
@ConditionalOnMissingBean(CacheResolver.class)
public class JCacheBucket4jConfiguration {
	private CacheManager cacheManager;

	public JCacheBucket4jConfiguration(CacheManager cacheManager){
		this.cacheManager = cacheManager;
	}

	@Bean
	public CacheResolver bucket4jCacheResolver() {
		return new JCacheCacheResolver(cacheManager);
	}
	
}
