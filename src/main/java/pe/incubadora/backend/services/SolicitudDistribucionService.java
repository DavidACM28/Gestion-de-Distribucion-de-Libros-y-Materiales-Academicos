package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.SolicitudDistribucionDTO;
import pe.incubadora.backend.dtos.SolicitudDistribucionDetalleDTO;
import pe.incubadora.backend.dtos.AprobarSolicitudDTO;
import pe.incubadora.backend.dtos.AprobarSolicitudDetalleDTO;
import pe.incubadora.backend.entities.MaterialAcademicoEntity;
import pe.incubadora.backend.entities.SedeIcpnaEntity;
import pe.incubadora.backend.entities.SolicitudDistribucionDetalleEntity;
import pe.incubadora.backend.entities.SolicitudDistribucionEntity;
import pe.incubadora.backend.repositories.LoteIngresoRepository;
import pe.incubadora.backend.repositories.MaterialAcademicoRepository;
import pe.incubadora.backend.repositories.SedeIcpnaRepository;
import pe.incubadora.backend.repositories.SolicitudDistribucionDetalleRepository;
import pe.incubadora.backend.repositories.SolicitudDistribucionRepository;
import pe.incubadora.backend.utils.sedeIcpna.SedeEstado;
import pe.incubadora.backend.utils.loteIngreso.LoteIngresoEstado;
import pe.incubadora.backend.utils.solicitudDistribucion.AprobarSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.CancelarSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.CreateSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.EnviarSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.ObservarSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.SolicitudDistribucionEstado;
import pe.incubadora.backend.utils.solicitudDistribucion.SolicitudDistribucionPrioridad;
import pe.incubadora.backend.utils.solicitudDistribucion.UpdateSolicitudDistribucionResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
/**
 * Contains business logic for distribution requests, including lifecycle transitions and approvals.
 */
public class SolicitudDistribucionService {
    @Autowired
    private SolicitudDistribucionRepository solicitudDistribucionRepository;
    @Autowired
    private SolicitudDistribucionDetalleRepository solicitudDistribucionDetalleRepository;
    @Autowired
    private SedeIcpnaRepository sedeIcpnaRepository;
    @Autowired
    private MaterialAcademicoRepository materialAcademicoRepository;
    @Autowired
    private LoteIngresoRepository loteIngresoRepository;

