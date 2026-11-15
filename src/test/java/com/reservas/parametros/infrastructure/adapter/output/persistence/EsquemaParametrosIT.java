package com.reservas.parametros.infrastructure.adapter.output.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reservas.soporte.PruebaIntegracion;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restricciones de las tablas {@code parametro} y {@code valor_parametro} contra PostgreSQL. Cada
 * prueba revierte su transacción; cada inserción rechazada corre en un punto de guardado, para
 * poder comprobar después que no quedó nada.
 */
@PruebaIntegracion
@Transactional
class EsquemaParametrosIT {

  private static final String INSERTAR_PARAMETRO =
      "INSERT INTO parametro (clave, descripcion, tipo, nivel_maximo, minimo, maximo,"
          + " defecto_numero, defecto_booleano, defecto_lista) VALUES ";

  private static final String INSERTAR_VALOR =
      "INSERT INTO valor_parametro (clave, nivel, valor_numero, valor_booleano, valor_lista)"
          + " VALUES ";

  private static final String CLAVE_PRUEBA = "prueba.parametro";

  // Definido por V2: ENTERO, 3 a 10, por defecto 5.
  private static final String CLAVE_EXISTENTE = "codigo_verificacion.intentos";

  @Autowired private JdbcTemplate jdbc;
  @Autowired private DataSource dataSource;

  private void insertarParametro(String valores) {
    jdbc.update(INSERTAR_PARAMETRO + valores);
  }

  private void insertarValor(String valores) {
    jdbc.update(INSERTAR_VALOR + valores);
  }

  /** Ejecuta la inserción en un punto de guardado y exige que la rechace la restricción dada. */
  private void assertRechazada(String restriccion, Runnable insercion) throws SQLException {
    // La conexión de la transacción de la prueba, la misma que usa JdbcTemplate. PostgreSQL
    // invalida la transacción tras un error; volver al punto de guardado la deja usable.
    Connection conexion = DataSourceUtils.getConnection(dataSource);
    Savepoint antes = conexion.setSavepoint();

    assertThatThrownBy(
            () -> {
              try {
                insercion.run();
              } finally {
                conexion.rollback(antes);
              }
            })
        .isInstanceOf(DataIntegrityViolationException.class)
        .satisfies(
            ex -> {
              Throwable causa = ((DataIntegrityViolationException) ex).getMostSpecificCause();
              assertThat(causa).isInstanceOf(PSQLException.class);
              assertThat(((PSQLException) causa).getServerErrorMessage().getConstraint())
                  .isEqualTo(restriccion);
            });
  }

