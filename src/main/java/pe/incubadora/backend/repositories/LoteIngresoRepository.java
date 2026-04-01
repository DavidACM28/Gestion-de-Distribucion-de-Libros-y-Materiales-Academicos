package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.LoteIngresoEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoteIngresoRepository extends JpaRepository<LoteIngresoEntity, Long> {
    @Query("""
        select coalesce(sum(l.cantidadDisponible), 0)
        from LoteIngresoEntity l
        where l.materialAcademico.id = :materialId
        and l.estado = :estado
    """)
    Integer sumCantidadDisponibleByMaterialIdAndEstado(Long materialId, String estado);

    @Query("""
        select l
        from LoteIngresoEntity l
        where l.materialAcademico.id = :materialId
        and l.estado = :estado
        and l.cantidadDisponible > 0
        and (l.fechaVigencia is null or l.fechaVigencia >= :hoy)
        order by
            case when l.fechaVigencia is null then 1 else 0 end,
            l.fechaVigencia asc,
            l.fechaIngreso asc,
            l.id asc
    """)
    List<LoteIngresoEntity> findDisponiblesOrdenFefoByMaterialId(Long materialId, String estado, LocalDate hoy);
}
