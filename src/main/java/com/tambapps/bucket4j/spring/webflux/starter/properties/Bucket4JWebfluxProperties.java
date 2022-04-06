package com.tambapps.bucket4j.spring.webflux.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = Bucket4JWebfluxProperties.PROPERTY_PREFIX)
@Data
public class Bucket4JWebfluxProperties {

  public static final String PROPERTY_PREFIX = "bucket4j-webflux";

  /**
   * Enables or disables the Bucket4j Spring Boot Starter.
   */
  private Boolean enabled = true;

  private List<Bucket4JConfiguration> filters = new ArrayList<>();


}
