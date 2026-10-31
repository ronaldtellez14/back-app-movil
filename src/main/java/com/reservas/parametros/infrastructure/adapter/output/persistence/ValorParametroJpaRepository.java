package com.reservas.parametros.infrastructure.adapter.output.persistence;

import com.reservas.parametros.domain.NivelParametro;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValorParametroJpaRepository extends JpaRepository<ValorParametroEntity, Long> {

  Optional<ValorParametroEntity> findByClaveAndNivel(String clave, NivelParametro nivel);
}
