package com.reservas.parametros.domain;

import java.util.List;

/** Valor de un parámetro, por defecto o personalizado. */
public sealed interface ValorParametro {

  /** Valor de los tipos ENTERO, MINUTOS, HORAS y DIAS. */
  record Numero(long valor) implements ValorParametro {}

  /** Valor del tipo BOOLEANO. */
  record Booleano(boolean valor) implements ValorParametro {}

  /** Valor del tipo LISTA_ENTEROS. La lista es inmutable y no admite nulos. */
  record ListaEnteros(List<Long> valores) implements ValorParametro {
    public ListaEnteros {
      valores = List.copyOf(valores);
    }
  }
}