  private int contarParametros(String clave) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM parametro WHERE clave = ?", Integer.class, clave);
  }

  private int contarValores() {
    return jdbc.queryForObject("SELECT count(*) FROM valor_parametro", Integer.class);
  }

  private void assertParametroRechazado(String restriccion, String valores) throws Exception {
    assertRechazada(restriccion, () -> insertarParametro(valores));
    assertThat(contarParametros(CLAVE_PRUEBA)).isZero();
  }

  private void assertValorRechazado(String restriccion, String valores) throws Exception {
    assertRechazada(restriccion, () -> insertarValor(valores));
    assertThat(contarValores()).isZero();
  }

  // Requisito: Definición de parámetro

  @Test
  void seRegistraUnaDefinicionNumericaValida() {
    insertarParametro(
        "('prueba.parametro', 'Días de prueba', 'DIAS', 'PLATAFORMA', 1, 365, 60, NULL, NULL)");

    Map<String, Object> fila =
        jdbc.queryForMap(
            "SELECT clave, descripcion, tipo, nivel_maximo, minimo, maximo, defecto_numero"
                + " FROM parametro WHERE clave = ?",
            CLAVE_PRUEBA);
    assertThat(fila)
        .containsEntry("clave", CLAVE_PRUEBA)
        .containsEntry("descripcion", "Días de prueba")
        .containsEntry("tipo", "DIAS")
        .containsEntry("nivel_maximo", "PLATAFORMA")
        .containsEntry("minimo", 1L)
        .containsEntry("maximo", 365L)
        .containsEntry("defecto_numero", 60L);
  }

  @Test
  void seRegistraUnaDefinicionBooleanaValida() {
    insertarParametro(
        "('prueba.parametro', 'Booleano de prueba', 'BOOLEANO', 'PLATAFORMA',"
            + " NULL, NULL, NULL, TRUE, NULL)");

    Map<String, Object> fila =
        jdbc.queryForMap(
            "SELECT minimo, maximo, defecto_booleano FROM parametro WHERE clave = ?", CLAVE_PRUEBA);
    assertThat(fila.get("minimo")).isNull();
    assertThat(fila.get("maximo")).isNull();
    assertThat(fila).containsEntry("defecto_booleano", true);
  }

  @Test
  void seRegistraUnaDefinicionDeListaValida() {
    insertarParametro(
        "('prueba.parametro', 'Lista de prueba', 'LISTA_ENTEROS', 'PLATAFORMA',"
            + " 1, 48, NULL, NULL, ARRAY[1, 48]::BIGINT[])");

    assertThat(contarParametros(CLAVE_PRUEBA)).isEqualTo(1);
  }

  @Test
  void claveDuplicadaSeRechaza() throws Exception {
    assertRechazada(
        "pk_parametro",
        () ->
            insertarParametro(
                "('codigo_verificacion.intentos', 'Duplicado', 'ENTERO', 'PLATAFORMA',"
                    + " 3, 10, 5, NULL, NULL)"));
    assertThat(contarParametros(CLAVE_EXISTENTE)).isEqualTo(1);
  }

  @Test
  void tipoNoPermitidoSeRechaza() throws Exception {
    String tipoRaro =
        "('prueba.parametro', 'Tipo raro', 'SEGUNDOS', 'PLATAFORMA', 1, 10, 5, NULL, NULL)";
    // PostgreSQL evalúa los CHECK por orden de nombre, y un tipo desconocido tampoco cumple
    // ninguna rama de ck_parametro_defecto_en_rango, que se evalúa primero.
    assertParametroRechazado("ck_parametro_defecto_en_rango", tipoRaro);

    // Sin ese CHECK (solo dentro de esta transacción, que se revierte), lo rechaza
    // ck_parametro_tipo.
    jdbc.execute("ALTER TABLE parametro DROP CONSTRAINT ck_parametro_defecto_en_rango");
    assertParametroRechazado("ck_parametro_tipo", tipoRaro);
  }

  @Test
  void nivelMaximoNoPermitidoSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_nivel_maximo",
        "('prueba.parametro', 'Nivel raro', 'ENTERO', 'SEDE', 1, 10, 5, NULL, NULL)");
  }

  // Requisito: La base de datos impide un valor por defecto fuera de rango

  @Test
  void valorPorDefectoMenorQueElMinimoSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Debajo', 'ENTERO', 'PLATAFORMA', 3, 10, 2, NULL, NULL)");
  }

  @Test
  void valorPorDefectoMayorQueElMaximoSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Encima', 'MINUTOS', 'PLATAFORMA', 5, 30, 31, NULL, NULL)");
  }

  @Test
  void valorPorDefectoEnLosLimitesSeAcepta() {
    insertarParametro(
        "('prueba.minimo', 'En el mínimo', 'HORAS', 'PLATAFORMA', 1, 24, 1, NULL, NULL)");
    insertarParametro(
        "('prueba.maximo', 'En el máximo', 'HORAS', 'PLATAFORMA', 1, 24, 24, NULL, NULL)");

    assertThat(contarParametros("prueba.minimo")).isEqualTo(1);
    assertThat(contarParametros("prueba.maximo")).isEqualTo(1);
  }

  @Test
  void minimoMayorQueElMaximoSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Rango invertido', 'LISTA_ENTEROS', 'PLATAFORMA',"
            + " 10, 1, NULL, NULL, ARRAY[5]::BIGINT[])");
  }

  @Test
  void listaConUnElementoFueraDeRangoSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Lista fuera', 'LISTA_ENTEROS', 'PLATAFORMA',"
            + " 1, 48, NULL, NULL, ARRAY[1, 49]::BIGINT[])");
  }

  @Test
  void listaConUnElementoNuloSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Lista con nulo', 'LISTA_ENTEROS', 'PLATAFORMA',"
            + " 1, 48, NULL, NULL, ARRAY[1, NULL]::BIGINT[])");
  }

  @Test
  void booleanoConMinimoSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Booleano con mínimo', 'BOOLEANO', 'PLATAFORMA',"
            + " 0, NULL, NULL, TRUE, NULL)");
  }

  @Test
  void booleanoConMaximoSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Booleano con máximo', 'BOOLEANO', 'PLATAFORMA',"
            + " NULL, 1, NULL, TRUE, NULL)");
  }

  @Test
  void numericoConValorPorDefectoBooleanoSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Con booleano', 'ENTERO', 'PLATAFORMA', 1, 10, NULL, TRUE, NULL)");
  }

  @Test
  void numericoConValorPorDefectoDeListaSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Con lista', 'ENTERO', 'PLATAFORMA',"
            + " 1, 10, NULL, NULL, ARRAY[5]::BIGINT[])");
  }

  @Test
  void numericoSinValorPorDefectoSeRechaza() throws Exception {
    assertParametroRechazado(
        "ck_parametro_defecto_en_rango",
        "('prueba.parametro', 'Sin defecto', 'ENTERO', 'PLATAFORMA', 1, 10, NULL, NULL, NULL)");
  }

  // Requisito: Valores personalizados por nivel

  @Test
  void seGuardaUnValorDePlataformaParaUnParametroDefinido() {
    insertarValor("('codigo_verificacion.intentos', 'PLATAFORMA', 7, NULL, NULL)");

    Map<String, Object> fila =
        jdbc.queryForMap("SELECT clave, nivel, valor_numero FROM valor_parametro");
    assertThat(fila)
        .containsEntry("clave", CLAVE_EXISTENTE)
        .containsEntry("nivel", "PLATAFORMA")
        .containsEntry("valor_numero", 7L);
  }

  @Test
  void valorDeNivelNegocioSeRechaza() throws Exception {
    assertValorRechazado(
        "ck_valor_parametro_nivel", "('codigo_verificacion.intentos', 'NEGOCIO', 7, NULL, NULL)");
  }

  @Test
  void valorDeNivelProfesionalSeRechaza() throws Exception {
    assertValorRechazado(
        "ck_valor_parametro_nivel",
        "('codigo_verificacion.intentos', 'PROFESIONAL', 7, NULL, NULL)");
  }

  @Test
  void segundoValorDePlataformaSeRechazaYElPrimeroNoCambia() throws Exception {
    insertarValor("('codigo_verificacion.intentos', 'PLATAFORMA', 7, NULL, NULL)");

    assertRechazada(
        "uq_valor_parametro_clave_nivel",
        () -> insertarValor("('codigo_verificacion.intentos', 'PLATAFORMA', 8, NULL, NULL)"));
    assertThat(jdbc.queryForList("SELECT valor_numero FROM valor_parametro", Long.class))
        .containsExactly(7L);
  }

  @Test
  void valorParaUnParametroQueNoExisteSeRechaza() throws Exception {
    assertValorRechazado(
        "fk_valor_parametro_parametro", "('no.existe', 'PLATAFORMA', 7, NULL, NULL)");
  }

  @Test
  void valorSinDatoSeRechaza() throws Exception {
    assertValorRechazado(
        "ck_valor_parametro_un_valor",
        "('codigo_verificacion.intentos', 'PLATAFORMA', NULL, NULL, NULL)");
  }

  @Test
  void valorConMasDeUnDatoSeRechaza() throws Exception {
    assertValorRechazado(
        "ck_valor_parametro_un_valor",
        "('codigo_verificacion.intentos', 'PLATAFORMA', 7, TRUE, NULL)");
  }
}
