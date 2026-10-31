-- Parámetros del CU-00, tabla "Parámetros que usa". El administrador podrá cambiarlos con el CU-13.

INSERT INTO parametro (clave, descripcion, tipo, nivel_maximo, minimo, maximo, defecto_numero)
VALUES
    ('sesion.cliente.dias_inactividad',
     'Días sin uso tras los cuales se cierra la sesión de un cliente',
     'DIAS', 'PLATAFORMA', 1, 365, 60),
    ('sesion.negocio.dias_inactividad',
     'Días sin uso tras los cuales se cierra la sesión de un profesional o dueño',
     'DIAS', 'PLATAFORMA', 1, 90, 30),
    ('sesion.panel.horas',
     'Horas que dura la sesión del panel desde que se inicia',
     'HORAS', 'PLATAFORMA', 1, 24, 8),
    ('codigo_verificacion.digitos',
     'Cantidad de dígitos del código de verificación',
     'ENTERO', 'PLATAFORMA', 6, 8, 6),
    ('codigo_verificacion.minutos_vigencia',
     'Minutos de vigencia del código de verificación',
     'MINUTOS', 'PLATAFORMA', 5, 30, 10),
    ('codigo_verificacion.intentos',
     'Intentos permitidos para ingresar el código de verificación',
     'ENTERO', 'PLATAFORMA', 3, 10, 5),
    ('contrasena.intentos_fallidos',
     'Intentos fallidos de contraseña antes de bloquear',
     'ENTERO', 'PLATAFORMA', 3, 10, 5),
    ('contrasena.minutos_bloqueo',
     'Minutos que dura el bloqueo por intentos fallidos de contraseña',
     'MINUTOS', 'PLATAFORMA', 5, 1440, 15);
