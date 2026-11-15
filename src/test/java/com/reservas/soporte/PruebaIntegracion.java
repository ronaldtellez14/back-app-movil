package com.reservas.soporte;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de integración contra {@code reservas_test} (perfil {@code it}). Todas las clases *IT usan
 * la misma configuración, así que comparten un solo contexto y la base se limpia una vez.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("PARAMETROS")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Import(PruebaIntegracion.Configuracion.class)
public @interface PruebaIntegracion {

  @TestConfiguration(proxyBeanMethods = false)
  class Configuracion {

    @Bean
    FlywayMigrationStrategy flywayLimpiezaSegura() {
      return new FlywayLimpiezaSegura();
    }
  }
}
