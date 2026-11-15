package com.reservas.compartido.error;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.reservas.compartido.error.RespuestaError.CampoError;
import com.reservas.parametros.domain.CodigoErrorParametro;
import com.reservas.parametros.domain.ParametroException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Tag("PARAMETROS")
class ManejadorGlobalErroresTest {

  private static final String CLAVE = "codigo_verificacion.intentos";

  private final ManejadorGlobalErrores manejador = new ManejadorGlobalErrores();

  private final Logger logger = (Logger) LoggerFactory.getLogger(ManejadorGlobalErrores.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @BeforeEach
  void capturarLog() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void soltarLog() {
    logger.detachAppender(appender);
    appender.stop();
  }

  private static void assertRespuesta(
      ResponseEntity<RespuestaError> respuesta, HttpStatus estado, CodigoError codigo) {
    assertThat(respuesta.getStatusCode()).isEqualTo(estado);
    assertThat(respuesta.getBody()).isNotNull();
    assertThat(respuesta.getBody().codigo()).isEqualTo(codigo.name());
    assertThat(respuesta.getBody().mensaje()).isEqualTo(codigo.mensaje());
  }

  // Firma usada solo para construir el MethodParameter de la excepción de validación.
  @SuppressWarnings("unused")
  private void metodoValidado(Object solicitud) {}

  @Test
  void validacionDevuelveLosCamposEnSnakeCase() throws NoSuchMethodException {
    BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "solicitud");
    resultado.addError(new FieldError("solicitud", "fechaInicio", "no debe ser nulo"));
    resultado.addError(new FieldError("solicitud", "nombre", "no debe estar vacío"));
    MethodParameter parametro =
        new MethodParameter(
            ManejadorGlobalErroresTest.class.getDeclaredMethod("metodoValidado", Object.class), 0);

    ResponseEntity<RespuestaError> respuesta =
        manejador.manejarValidacion(new MethodArgumentNotValidException(parametro, resultado));

    assertRespuesta(respuesta, HttpStatus.BAD_REQUEST, CodigoError.SOLICITUD_INVALIDA);
    assertThat(respuesta.getBody().campos())
        .containsExactly(
            new CampoError("fecha_inicio", "no debe ser nulo"),
            new CampoError("nombre", "no debe estar vacío"));
    assertThat(respuesta.getBody().trazaId()).isNull();
  }

  @Test
  void cuerpoIlegibleDevuelve400() {
    ResponseEntity<RespuestaError> respuesta =
        manejador.manejarCuerpoIlegible(
            new HttpMessageNotReadableException(
                "JSON roto", new MockHttpInputMessage(new byte[0])));

    assertRespuesta(respuesta, HttpStatus.BAD_REQUEST, CodigoError.CUERPO_ILEGIBLE);
    assertThat(respuesta.getBody().campos()).isEmpty();
  }

  @Test
  void accesoDenegadoDevuelve403() {
    ResponseEntity<RespuestaError> respuesta =
        manejador.manejarAccesoDenegado(new AccessDeniedException("denegado"));

    assertRespuesta(respuesta, HttpStatus.FORBIDDEN, CodigoError.ACCESO_DENEGADO);
  }

  @Test
  void recursoNoEncontradoDevuelve404() {
    ResponseEntity<RespuestaError> respuesta =
        manejador.manejarNoEncontrado(
            new NoResourceFoundException(HttpMethod.GET, "/api/v1/no-existe", "no-existe"));

    assertRespuesta(respuesta, HttpStatus.NOT_FOUND, CodigoError.RECURSO_NO_ENCONTRADO);
  }

  @Test
  void metodoNoPermitidoDevuelve405() {
    ResponseEntity<RespuestaError> respuesta =
        manejador.manejarMetodoNoPermitido(new HttpRequestMethodNotSupportedException("DELETE"));

    assertRespuesta(respuesta, HttpStatus.METHOD_NOT_ALLOWED, CodigoError.METODO_NO_PERMITIDO);
  }

  @Test
  void errorNoControladoDevuelve500ConTrazaIdYSinElMensaje() {
    ResponseEntity<RespuestaError> respuesta =
        manejador.manejarInesperado(new IllegalArgumentException("correo ana@ejemplo.com"));

    assertRespuesta(respuesta, HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO);
    assertThat(respuesta.getBody().trazaId()).isNotBlank();
    assertThat(respuesta.getBody().campos()).isEmpty();
    assertThat(respuesta.getBody().toString()).doesNotContain("ana@ejemplo.com");
  }

  @Test
  void parametroExceptionDevuelve500SinClaveValorRangoNiMensaje() {
    ParametroException ex =
        new ParametroException(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO, CLAVE);

    ResponseEntity<RespuestaError> respuesta = manejador.manejarInesperado(ex);

    assertRespuesta(respuesta, HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO);
    RespuestaError cuerpo = respuesta.getBody();
    assertThat(cuerpo.trazaId()).isNotBlank();
    assertThat(cuerpo.campos()).isEmpty();
    assertThat(cuerpo.toString())
        .doesNotContain(CLAVE)
        .doesNotContain(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO.name())
        .doesNotContain(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO.mensaje());
  }

  @Test
  void elLogDeParametroExceptionTieneTrazaIdTipoYTrazaSinElMensaje() {
    ParametroException ex =
        new ParametroException(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO, CLAVE);

    String trazaId = manejador.manejarInesperado(ex).getBody().trazaId();

    assertThat(appender.list).hasSize(1);
    ILoggingEvent evento = appender.list.getFirst();
    assertThat(evento.getLevel()).isEqualTo(Level.ERROR);
    // Sin la excepción adjunta: Logback imprimiría su mensaje.
    assertThat(evento.getThrowableProxy()).isNull();
    assertThat(evento.getFormattedMessage())
        .contains(trazaId)
        .contains(ParametroException.class.getName())
        .contains("\tat " + ManejadorGlobalErroresTest.class.getName())
        .doesNotContain(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO.mensaje())
        .doesNotContain(CLAVE);
  }

  @Test
  void elLogIncluyeElTipoDeLasCausasSinSusMensajes() {
    IllegalStateException causa = new IllegalStateException("documento 1020304050");
    RuntimeException ex = new RuntimeException("celular 3001234567", causa);

    manejador.manejarInesperado(ex);

    String mensaje = appender.list.getFirst().getFormattedMessage();
    assertThat(mensaje)
        .contains("Causado por: " + IllegalStateException.class.getName())
        .doesNotContain("1020304050")
        .doesNotContain("3001234567");
  }

  @Test
  void elLogTerminaAunqueLasCausasFormenUnCiclo() {
    RuntimeException primera = new RuntimeException("a");
    IllegalStateException segunda = new IllegalStateException("b");
    primera.initCause(segunda);
    segunda.initCause(primera);

    manejador.manejarInesperado(primera);

    String mensaje = appender.list.getFirst().getFormattedMessage();
    int veces = mensaje.split("Causado por: ", -1).length - 1;
    assertThat(veces).isEqualTo(1);
  }
}
