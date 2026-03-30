package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.SolicitudDistribucionDetalleEntity;
import pe.incubadora.backend.entities.SolicitudDistribucionEntity;

@Repository
public interface SolicitudDistribucionDetalleRepository extends JpaRepository<SolicitudDistribucionDetalleEntity, Long> {
}
