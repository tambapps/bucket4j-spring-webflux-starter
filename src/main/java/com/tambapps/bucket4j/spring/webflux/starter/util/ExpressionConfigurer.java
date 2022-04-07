package com.tambapps.bucket4j.spring.webflux.starter.util;

import org.springframework.expression.EvaluationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 * Interface used to configure the context of an Expression Parser
 */
public interface ExpressionConfigurer {

  /**
   * Configure the context of an expression parser and returns the root object to use
   *
   * @param context the evaluation context
   * @param request the request
   * @return the root object to use for the expression parser
   */
  Mono<Object> configure(EvaluationContext context, ServerHttpRequest request);

}
