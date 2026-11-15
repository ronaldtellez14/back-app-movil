package com.reservas.parametros;

import static org.assertj.core.api.Assertions.assertThat;

import com.reservas.parametros.application.port.input.ClavesParametro;
import com.reservas.parametros.application.port.input.ConsultarParametroUseCase;
import com.reservas.soporte.PruebaIntegracion;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/** Base recién migrada: las 8 definiciones del CU-00 y ningún valor personalizado. */
@PruebaIntegracion
class ParametrosInicialesIT {

  private record Definicion(
      String clave,
      String descripcion,
      String tipo,
      long defecto,
      long minimo,
      long maximo,
      String nivelMaximo) {}

  // Tabla del escenario "Base de datos recién migrada", con las descripciones del proposal.
  private static final List<Definicion> ESPERADAS =
      List.of(
          new Definicion(
              "codigo_verificacion.digitos",
              "Cantidad de dígitos del código de verificación",
              "ENTERO",
              6,
              6,
              8,
              "PLATAFORMA"),
          new Definicion(
              "codigo_verificacion.intentos",
              "Intentos permitidos para ingresar el código de verificación",
              "ENTERO",
              5,
              3,
              10,
              "PLATAFORMA"),
          new Definicion(
              "codigo_verificacion.minutos_vigencia",
              "Minutos de vigencia del código de verificación",
              "MINUTOS",
              10,
              5,
              30,
              "PLATAFORMA"),
          new Definicion(
              "contrasena.intentos_fallidos",
              "Intentos fallidos de contraseña antes de bloquear",
              "ENTERO",
              5,
              3,
              10,
              "PLATAFORMA"),
          new Definicion(
              "contrasena.minutos_bloqueo",
              "Minutos que dura el bloqueo por intentos fallidos de contraseña",
              "MINUTOS",
              15,
              5,
              1440,
              "PLATAFORMA"),
          new Definicion(
              "sesion.cliente.dias_inactividad",
              "Días sin uso tras los cuales se cierra la sesión de un cliente",
              "DIAS",
              60,
              1,
              365,
              "PLATAFORMA"),
          new Definicion(
              "sesion.negocio.dias_inactividad",
              "Días sin uso tras los cuales se cierra la sesión de un profesional o dueño",
              "DIAS",
              30,
              1,
              90,
              "PLATAFORMA"),
          new Definicion(
              "sesion.panel.horas",
              "Horas que dura la sesión del panel desde que se inicia",
              "HORAS",
              8,
              1,
              24,
              "PLATAFORMA"));

  @Autowired private JdbcTemplate jdbc;
  @Autowired private Environment entorno;
  @Autowired private ConsultarParametroUseCase consultarParametro;

  @Test
  void elContextoArrancaValidandoElEsquemaContraReservasTest() {
    assertThat(entorno.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    assertThat(jdbc.queryForObject("SELECT current_database()", String.class))
        .isEqualTo("reservas_test");
  }

  @Test
  void existenExactamenteLasOchoDefinicionesDelCu00() {
    List<Definicion> guardadas =
        jdbc.query(
            "SELECT clave, descripcion, tipo, defecto_numero, minimo, maximo, nivel_maximo"
                + " FROM parametro ORDER BY clave",
            (fila, i) ->
                new Definicion(
                    fila.getString("clave"),
                    fila.getString("descripcion"),
                    fila.getString("tipo"),
                    fila.getLong("defecto_numero"),
                    fila.getLong("minimo"),
                    fila.getLong("maximo"),
                    fila.getString("nivel_maximo")));

    assertThat(guardadas).containsExactlyElementsOf(ESPERADAS);
  }

  @Test
  void noHayValoresPersonalizados() {
    assertThat(jdbc.queryForObject("SELECT count(*) FROM valor_parametro", Integer.class)).isZero();
  }

  @Test
  void cadaConstanteDeClavesParametroExisteEnLaBase() throws IllegalAccessException {
    List<String> claves = jdbc.queryForList("SELECT clave FROM parametro", String.class);
    List<Field> constantes =
        Arrays.stream(ClavesParametro.class.getDeclaredFields())
            .filter(campo -> Modifier.isStatic(campo.getModifiers()))
            .filter(campo -> campo.getType() == String.class)
            .toList();

    assertThat(constantes).hasSize(ESPERADAS.size());
    for (Field constante : constantes) {
      assertThat(claves).as(constante.getName()).contains((String) constante.get(null));
    }
  }

  @Test
  void laSesionDelClienteDuraSesentaDias() {
    assertThat(consultarParametro.obtenerDuracion(ClavesParametro.SESION_CLIENTE_DIAS_INACTIVIDAD))
        .isEqualTo(Duration.ofDays(60));
  }
}
