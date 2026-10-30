package com.reservas.compartido.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales del contrato OpenAPI que consume reservas-app. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("API de reservas")
                .version("v1")
                .description(
                    "API de la plataforma de reservas de citas para negocios con agenda en"
                        + " Colombia. Contrato consumido por la app reservas-app."));
  }
}
