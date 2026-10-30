package com.reservas.compartido.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Ajustes de Jackson que forman parte del contrato de la API. El snake_case y la zona horaria
 * America/Bogota se configuran en application.yml.
 */
@Configuration
public class JacksonConfig {

  /** Fechas y horas siempre como texto ISO-8601, nunca como números. */
  @Bean
  public JsonMapperBuilderCustomizer fechasIso() {
    return builder -> builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
  }
}
