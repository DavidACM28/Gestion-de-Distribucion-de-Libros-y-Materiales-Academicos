package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.LoteIngresoEntity;

@Repository
public interface LoteIngresoRepository extends JpaRepository<LoteIngresoEntity, Long> {
    @Query("""
        select coalesce(sum(l.cantidadDisponible), 0)
        from LoteIngresoEntity l
        where l.materialAcademico.id = :materialId
        and l.estado = :estado
    """)
    Integer sumCantidadDisponibleByMaterialIdAndEstado(Long materialId, String estado);
}
