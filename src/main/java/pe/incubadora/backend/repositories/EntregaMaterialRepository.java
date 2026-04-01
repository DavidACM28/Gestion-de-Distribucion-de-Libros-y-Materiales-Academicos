package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.EntregaMaterialEntity;

@Repository
public interface EntregaMaterialRepository extends JpaRepository<EntregaMaterialEntity, Long>, JpaSpecificationExecutor<EntregaMaterialEntity> {
}
