package com.tambapps.bucket4j.spring.webflux.starter.configuration;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class ExpressionConverter implements Converter<String, Expression> {

  private final SpelExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(
      SpelCompilerMode.IMMEDIATE,
      this.getClass().getClassLoader()));

  @Override
  public Expression convert(String source) {
    return expressionParser.parseExpression(source);
  }
}
