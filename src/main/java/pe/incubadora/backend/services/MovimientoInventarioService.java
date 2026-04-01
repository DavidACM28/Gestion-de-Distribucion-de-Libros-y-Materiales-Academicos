package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.AjusteMovimientoInventarioDTO;
import pe.incubadora.backend.entities.LoteIngresoEntity;
import pe.incubadora.backend.entities.MaterialAcademicoEntity;
import pe.incubadora.backend.entities.MovimientoInventarioEntity;
import pe.incubadora.backend.repositories.LoteIngresoRepository;
import pe.incubadora.backend.repositories.MaterialAcademicoRepository;
import pe.incubadora.backend.repositories.MovimientoInventarioRepository;
import pe.incubadora.backend.utils.loteIngreso.LoteIngresoEstado;
import pe.incubadora.backend.utils.movimientoInventario.CreateAjusteMovimientoInventarioResult;
import pe.incubadora.backend.utils.movimientoInventario.TipoAjusteMovimiento;

import java.time.LocalDate;
import java.util.Optional;

@Service
/**
 * Handles inventory movement creation and querying.
 */
public class MovimientoInventarioService {
    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Autowired
    private MaterialAcademicoRepository materialAcademicoRepository;
    @Autowired
    private LoteIngresoRepository loteIngresoRepository;

    /**
     * Creates a manual inventory adjustment and updates lot available quantity accordingly.
     *
     * @param dto adjustment payload
     * @return operation result
     */
    @Transactional
    public CreateAjusteMovimientoInventarioResult createAjusteMovimientoInventario(AjusteMovimientoInventarioDTO dto) {
        MaterialAcademicoEntity material = materialAcademicoRepository.findById(dto.getIdMaterial()).orElse(null);
        if (material == null) {
            return CreateAjusteMovimientoInventarioResult.MATERIAL_NOT_FOUND;
        }

        LoteIngresoEntity lote = loteIngresoRepository.findById(dto.getIdLote()).orElse(null);
        if (lote == null) {
            return CreateAjusteMovimientoInventarioResult.LOTE_NOT_FOUND;
        }
        if (!lote.getMaterialAcademico().getId().equals(material.getId())) {
            return CreateAjusteMovimientoInventarioResult.LOTE_MATERIAL_CONFLICT;
        }
        if (LoteIngresoEstado.FUERA_VIGENCIA.name().equalsIgnoreCase(lote.getEstado())) {
            return CreateAjusteMovimientoInventarioResult.LOTE_NOT_VALID;
        }

        TipoAjusteMovimiento tipoAjuste;
        try {
            tipoAjuste = TipoAjusteMovimiento.valueOf(dto.getTipoAjuste().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CreateAjusteMovimientoInventarioResult.TIPO_AJUSTE_NOT_VALID;
        }

        if (tipoAjuste == TipoAjusteMovimiento.SALIDA && dto.getCantidad() > lote.getCantidadDisponible()) {
            return CreateAjusteMovimientoInventarioResult.STOCK_INSUFFICIENT;
        }

        if (tipoAjuste == TipoAjusteMovimiento.INGRESO) {
            lote.setCantidadDisponible(lote.getCantidadDisponible() + dto.getCantidad());
            if (LoteIngresoEstado.AGOTADO.name().equalsIgnoreCase(lote.getEstado())) {
                lote.setEstado(LoteIngresoEstado.DISPONIBLE.name());
            }
        } else {
            lote.setCantidadDisponible(lote.getCantidadDisponible() - dto.getCantidad());
            if (lote.getCantidadDisponible() == 0) {
                lote.setEstado(LoteIngresoEstado.AGOTADO.name());
            }
        }
        loteIngresoRepository.save(lote);

        MovimientoInventarioEntity movimiento = new MovimientoInventarioEntity();
        movimiento.setMaterialAcademico(material);
        movimiento.setLote(lote);
        movimiento.setFecha(LocalDate.now());
        movimiento.setTipoMovimiento("AJUSTE");
        movimiento.setCantidad(dto.getCantidad());
        movimiento.setReferenciaTipo("AJUSTE");
        movimiento.setReferenciaId(lote.getId());
        movimiento.setComentario(dto.getComentario());
        movimientoInventarioRepository.save(movimiento);

        return CreateAjusteMovimientoInventarioResult.CREATED;
    }

    /**
     * Returns one inventory movement by id.
     *
     * @param id movement identifier
     * @return optional movement
     */
    public Optional<MovimientoInventarioEntity> getMovimientoInventarioById(Long id) {
        return movimientoInventarioRepository.findById(id);
    }

    /**
     * Returns paginated inventory movements using optional filters.
     *
     * @param materialId optional material id filter
     * @param loteId optional lot id filter
     * @param tipoMovimiento optional movement type filter
     * @param fechaDesde optional start date
     * @param fechaHasta optional end date
     * @param page page index
     * @param size page size
     * @param sort sort direction token
     * @return paginated movement list
     */
    public Page<MovimientoInventarioEntity> getMovimientosByFilters(
        Long materialId,
        Long loteId,
        String tipoMovimiento,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        int page,
        int size,
        String sort
    ) {
        Specification<MovimientoInventarioEntity> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (materialId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("materialAcademico").get("id"), materialId));
        }
        if (loteId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("lote").get("id"), loteId));
        }
        if (tipoMovimiento != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipoMovimiento"), tipoMovimiento.toUpperCase()));
        }
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
        }

        Sort.Direction direction = "descending".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "id"));

        return movimientoInventarioRepository.findAll(spec, pageable);
    }
}
