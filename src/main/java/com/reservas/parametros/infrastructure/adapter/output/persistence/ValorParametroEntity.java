package com.reservas.parametros.infrastructure.adapter.output.persistence;

import com.reservas.parametros.domain.NivelParametro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "valor_parametro")
@Getter
@Setter
public class ValorParametroEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "clave", nullable = false, length = 100)
  private String clave;

  @Enumerated(EnumType.STRING)
  @Column(name = "nivel", nullable = false, length = 20)
  private NivelParametro nivel;

  @Column(name = "valor_numero")
  private Long valorNumero;

  @Column(name = "valor_booleano")
  private Boolean valorBooleano;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "valor_lista")
  private List<Long> valorLista;
}
