package com.reservas.parametros.infrastructure.adapter.output.persistence;

import com.reservas.parametros.domain.DefinicionParametro;
import com.reservas.parametros.domain.ValorParametro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ParametroPersistenceMapper {

  @Mapping(target = "valorPorDefecto", source = ".", qualifiedByName = "valorPorDefecto")
  DefinicionParametro aDominio(ParametroEntity entidad);

  /** Toma la columna de valor por defecto que corresponde al tipo. */
  @Named("valorPorDefecto")
  default ValorParametro valorPorDefecto(ParametroEntity entidad) {
    return switch (entidad.getTipo()) {
      case ENTERO, MINUTOS, HORAS, DIAS -> new ValorParametro.Numero(entidad.getDefectoNumero());
      case BOOLEANO -> new ValorParametro.Booleano(entidad.getDefectoBooleano());
      case LISTA_ENTEROS -> new ValorParametro.ListaEnteros(entidad.getDefectoLista());
    };
  }

  /** Toma la única columna de valor con dato (la base exige exactamente una). */
  default ValorParametro aValor(ValorParametroEntity entidad) {
    if (entidad.getValorNumero() != null) {
      return new ValorParametro.Numero(entidad.getValorNumero());
    }
    if (entidad.getValorBooleano() != null) {
      return new ValorParametro.Booleano(entidad.getValorBooleano());
    }
    if (entidad.getValorLista() != null) {
      return new ValorParametro.ListaEnteros(entidad.getValorLista());
    }
    throw new IllegalStateException(
        "El valor del parámetro " + entidad.getClave() + " no tiene dato.");
  }
}
