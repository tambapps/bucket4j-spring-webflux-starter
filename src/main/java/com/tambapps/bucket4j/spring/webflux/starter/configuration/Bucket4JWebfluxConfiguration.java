package com.tambapps.bucket4j.spring.webflux.starter.configuration;

import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = Bucket4JWebfluxProperties.PROPERTY_PREFIX, value = { "enabled" }, matchIfMissing = true)
public class Bucket4JWebfluxConfiguration {

  // there may be some useful things here someday (?)

}