    /**
     * Creates a distribution request in {@code BORRADOR} with its detail lines.
     *
     * @param dto request payload
     * @return create operation result
     */
    @Transactional
    public CreateSolicitudDistribucionResult createSolicitudDistribucion(SolicitudDistribucionDTO dto) {
        LocalDate fechaLimiteRevision;
        SolicitudDistribucionPrioridad prioridad;

        SedeIcpnaEntity sede = sedeIcpnaRepository.findById(dto.getIdSede()).orElse(null);
        if (sede == null) {
            return CreateSolicitudDistribucionResult.SEDE_NOT_FOUND;
        }
        if (!SedeEstado.ACTIVA.name().equalsIgnoreCase(sede.getEstado())) {
            return CreateSolicitudDistribucionResult.SEDE_NOT_ACTIVE;
        }
        if (solicitudDistribucionRepository.existsBySedeIcpnaIdAndPeriodoAcademicoAndEstadoNot(
            dto.getIdSede(),
            dto.getPeriodoAcademico(),
            SolicitudDistribucionEstado.CANCELADA.name()
        )) {
            return CreateSolicitudDistribucionResult.SOLICITUD_DUPLICADA;
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return CreateSolicitudDistribucionResult.ITEMS_EMPTY;
        }

        try {
            YearMonth.parse(dto.getPeriodoAcademico());
            prioridad = SolicitudDistribucionPrioridad.valueOf(dto.getPrioridad().toUpperCase());
        } catch (DateTimeParseException e) {
            return CreateSolicitudDistribucionResult.PERIODO_NOT_VALID;
        } catch (IllegalArgumentException e) {
            return CreateSolicitudDistribucionResult.PRIORIDAD_NOT_VALID;
        }

        CreateSolicitudDistribucionResult validationResult = validateItems(dto);
        if (validationResult != null) {
            return validationResult;
        }

        fechaLimiteRevision = switch (prioridad) {
            case NORMAL -> LocalDate.now().plusDays(4);
            case ALTA -> LocalDate.now().plusDays(2);
            case URGENTE -> LocalDate.now().plusDays(1);
        };

        SolicitudDistribucionEntity solicitud = new SolicitudDistribucionEntity();
        solicitud.setCodigo(dto.getCodigo());
        solicitud.setSedeIcpna(sede);
        solicitud.setPeriodoAcademico(dto.getPeriodoAcademico());
        solicitud.setFechaSolicitud(LocalDate.now());
        solicitud.setPrioridad(prioridad.name());
        solicitud.setEstado(SolicitudDistribucionEstado.BORRADOR.name());
        solicitud.setFechaLimiteRevision(fechaLimiteRevision);
        solicitud.setComentarioSolicitud(dto.getComentarioSolicitud());
        solicitud.setComentarioRevision(null);
        solicitud = solicitudDistribucionRepository.save(solicitud);

        for (SolicitudDistribucionDetalleDTO item : dto.getItems()) {
            MaterialAcademicoEntity material = materialAcademicoRepository.findById(item.getIdMaterial()).orElseThrow();

            SolicitudDistribucionDetalleEntity detalle = new SolicitudDistribucionDetalleEntity();
            detalle.setSolicitud(solicitud);
            detalle.setMaterial(material);
            detalle.setCantidadSolicitada(item.getCantidadSolicitada());
            detalle.setCantidadAprobada(0);
            detalle.setComentarioItem(item.getComentarioItem());
            solicitudDistribucionDetalleRepository.save(detalle);
        }

        return CreateSolicitudDistribucionResult.CREATED;
    }

    /**
     * Updates a request and optionally replaces its detail lines.
     *
     * @param dto update payload
     * @param id request identifier
     * @return update operation result
     */
    @Transactional
    public UpdateSolicitudDistribucionResult updateSolicitudDistribucion(SolicitudDistribucionDTO dto, Long id) {
        SolicitudDistribucionEntity solicitud = solicitudDistribucionRepository.findById(id).orElse(null);
        if (solicitud == null) {
            return UpdateSolicitudDistribucionResult.SOLICITUD_NOT_FOUND;
        }

        SedeIcpnaEntity sede = obtenerSedeParaUpdate(dto, solicitud);
        if (sede == null) {
            return UpdateSolicitudDistribucionResult.SEDE_NOT_FOUND;
        }

        UpdateSolicitudDistribucionResult result = validateSolicitudDistribucionDTO(dto, solicitud, sede);
        if (result != null) {
            return result;
        }

        applyChanges(dto, solicitud, sede);
        solicitudDistribucionRepository.save(solicitud);
        return UpdateSolicitudDistribucionResult.UPDATED;
    }

    /**
     * Moves a request to {@code ENVIADA} when current status allows it.
     *
     * @param id request identifier
     * @return operation result
     */
    @Transactional
    public EnviarSolicitudDistribucionResult enviarSolicitudDistribucion(Long id) {
        SolicitudDistribucionEntity solicitud = solicitudDistribucionRepository.findById(id).orElse(null);
        if (solicitud == null) {
            return EnviarSolicitudDistribucionResult.SOLICITUD_NOT_FOUND;
        }

        if (!SolicitudDistribucionEstado.BORRADOR.name().equalsIgnoreCase(solicitud.getEstado())
            && !SolicitudDistribucionEstado.OBSERVADA.name().equalsIgnoreCase(solicitud.getEstado())) {
            return EnviarSolicitudDistribucionResult.ESTADO_INVALIDO;
        }

        solicitud.setEstado(SolicitudDistribucionEstado.ENVIADA.name());
        solicitudDistribucionRepository.save(solicitud);
        return EnviarSolicitudDistribucionResult.UPDATED;
    }

