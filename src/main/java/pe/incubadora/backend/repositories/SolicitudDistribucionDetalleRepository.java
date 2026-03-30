package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.SolicitudDistribucionDetalleEntity;

@Repository
public interface SolicitudDistribucionDetalleRepository extends JpaRepository<SolicitudDistribucionDetalleEntity, Long> {
    void deleteBySolicitudId(Long solicitudId);
    java.util.List<SolicitudDistribucionDetalleEntity> findBySolicitudId(Long solicitudId);
}
