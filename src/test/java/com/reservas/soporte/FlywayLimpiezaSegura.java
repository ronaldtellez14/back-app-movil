package com.reservas.soporte;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;

/**
 * Limpia y migra la base antes de las pruebas de integración, solo si su nombre real termina en
 * {@code _test}. Si no, falla sin limpiar, para no borrar la base de desarrollo.
 */
public class FlywayLimpiezaSegura implements FlywayMigrationStrategy {

  static final String SUFIJO_BASE_PRUEBAS = "_test";

  @Override
  public void migrate(Flyway flyway) {
    String base = baseActual(flyway);
    if (base == null || !base.endsWith(SUFIJO_BASE_PRUEBAS)) {
      throw new IllegalStateException(
          "La base '"
              + base
              + "' no termina en "
              + SUFIJO_BASE_PRUEBAS
              + ": no se limpia. Revise DB_TEST_URL.");
    }
    flyway.clean();
    flyway.migrate();
  }

  // El nombre real de la base, no el de la URL, que puede tener alias o parámetros.
  private static String baseActual(Flyway flyway) {
    try (Connection conexion = flyway.getConfiguration().getDataSource().getConnection();
        Statement consulta = conexion.createStatement();
        ResultSet resultado = consulta.executeQuery("SELECT current_database()")) {
      return resultado.next() ? resultado.getString(1) : null;
    } catch (SQLException ex) {
      throw new IllegalStateException("No se pudo consultar el nombre de la base.", ex);
    }
  }
}