    /**
     * Marks a request as observed and stores the review comment.
     *
     * @param id request identifier
     * @param comentarioRevision review comment
     * @return operation result
     */
    @Transactional
    public ObservarSolicitudDistribucionResult observarSolicitudDistribucion(Long id, String comentarioRevision) {
        SolicitudDistribucionEntity solicitud = solicitudDistribucionRepository.findById(id).orElse(null);
        if (solicitud == null) {
            return ObservarSolicitudDistribucionResult.SOLICITUD_NOT_FOUND;
        }

        if (comentarioRevision == null || comentarioRevision.trim().isEmpty()) {
            return ObservarSolicitudDistribucionResult.COMENTARIO_REVISION_EMPTY;
        }

        if (!SolicitudDistribucionEstado.ENVIADA.name().equalsIgnoreCase(solicitud.getEstado())) {
            return ObservarSolicitudDistribucionResult.ESTADO_INVALIDO;
        }

        solicitud.setEstado(SolicitudDistribucionEstado.OBSERVADA.name());
        solicitud.setComentarioRevision(comentarioRevision.trim());
        solicitudDistribucionRepository.save(solicitud);
        return ObservarSolicitudDistribucionResult.UPDATED;
    }

    /**
     * Cancels a request unless it already reached non-cancelable states.
     *
     * @param id request identifier
     * @return operation result
     */
    @Transactional
    public CancelarSolicitudDistribucionResult cancelarSolicitudDistribucion(Long id) {
        SolicitudDistribucionEntity solicitud = solicitudDistribucionRepository.findById(id).orElse(null);
        if (solicitud == null) {
            return CancelarSolicitudDistribucionResult.SOLICITUD_NOT_FOUND;
        }

        if (SolicitudDistribucionEstado.DESPACHADA.name().equalsIgnoreCase(solicitud.getEstado())
            || SolicitudDistribucionEstado.ENTREGADA.name().equalsIgnoreCase(solicitud.getEstado())
            || SolicitudDistribucionEstado.PARCIAL.name().equalsIgnoreCase(solicitud.getEstado())) {
            return CancelarSolicitudDistribucionResult.ESTADO_INVALIDO;
        }

        solicitud.setEstado(SolicitudDistribucionEstado.CANCELADA.name());
        solicitudDistribucionRepository.save(solicitud);
        return CancelarSolicitudDistribucionResult.UPDATED;
    }

    /**
     * Approves request details and transitions the request to {@code APROBADA}.
     *
     * @param id request identifier
     * @param dto approval payload
     * @return operation result
     */
    @Transactional
    public AprobarSolicitudDistribucionResult aprobarSolicitudDistribucion(Long id, AprobarSolicitudDTO dto) {
        SolicitudDistribucionEntity solicitud = solicitudDistribucionRepository.findById(id).orElse(null);
        if (solicitud == null) {
            return AprobarSolicitudDistribucionResult.SOLICITUD_NOT_FOUND;
        }

        if (!SolicitudDistribucionEstado.ENVIADA.name().equalsIgnoreCase(solicitud.getEstado())
            && !SolicitudDistribucionEstado.OBSERVADA.name().equalsIgnoreCase(solicitud.getEstado())) {
            return AprobarSolicitudDistribucionResult.ESTADO_INVALIDO;
        }

        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            return AprobarSolicitudDistribucionResult.ITEMS_EMPTY;
        }

        AprobarSolicitudDistribucionResult validationResult = validateAprobacion(dto, solicitud.getId());
        if (validationResult != null) {
            return validationResult;
        }

