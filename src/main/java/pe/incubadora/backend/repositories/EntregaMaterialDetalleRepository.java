package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.EntregaMaterialDetalleEntity;

import java.util.List;

@Repository
public interface EntregaMaterialDetalleRepository extends JpaRepository<EntregaMaterialDetalleEntity, Long> {
    List<EntregaMaterialDetalleEntity> findByEntregaId(Long entregaId);

    void deleteByEntregaId(Long entregaId);
}
