package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.EntregaMaterialDTO;
import pe.incubadora.backend.entities.EntregaMaterialDetalleEntity;
import pe.incubadora.backend.entities.EntregaMaterialEntity;
import pe.incubadora.backend.entities.SolicitudDistribucionDetalleEntity;
import pe.incubadora.backend.entities.SolicitudDistribucionEntity;
import pe.incubadora.backend.repositories.EntregaMaterialDetalleRepository;
import pe.incubadora.backend.repositories.EntregaMaterialRepository;
import pe.incubadora.backend.repositories.SolicitudDistribucionDetalleRepository;
import pe.incubadora.backend.repositories.SolicitudDistribucionRepository;
import pe.incubadora.backend.utils.entregaMaterial.CreateEntregaMaterialResult;
import pe.incubadora.backend.utils.entregaMaterial.EntregaEstado;
import pe.incubadora.backend.utils.solicitudDistribucion.SolicitudDistribucionEstado;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class EntregaMaterialService {
    @Autowired
    private EntregaMaterialRepository entregaMaterialRepository;
    @Autowired
    private EntregaMaterialDetalleRepository entregaMaterialDetalleRepository;
    @Autowired
    private SolicitudDistribucionRepository solicitudDistribucionRepository;
    @Autowired
    private SolicitudDistribucionDetalleRepository solicitudDistribucionDetalleRepository;

    @Transactional
    public CreateEntregaMaterialResult createEntregaMaterial(EntregaMaterialDTO dto) {
        SolicitudDistribucionEntity solicitud = solicitudDistribucionRepository.findById(dto.getSolicitudId()).orElse(null);
        if (solicitud == null) {
            return CreateEntregaMaterialResult.SOLICITUD_NOT_FOUND;
        }

        if (!SolicitudDistribucionEstado.APROBADA.name().equalsIgnoreCase(solicitud.getEstado())) {
            return CreateEntregaMaterialResult.SOLICITUD_ESTADO_NOT_VALID;
        }

        LocalDate fechaProgramada;
        try {
            fechaProgramada = LocalDate.parse(dto.getFechaProgramada().trim());
        } catch (DateTimeParseException e) {
            return CreateEntregaMaterialResult.FECHA_NOT_VALID;
        }

        if (fechaProgramada.isBefore(LocalDate.now())) {
            return CreateEntregaMaterialResult.FECHA_PROGRAMADA_NOT_VALID;
        }

        List<SolicitudDistribucionDetalleEntity> detallesSolicitud =
            solicitudDistribucionDetalleRepository.findBySolicitudId(solicitud.getId());
        List<SolicitudDistribucionDetalleEntity> detallesAprobados = detallesSolicitud.stream()
            .filter(d -> d.getCantidadAprobada() != null && d.getCantidadAprobada() > 0)
            .toList();

        if (detallesAprobados.isEmpty()) {
            return CreateEntregaMaterialResult.DETALLE_EMPTY;
        }

        EntregaMaterialEntity entrega = new EntregaMaterialEntity();
        entrega.setCodigo(dto.getCodigo().trim());
        entrega.setSolicitud(solicitud);
        entrega.setFechaProgramada(fechaProgramada);
        entrega.setEstadoEntrega(EntregaEstado.PROGRAMADA.name());
        entrega.setResponsableAlmacen(dto.getResponsableAlmacen().trim());
        entrega.setComentario(dto.getComentario());
        entrega = entregaMaterialRepository.save(entrega);

        for (SolicitudDistribucionDetalleEntity detalleSolicitud : detallesAprobados) {
            EntregaMaterialDetalleEntity detalleEntrega = new EntregaMaterialDetalleEntity();
            detalleEntrega.setEntrega(entrega);
            detalleEntrega.setMaterial(detalleSolicitud.getMaterial());
            detalleEntrega.setCantidad(detalleSolicitud.getCantidadAprobada());
            entregaMaterialDetalleRepository.save(detalleEntrega);
        }

        return CreateEntregaMaterialResult.CREATED;
    }

    public Optional<EntregaMaterialEntity> getEntregaMaterialById(Long id) {
        return entregaMaterialRepository.findById(id);
    }

    public Page<EntregaMaterialEntity> getEntregasByFilters(
        Long userSedeId, Long solicitudId, Long sedeId, String estadoEntrega, LocalDate fechaDesde,
        LocalDate fechaHasta, int page, int size, String sort
    ) {
        Specification<EntregaMaterialEntity> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (userSedeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("solicitud").get("sedeIcpna").get("id"), userSedeId));
        }
        if (sedeId != null && userSedeId == null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("solicitud").get("sedeIcpna").get("id"), sedeId));
        }
        if (solicitudId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("solicitud").get("id"), solicitudId));
        }
        if (estadoEntrega != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estadoEntrega"), estadoEntrega.toUpperCase()));
        }
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaProgramada"), fechaDesde));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaProgramada"), fechaHasta));
        }

        Sort.Direction direction = "descending".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "id"));

        return entregaMaterialRepository.findAll(spec, pageable);
    }
}