        applyAprobacion(dto, solicitud);
        solicitud.setEstado(SolicitudDistribucionEstado.APROBADA.name());
        if (dto.getComentarioRevision() != null) {
            solicitud.setComentarioRevision(dto.getComentarioRevision());
        }
        solicitudDistribucionRepository.save(solicitud);
        return AprobarSolicitudDistribucionResult.UPDATED;
    }

    /**
     * Returns one request by id.
     *
     * @param id request identifier
     * @return optional request
     */
    public Optional<SolicitudDistribucionEntity> getSolicitudDistribucionById(Long id) {
        return solicitudDistribucionRepository.findById(id);
    }

    /**
     * Returns paginated requests using optional filters and requester scope.
     *
     * @param userSedeId requester's branch id (when role is {@code SEDE})
     * @param sedeId optional branch filter for non-branch users
     * @param periodoAcademico optional academic period
     * @param estado optional request status
     * @param prioridad optional priority
     * @param fechaDesde optional lower bound for request date
     * @param fechaHasta optional upper bound for request date
     * @param page page index
     * @param size page size
     * @param sort sort direction token
     * @return paginated requests
     */
    public Page<SolicitudDistribucionEntity> getSolicitudesDistribucionByFilters(
        Long userSedeId, Long sedeId, String periodoAcademico, String estado, String prioridad, LocalDate fechaDesde,
        LocalDate fechaHasta, int page, int size, String sort
    ) {
        Specification<SolicitudDistribucionEntity> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (userSedeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sedeIcpna").get("id"), userSedeId));
        }
        if (sedeId != null && userSedeId == null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sedeIcpna").get("id"), sedeId));
        }
        if (periodoAcademico != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("periodoAcademico"), periodoAcademico));
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado.toUpperCase()));
        }
        if (prioridad != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("prioridad"), prioridad.toUpperCase()));
        }
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaSolicitud"), fechaDesde));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaSolicitud"), fechaHasta));
        }

        Sort.Direction direction = "descending".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "id"));

        return  solicitudDistribucionRepository.findAll(spec, pageable);
    }

    /**
     * Validates create request detail lines:
     * non-zero quantities, no duplicate materials and existing materials.
     *
     * @param dto create payload
     * @return first validation error or {@code null} when valid
     */
    private CreateSolicitudDistribucionResult validateItems(SolicitudDistribucionDTO dto) {
        Set<Long> materialIds = new HashSet<>();

        for (SolicitudDistribucionDetalleDTO item : dto.getItems()) {
            if (item.getCantidadSolicitada() == null || item.getCantidadSolicitada() <= 0) {
                return CreateSolicitudDistribucionResult.CANTIDAD_SOLICITADA_NOT_VALID;
            }
            if (!materialIds.add(item.getIdMaterial())) {
                return CreateSolicitudDistribucionResult.MATERIAL_DUPLICATE;
            }
            if (materialAcademicoRepository.findById(item.getIdMaterial()).isEmpty()) {
                return CreateSolicitudDistribucionResult.MATERIAL_NOT_FOUND;
            }
        }
        return null;
    }

    /**
     * Validates an approval payload against request details and available stock.
     *
     * @param dto approval payload
     * @param solicitudId request identifier
     * @return first validation error or {@code null} when valid
     */
    private AprobarSolicitudDistribucionResult validateAprobacion(AprobarSolicitudDTO dto, Long solicitudId) {
        boolean hasApprovedItem = false;

        for (AprobarSolicitudDetalleDTO item : dto.getItems()) {
            if (item.getDetalleId() == null || item.getCantidadAprobada() == null) {
                return AprobarSolicitudDistribucionResult.CANTIDAD_APROBADA_NOT_VALID;
            }

            SolicitudDistribucionDetalleEntity detalle =
                solicitudDistribucionDetalleRepository.findById(item.getDetalleId()).orElse(null);
            if (detalle == null) {
                return AprobarSolicitudDistribucionResult.DETALLE_NOT_FOUND;
            }
            if (!detalle.getSolicitud().getId().equals(solicitudId)) {
                return AprobarSolicitudDistribucionResult.DETALLE_NOT_BELONG_TO_SOLICITUD;
            }
            if (item.getCantidadAprobada() < 0 || item.getCantidadAprobada() > detalle.getCantidadSolicitada()) {
                return AprobarSolicitudDistribucionResult.CANTIDAD_APROBADA_NOT_VALID;
            }
            if (item.getCantidadAprobada() > 0) {
                hasApprovedItem = true;
            }

            Integer stockDisponible = loteIngresoRepository.sumCantidadDisponibleByMaterialIdAndEstado(
                detalle.getMaterial().getId(),
                LoteIngresoEstado.DISPONIBLE.name()
            );
            if (item.getCantidadAprobada() > stockDisponible) {
                return AprobarSolicitudDistribucionResult.STOCK_INSUFFICIENT;
            }
        }

        if (!hasApprovedItem) {
            return AprobarSolicitudDistribucionResult.CANTIDAD_APROBADA_NOT_VALID;
        }

        return null;
    }

    /**
     * Validates update payload in context of current request and target branch.
     *
     * @param dto update payload
     * @param solicitud current request entity
     * @param sede target branch
     * @return first validation error or {@code null} when valid
     */
    private UpdateSolicitudDistribucionResult validateSolicitudDistribucionDTO(
        SolicitudDistribucionDTO dto, SolicitudDistribucionEntity solicitud,SedeIcpnaEntity sede
    ) {
        String periodoAcademico = solicitud.getPeriodoAcademico();

        if (dto.getCodigo() != null) {
            if (dto.getCodigo().trim().isEmpty()) {
                return UpdateSolicitudDistribucionResult.CODIGO_EMPTY;
            }
        }

        if (!SedeEstado.ACTIVA.name().equalsIgnoreCase(sede.getEstado())) {
            return UpdateSolicitudDistribucionResult.SEDE_NOT_ACTIVE;
        }

        if (dto.getPeriodoAcademico() != null) {
            if (dto.getPeriodoAcademico().trim().isEmpty()) {
                return UpdateSolicitudDistribucionResult.PERIODO_NOT_VALID;
            }
            try {
                YearMonth.parse(dto.getPeriodoAcademico().trim());
                periodoAcademico = dto.getPeriodoAcademico().trim();
            } catch (DateTimeParseException e) {
                return UpdateSolicitudDistribucionResult.PERIODO_NOT_VALID;
            }
        }

        if (dto.getPrioridad() != null) {
            if (dto.getPrioridad().trim().isEmpty()) {
                return UpdateSolicitudDistribucionResult.PRIORIDAD_NOT_VALID;
            }
            try {
                SolicitudDistribucionPrioridad.valueOf(dto.getPrioridad().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return UpdateSolicitudDistribucionResult.PRIORIDAD_NOT_VALID;
            }
        }

        if (solicitudDistribucionRepository.existsBySedeIcpnaIdAndPeriodoAcademicoAndEstadoNotAndIdNot(
            sede.getId(), periodoAcademico, SolicitudDistribucionEstado.CANCELADA.name(), solicitud.getId())) {
            return UpdateSolicitudDistribucionResult.SOLICITUD_DUPLICADA;
        }

        if (dto.getItems() != null) {
            if (dto.getItems().isEmpty()) {
                return UpdateSolicitudDistribucionResult.ITEMS_EMPTY;
            }
            return validateUpdateItems(dto);
        }

        return null;
    }

    /**
     * Validates detail lines provided in update payload.
     *
     * @param dto update payload
     * @return first validation error or {@code null} when valid
     */
    private UpdateSolicitudDistribucionResult validateUpdateItems(SolicitudDistribucionDTO dto) {
        Set<Long> materialIds = new HashSet<>();

        for (SolicitudDistribucionDetalleDTO item : dto.getItems()) {
            if (item.getIdMaterial() == null) {
                return UpdateSolicitudDistribucionResult.MATERIAL_REQUIRED;
            }
            if (item.getCantidadSolicitada() == null || item.getCantidadSolicitada() <= 0) {
                return UpdateSolicitudDistribucionResult.CANTIDAD_SOLICITADA_NOT_VALID;
            }
            if (!materialIds.add(item.getIdMaterial())) {
                return UpdateSolicitudDistribucionResult.MATERIAL_DUPLICATE;
            }
            if (materialAcademicoRepository.findById(item.getIdMaterial()).isEmpty()) {
                return UpdateSolicitudDistribucionResult.MATERIAL_NOT_FOUND;
            }
        }

        return null;
    }

    /**
     * Applies non-null update fields and optionally replaces request details.
     *
     * @param dto update payload
     * @param solicitud target request
     * @param sede resolved target branch
     */
    private void applyChanges(SolicitudDistribucionDTO dto, SolicitudDistribucionEntity solicitud, SedeIcpnaEntity sede) {
        if (dto.getCodigo() != null) {
            solicitud.setCodigo(dto.getCodigo());
        }
        if (dto.getIdSede() != null) {
            solicitud.setSedeIcpna(sede);
        }
        if (dto.getPeriodoAcademico() != null) {
            solicitud.setPeriodoAcademico(dto.getPeriodoAcademico().trim());
        }
        if (dto.getPrioridad() != null) {
            SolicitudDistribucionPrioridad prioridad = SolicitudDistribucionPrioridad.valueOf(dto.getPrioridad().trim().toUpperCase());
            solicitud.setPrioridad(prioridad.name());
            solicitud.setFechaLimiteRevision(calcularFechaLimiteRevision(prioridad));
        }
        if (dto.getComentarioSolicitud() != null) {
            solicitud.setComentarioSolicitud(dto.getComentarioSolicitud());
        }
        if (dto.getItems() != null) {
            reemplazarItems(dto, solicitud);
        }
    }

    /**
     * Persists approved quantities for each detail id in the approval payload.
     *
     * @param dto approval payload
     * @param solicitud target request
     */
    private void applyAprobacion(AprobarSolicitudDTO dto, SolicitudDistribucionEntity solicitud) {
        for (AprobarSolicitudDetalleDTO item : dto.getItems()) {
            SolicitudDistribucionDetalleEntity detalle =
                solicitudDistribucionDetalleRepository.findById(item.getDetalleId()).orElseThrow();
            detalle.setCantidadAprobada(item.getCantidadAprobada());
            solicitudDistribucionDetalleRepository.save(detalle);
        }
    }

    /**
     * Replaces all existing request details with payload items.
     *
     * @param dto update payload
     * @param solicitud target request
     */
    private void reemplazarItems(SolicitudDistribucionDTO dto, SolicitudDistribucionEntity solicitud) {
        solicitudDistribucionDetalleRepository.deleteBySolicitudId(solicitud.getId());

        for (SolicitudDistribucionDetalleDTO item : dto.getItems()) {
            MaterialAcademicoEntity material = materialAcademicoRepository.findById(item.getIdMaterial()).orElseThrow();

            SolicitudDistribucionDetalleEntity detalle = new SolicitudDistribucionDetalleEntity();
            detalle.setSolicitud(solicitud);
            detalle.setMaterial(material);
            detalle.setCantidadSolicitada(item.getCantidadSolicitada());
            detalle.setCantidadAprobada(0);
            detalle.setComentarioItem(item.getComentarioItem());
            solicitudDistribucionDetalleRepository.save(detalle);
        }
    }

    /**
     * Resolves branch to use on update:
     * keeps current branch when payload does not include a new branch id.
     *
     * @param dto update payload
     * @param solicitud current request
     * @return resolved branch or {@code null} when requested branch does not exist
     */
    private SedeIcpnaEntity obtenerSedeParaUpdate(SolicitudDistribucionDTO dto, SolicitudDistribucionEntity solicitud) {
        if (dto.getIdSede() == null) {
            return solicitud.getSedeIcpna();
        }
        return sedeIcpnaRepository.findById(dto.getIdSede()).orElse(null);
    }

    /**
     * Calculates review deadline according to request priority.
     *
     * @param prioridad request priority
     * @return deadline date
     */
    private LocalDate calcularFechaLimiteRevision(SolicitudDistribucionPrioridad prioridad) {
        return switch (prioridad) {
            case NORMAL -> LocalDate.now().plusDays(4);
            case ALTA -> LocalDate.now().plusDays(2);
            case URGENTE -> LocalDate.now().plusDays(1);
        };
    }
}
