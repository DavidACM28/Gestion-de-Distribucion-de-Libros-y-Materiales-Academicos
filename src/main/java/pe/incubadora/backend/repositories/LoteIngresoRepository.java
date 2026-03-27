package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.LoteIngresoEntity;

@Repository
public interface LoteIngresoRepository extends JpaRepository<LoteIngresoEntity, Long> {
}
