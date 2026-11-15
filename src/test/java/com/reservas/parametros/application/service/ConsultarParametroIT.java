package com.reservas.parametros.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reservas.parametros.application.port.input.ClavesParametro;
import com.reservas.parametros.application.port.input.ConsultarParametroUseCase;
import com.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de punta a punta: caso de uso, adaptador JPA y PostgreSQL. Revierte al terminar. */
@PruebaIntegracion
@Transactional
class ConsultarParametroIT {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ConsultarParametroUseCase consultarParametro;

  @Test
  void devuelveElValorDePlataformaEnLugarDelPorDefecto() {
    // Por defecto 5, rango 3 a 10 (V2).
    String clave = ClavesParametro.CODIGO_VERIFICACION_INTENTOS;
    assertThat(consultarParametro.obtenerEntero(clave)).isEqualTo(5);

    jdbc.update(
        "INSERT INTO valor_parametro (clave, nivel, valor_numero) VALUES (?, 'PLATAFORMA', 7)",
        clave);

    assertThat(consultarParametro.obtenerEntero(clave)).isEqualTo(7);
  }
}
