package com.tambapps.bucket4j.spring.webflux.starter.configuration;

import com.tambapps.bucket4j.spring.webflux.starter.properties.BandWidth;
import com.tambapps.bucket4j.spring.webflux.starter.properties.Bucket4JWebfluxProperties;
import com.tambapps.bucket4j.spring.webflux.starter.properties.RateLimit;
import com.tambapps.bucket4j.spring.webflux.starter.service.RateLimitService;
import com.tambapps.bucket4j.spring.webflux.starter.util.CacheResolver;
import com.tambapps.bucket4j.spring.webflux.starter.util.ExpressionConfigurer;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.Refill;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

//@Configuration
//@ConditionalOnProperty(prefix = Bucket4JWebfluxProperties.PROPERTY_PREFIX, value = { "enabled" }, matchIfMissing = true)

public class Bucket4JWebfluxConfiguration {


}
