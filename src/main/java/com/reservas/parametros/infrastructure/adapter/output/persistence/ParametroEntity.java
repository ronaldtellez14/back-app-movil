package com.reservas.parametros.infrastructure.adapter.output.persistence;

import com.reservas.parametros.domain.NivelParametro;
import com.reservas.parametros.domain.TipoParametro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "parametro")
@Getter
@Setter
public class ParametroEntity {

  @Id
  @Column(name = "clave", length = 100)
  private String clave;

  @Column(name = "descripcion", nullable = false)
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, length = 20)
  private TipoParametro tipo;

  @Enumerated(EnumType.STRING)
  @Column(name = "nivel_maximo", nullable = false, length = 20)
  private NivelParametro nivelMaximo;

  @Column(name = "minimo")
  private Long minimo;

  @Column(name = "maximo")
  private Long maximo;

  @Column(name = "defecto_numero")
  private Long defectoNumero;

  @Column(name = "defecto_booleano")
  private Boolean defectoBooleano;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "defecto_lista")
  private List<Long> defectoLista;
}
