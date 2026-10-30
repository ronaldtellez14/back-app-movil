package com.reservas.compartido.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Respuesta estándar de error de la API. */
@Schema(description = "Respuesta estándar de error")
public record RespuestaError(
    @Schema(description = "Código del error", example = "SOLICITUD_INVALIDA") String codigo,
    @Schema(description = "Mensaje legible del error") String mensaje,
    @Schema(description = "Errores por campo; vacío si el error no es de un campo")
        List<CampoError> campos,
    @Schema(
            description =
                "Identificador para buscar el error en los logs; solo en errores no controlados")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String trazaId) {

  public static RespuestaError de(CodigoError codigoError) {
    return de(codigoError, List.of());
  }

  public static RespuestaError de(CodigoError codigoError, List<CampoError> campos) {
    return new RespuestaError(codigoError.name(), codigoError.mensaje(), List.copyOf(campos), null);
  }

  public static RespuestaError conTraza(CodigoError codigoError, String trazaId) {
    return new RespuestaError(codigoError.name(), codigoError.mensaje(), List.of(), trazaId);
  }

  /** Error de validación de un campo de la solicitud. */
  @Schema(description = "Error de validación de un campo")
  public record CampoError(
      @Schema(description = "Nombre del campo en snake_case") String campo,
      @Schema(description = "Mensaje de validación") String mensaje) {}
}
