package com.tambapps.bucket4j.spring.webflux.starter.configuration.cache;


import com.tambapps.bucket4j.spring.webflux.starter.configuration.cache.hazelcast.HazelcastBucket4jCacheConfiguration;
import com.tambapps.bucket4j.spring.webflux.starter.configuration.cache.jcache.JCacheBucket4jConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@AutoConfigureAfter(CacheAutoConfiguration.class)
@Import(value = {JCacheBucket4jConfiguration.class, HazelcastBucket4jCacheConfiguration.class})
public class Bucket4jCacheConfiguration {


}
