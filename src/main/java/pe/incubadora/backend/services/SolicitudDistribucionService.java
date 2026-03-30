package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
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
import pe.incubadora.backend.utils.solicitudDistribucion.SolicitudDistribucionEstado;
import pe.incubadora.backend.utils.solicitudDistribucion.SolicitudDistribucionPrioridad;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
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
}
