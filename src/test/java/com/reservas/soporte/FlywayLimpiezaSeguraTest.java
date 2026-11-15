package com.reservas.soporte;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("PARAMETROS")
@ExtendWith(MockitoExtension.class)
class FlywayLimpiezaSeguraTest {

  @Mock private Flyway flyway;
  @Mock private Configuration configuracion;
  @Mock private DataSource dataSource;
  @Mock private Connection conexion;
  @Mock private Statement consulta;
  @Mock private ResultSet resultado;

  private final FlywayLimpiezaSegura estrategia = new FlywayLimpiezaSegura();

  private void dadaLaConsulta() throws SQLException {
    when(flyway.getConfiguration()).thenReturn(configuracion);
    when(configuracion.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(conexion);
    when(conexion.createStatement()).thenReturn(consulta);
    when(consulta.executeQuery("SELECT current_database()")).thenReturn(resultado);
  }

  private void dadaLaBase(String nombre) throws SQLException {
    dadaLaConsulta();
    when(resultado.next()).thenReturn(true);
    when(resultado.getString(1)).thenReturn(nombre);
  }

  @Test
  void conUnaBaseQueNoTerminaEnTestLanzaExcepcionSinLimpiar() throws SQLException {
    dadaLaBase("reservas");

    assertThatThrownBy(() -> estrategia.migrate(flyway)).isInstanceOf(IllegalStateException.class);
    verify(flyway, never()).clean();
    verify(flyway, never()).migrate();
  }

  @Test
  void conUnaBaseQueSoloContieneTestNoLimpia() throws SQLException {
    dadaLaBase("reservas_test_copia");

    assertThatThrownBy(() -> estrategia.migrate(flyway)).isInstanceOf(IllegalStateException.class);
    verify(flyway, never()).clean();
  }

  @Test
  void conReservasTestLimpiaYLuegoMigra() throws SQLException {
    dadaLaBase("reservas_test");

    estrategia.migrate(flyway);

    InOrder orden = inOrder(flyway);
    orden.verify(flyway).clean();
    orden.verify(flyway).migrate();
  }

  @Test
  void siLaConsultaNoDevuelveFilasNoLimpia() throws SQLException {
    dadaLaConsulta();
    when(resultado.next()).thenReturn(false);

    assertThatThrownBy(() -> estrategia.migrate(flyway)).isInstanceOf(IllegalStateException.class);
    verify(flyway, never()).clean();
  }

  @Test
  void siNoSePuedeConsultarLaBaseNoLimpia() throws SQLException {
    when(flyway.getConfiguration()).thenReturn(configuracion);
    when(configuracion.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenThrow(new SQLException("sin conexión"));

    assertThatThrownBy(() -> estrategia.migrate(flyway))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(SQLException.class);
    verify(flyway, never()).clean();
  }
}
