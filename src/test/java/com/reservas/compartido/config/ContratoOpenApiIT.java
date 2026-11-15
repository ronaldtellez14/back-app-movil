package com.reservas.compartido.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Este cambio no expone endpoints de parámetros. */
@PruebaIntegracion
class ContratoOpenApiIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void ningunaRutaDelContratoEsDeParametros() throws Exception {
    MockHttpServletResponse respuesta =
        mockMvc.perform(get("/docs/api-docs")).andReturn().getResponse();

    assertThat(respuesta.getStatus()).isEqualTo(200);
    JsonNode contrato = jsonMapper.readTree(respuesta.getContentAsString());
    assertThat(contrato.has("openapi")).isTrue();
    assertThat(contrato.path("paths").propertyNames())
        .noneMatch(ruta -> ruta.contains("parametros"));
  }
}
