package com.reservas.compartido.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.reservas.compartido.error.RespuestaError.CampoError;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

@Tag("PARAMETROS")
class RespuestaErrorTest {

  // Misma estrategia de nombres que application.yml.
  private final JsonMapper json =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

  @Test
  void deUsaElCodigoYElMensajeConstantesSinCampos() {
    RespuestaError respuesta = RespuestaError.de(CodigoError.RECURSO_NO_ENCONTRADO);

    assertThat(respuesta.codigo()).isEqualTo("RECURSO_NO_ENCONTRADO");
    assertThat(respuesta.mensaje()).isEqualTo(CodigoError.RECURSO_NO_ENCONTRADO.mensaje());
    assertThat(respuesta.campos()).isEmpty();
    assertThat(respuesta.trazaId()).isNull();
  }

  @Test
  void deConCamposGuardaUnaCopiaInmutable() {
    List<CampoError> campos = new ArrayList<>(List.of(new CampoError("fecha_inicio", "requerido")));

    RespuestaError respuesta = RespuestaError.de(CodigoError.SOLICITUD_INVALIDA, campos);
    campos.clear();

    assertThat(respuesta.codigo()).isEqualTo("SOLICITUD_INVALIDA");
    assertThat(respuesta.campos()).containsExactly(new CampoError("fecha_inicio", "requerido"));
    assertThat(respuesta.trazaId()).isNull();
  }

  @Test
  void conTrazaGuardaElTrazaIdSinCampos() {
    RespuestaError respuesta = RespuestaError.conTraza(CodigoError.ERROR_INTERNO, "abc-123");

    assertThat(respuesta.codigo()).isEqualTo("ERROR_INTERNO");
    assertThat(respuesta.mensaje()).isEqualTo(CodigoError.ERROR_INTERNO.mensaje());
    assertThat(respuesta.campos()).isEmpty();
    assertThat(respuesta.trazaId()).isEqualTo("abc-123");
  }

  @Test
  void trazaIdSeSerializaSoloCuandoTieneValor() {
    JsonNode sinTraza = json.valueToTree(RespuestaError.de(CodigoError.ACCESO_DENEGADO));
    JsonNode conTraza =
        json.valueToTree(RespuestaError.conTraza(CodigoError.ERROR_INTERNO, "abc-123"));

    assertThat(sinTraza.has("traza_id")).isFalse();
    assertThat(sinTraza.has("campos")).isTrue();
    assertThat(conTraza.get("traza_id").asString()).isEqualTo("abc-123");
  }
}
