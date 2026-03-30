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
import pe.incubadora.backend.entities.MaterialAcademicoEntity;
import pe.incubadora.backend.entities.SedeIcpnaEntity;
import pe.incubadora.backend.entities.SolicitudDistribucionDetalleEntity;
import pe.incubadora.backend.entities.SolicitudDistribucionEntity;
import pe.incubadora.backend.repositories.MaterialAcademicoRepository;
import pe.incubadora.backend.repositories.SedeIcpnaRepository;
import pe.incubadora.backend.repositories.SolicitudDistribucionDetalleRepository;
import pe.incubadora.backend.repositories.SolicitudDistribucionRepository;
import pe.incubadora.backend.utils.sedeIcpna.SedeEstado;
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
public class SolicitudDistribucionService {
    @Autowired
    private SolicitudDistribucionRepository solicitudDistribucionRepository;
    @Autowired
    private SolicitudDistribucionDetalleRepository solicitudDistribucionDetalleRepository;
    @Autowired
    private SedeIcpnaRepository sedeIcpnaRepository;
    @Autowired
    private MaterialAcademicoRepository materialAcademicoRepository;

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

    public Optional<SolicitudDistribucionEntity> getSolicitudDistribucionById(Long id) {
        return solicitudDistribucionRepository.findById(id);
    }

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

    private SedeIcpnaEntity obtenerSedeParaUpdate(SolicitudDistribucionDTO dto, SolicitudDistribucionEntity solicitud) {
        if (dto.getIdSede() == null) {
            return solicitud.getSedeIcpna();
        }
        return sedeIcpnaRepository.findById(dto.getIdSede()).orElse(null);
    }

    private LocalDate calcularFechaLimiteRevision(SolicitudDistribucionPrioridad prioridad) {
        return switch (prioridad) {
            case NORMAL -> LocalDate.now().plusDays(4);
            case ALTA -> LocalDate.now().plusDays(2);
            case URGENTE -> LocalDate.now().plusDays(1);
        };
    }
}
