package com.reservas.compartido.error;

import com.reservas.compartido.error.RespuestaError.CampoError;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Traduce las excepciones no controladas a la respuesta estándar de error. */
@Slf4j
@RestControllerAdvice
public class ManejadorGlobalErrores {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException ex) {
    List<CampoError> campos =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new CampoError(aSnakeCase(error.getField()), error.getDefaultMessage()))
            .toList();
    return responder(CodigoError.SOLICITUD_INVALIDA, campos);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<RespuestaError> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
    return responder(CodigoError.CUERPO_ILEGIBLE, List.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<RespuestaError> manejarAccesoDenegado(AccessDeniedException ex) {
    return responder(CodigoError.ACCESO_DENEGADO, List.of());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<RespuestaError> manejarNoEncontrado(NoResourceFoundException ex) {
    return responder(CodigoError.RECURSO_NO_ENCONTRADO, List.of());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<RespuestaError> manejarMetodoNoPermitido(
      HttpRequestMethodNotSupportedException ex) {
    return responder(CodigoError.METODO_NO_PERMITIDO, List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<RespuestaError> manejarInesperado(Exception ex) {
    String trazaId = UUID.randomUUID().toString();
    log.error("Error no controlado [traza_id={}]: {}", trazaId, trazaSinMensajes(ex));
    CodigoError codigo = CodigoError.ERROR_INTERNO;
    return ResponseEntity.status(codigo.estado()).body(RespuestaError.conTraza(codigo, trazaId));
  }

  /**
   * Tipo y traza (clase, método y línea) de la excepción y sus causas, sin los mensajes: podrían
   * contener datos personales (Ley 1581).
   */
  private static String trazaSinMensajes(Throwable ex) {
    StringBuilder traza = new StringBuilder(ex.getClass().getName());
    Set<Throwable> vistas = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Throwable actual = ex; actual != null && vistas.add(actual); actual = actual.getCause()) {
      if (actual != ex) {
        traza
            .append(System.lineSeparator())
            .append("Causado por: ")
            .append(actual.getClass().getName());
      }
      for (StackTraceElement elemento : actual.getStackTrace()) {
        traza.append(System.lineSeparator()).append("\tat ").append(elemento);
      }
    }
    return traza.toString();
  }

  /** Nombre del campo como aparece en el JSON (snake_case), p. ej. fechaInicio -> fecha_inicio. */
  private static String aSnakeCase(String campo) {
    return campo.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
  }

  private ResponseEntity<RespuestaError> responder(CodigoError codigo, List<CampoError> campos) {
    return ResponseEntity.status(codigo.estado()).body(RespuestaError.de(codigo, campos));
  }
}
