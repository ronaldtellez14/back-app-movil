package com.reservas.parametros.domain;

/**
 * Nivel en el que se personaliza un parámetro. La jerarquía es profesional > negocio > plataforma;
 * por ahora solo existe el nivel plataforma.
 */
public enum NivelParametro {
  PLATAFORMA,
  NEGOCIO,
  PROFESIONAL
}
