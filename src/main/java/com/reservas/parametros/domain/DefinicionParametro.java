package com.reservas.parametros.domain;

/**
 * Definición de un parámetro configurable. No valida el rango al construirse: un valor por defecto
 * inválido se detecta al consultarlo, con {@link CodigoErrorParametro#PARAMETRO_FUERA_DE_RANGO}.
 *
 * @param minimo nulo solo en booleano
 * @param maximo nulo solo en booleano
 */
public record DefinicionParametro(
    String clave,
    String descripcion,
    TipoParametro tipo,
    ValorParametro valorPorDefecto,
    Long minimo,
    Long maximo,
    NivelParametro nivelMaximo) {

  /**
   * Revisa que el valor esté entre el mínimo y el máximo. En booleano no aplica; en lista se revisa
   * cada elemento.
   *
   * @throws ParametroException con {@link CodigoErrorParametro#PARAMETRO_FUERA_DE_RANGO}
   */
  public void validarRango(ValorParametro valor) {
    switch (valor) {
      case ValorParametro.Numero numero -> validarElemento(numero.valor());
      case ValorParametro.Booleano booleano -> {
        // Los booleanos no tienen mínimo ni máximo.
      }
      case ValorParametro.ListaEnteros lista -> lista.valores().forEach(this::validarElemento);
    }
  }

  private void validarElemento(long valor) {
    if (minimo == null || maximo == null || valor < minimo || valor > maximo) {
      throw new ParametroException(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO, clave);
    }
  }
}
