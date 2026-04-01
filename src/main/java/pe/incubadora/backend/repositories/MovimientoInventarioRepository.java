package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.MovimientoInventarioEntity;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventarioEntity, Long> {
}
