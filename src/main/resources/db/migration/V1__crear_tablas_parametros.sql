-- Definiciones de parámetros configurables y valores personalizados por nivel.
-- En un CHECK, una expresión NULL cuenta como aprobada: cada rama exige IS NOT NULL antes de comparar.

CREATE TABLE parametro (
    clave            VARCHAR(100) NOT NULL,
    descripcion      VARCHAR(255) NOT NULL,
    tipo             VARCHAR(20)  NOT NULL,
    nivel_maximo     VARCHAR(20)  NOT NULL,
    minimo           BIGINT,
    maximo           BIGINT,
    defecto_numero   BIGINT,
    defecto_booleano BOOLEAN,
    defecto_lista    BIGINT[],
    CONSTRAINT pk_parametro PRIMARY KEY (clave),
    CONSTRAINT ck_parametro_tipo
        CHECK (tipo IN ('ENTERO', 'MINUTOS', 'HORAS', 'DIAS', 'BOOLEANO', 'LISTA_ENTEROS')),
    CONSTRAINT ck_parametro_nivel_maximo
        CHECK (nivel_maximo IN ('PLATAFORMA', 'NEGOCIO', 'PROFESIONAL')),
    CONSTRAINT ck_parametro_defecto_en_rango CHECK (
        (tipo IN ('ENTERO', 'MINUTOS', 'HORAS', 'DIAS')
            AND defecto_numero IS NOT NULL
            AND defecto_booleano IS NULL
            AND defecto_lista IS NULL
            AND minimo IS NOT NULL
            AND maximo IS NOT NULL
            AND minimo <= maximo
            AND defecto_numero BETWEEN minimo AND maximo)
        OR (tipo = 'BOOLEANO'
            AND defecto_booleano IS NOT NULL
            AND defecto_numero IS NULL
            AND defecto_lista IS NULL
            AND minimo IS NULL
            AND maximo IS NULL)
        OR (tipo = 'LISTA_ENTEROS'
            AND defecto_lista IS NOT NULL
            AND defecto_numero IS NULL
            AND defecto_booleano IS NULL
            AND minimo IS NOT NULL
            AND maximo IS NOT NULL
            AND minimo <= maximo
            AND array_position(defecto_lista, NULL) IS NULL
            AND minimo <= ALL (defecto_lista)
            AND maximo >= ALL (defecto_lista))
    )
);

CREATE TABLE valor_parametro (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    clave          VARCHAR(100) NOT NULL,
    nivel          VARCHAR(20)  NOT NULL,
    valor_numero   BIGINT,
    valor_booleano BOOLEAN,
    valor_lista    BIGINT[],
    CONSTRAINT pk_valor_parametro PRIMARY KEY (id),
    CONSTRAINT fk_valor_parametro_parametro FOREIGN KEY (clave) REFERENCES parametro (clave),
    -- Se amplía cuando lleguen los niveles negocio y profesional, con sus columnas de id.
    CONSTRAINT ck_valor_parametro_nivel CHECK (nivel = 'PLATAFORMA'),
    CONSTRAINT ck_valor_parametro_un_valor
        CHECK (num_nonnulls(valor_numero, valor_booleano, valor_lista) = 1),
    CONSTRAINT uq_valor_parametro_clave_nivel UNIQUE (clave, nivel)
);
