package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.SolicitudDistribucionEntity;

@Repository
public interface SolicitudDistribucionRepository extends JpaRepository<SolicitudDistribucionEntity, Long>, JpaSpecificationExecutor<SolicitudDistribucionEntity> {
    boolean existsBySedeIcpnaIdAndPeriodoAcademicoAndEstadoNot(Long sedeId, String periodoAcademico, String estado);
    boolean existsBySedeIcpnaIdAndPeriodoAcademicoAndEstadoNotAndIdNot(
        Long sedeId,
        String periodoAcademico,
        String estado,
        Long id
    );
}
