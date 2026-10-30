package com.reservas.compartido.config;

import com.reservas.compartido.error.CodigoError;
import com.reservas.compartido.error.RespuestaError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seguridad base: cerrado por defecto, salvo la salud de la aplicación y la documentación de la
 * API. Cada módulo abrirá sus endpoints cuando se implemente su caso de uso.
 */
@Configuration
public class SeguridadConfig {

  private static final String[] RUTAS_PUBLICAS = {
    "/actuator/health",
    "/docs",
    "/docs/**",
    // Recursos estáticos de Swagger UI: springdoc los sirve aquí aunque la UI se abra en /docs.
    "/swagger-ui/**"
  };

  @Bean
  public SecurityFilterChain cadenaSeguridad(HttpSecurity http, JsonMapper jsonMapper) {
    // CSRF desactivado a propósito: API REST sin estado, sin sesión ni cookies de autenticación,
    // así que el navegador no envía credenciales automáticamente. Revisar si algún día se
    // autentica con cookies.
    return http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(RUTAS_PUBLICAS).permitAll().anyRequest().authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, e) ->
                            escribirError(response, jsonMapper, CodigoError.NO_AUTENTICADO))
                    .accessDeniedHandler(
                        (request, response, e) ->
                            escribirError(response, jsonMapper, CodigoError.ACCESO_DENEGADO)))
        .build();
  }

  private static void escribirError(
      HttpServletResponse response, JsonMapper jsonMapper, CodigoError codigo) throws IOException {
    response.setStatus(codigo.estado().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    jsonMapper.writeValue(response.getOutputStream(), RespuestaError.de(codigo));
  }
}
