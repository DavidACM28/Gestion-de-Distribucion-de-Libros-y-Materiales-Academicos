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
import pe.incubadora.backend.dtos.RegistrarRecepcionEntregaDTO;
import pe.incubadora.backend.entities.EntregaMaterialDetalleEntity;
import pe.incubadora.backend.entities.EntregaMaterialEntity;
import pe.incubadora.backend.entities.LoteIngresoEntity;
import pe.incubadora.backend.entities.MovimientoInventarioEntity;
import pe.incubadora.backend.entities.SolicitudDistribucionDetalleEntity;
import pe.incubadora.backend.entities.SolicitudDistribucionEntity;
import pe.incubadora.backend.repositories.EntregaMaterialDetalleRepository;
import pe.incubadora.backend.repositories.EntregaMaterialRepository;
import pe.incubadora.backend.repositories.LoteIngresoRepository;
import pe.incubadora.backend.repositories.MovimientoInventarioRepository;
import pe.incubadora.backend.repositories.SolicitudDistribucionDetalleRepository;
import pe.incubadora.backend.repositories.SolicitudDistribucionRepository;
import pe.incubadora.backend.utils.entregaMaterial.CreateEntregaMaterialResult;
import pe.incubadora.backend.utils.entregaMaterial.DespacharEntregaMaterialResult;
import pe.incubadora.backend.utils.entregaMaterial.EnRutaEntregaMaterialResult;
import pe.incubadora.backend.utils.entregaMaterial.EntregaEstado;
import pe.incubadora.backend.utils.entregaMaterial.RegistrarRecepcionEntregaMaterialResult;
import pe.incubadora.backend.utils.loteIngreso.LoteIngresoEstado;
import pe.incubadora.backend.utils.movimientoInventario.TipoAjusteMovimiento;
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
    @Autowired
    private LoteIngresoRepository loteIngresoRepository;
    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

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

    @Transactional
    public DespacharEntregaMaterialResult despacharEntregaMaterial(Long id) {
        EntregaMaterialEntity entrega = entregaMaterialRepository.findById(id).orElse(null);
        if (entrega == null) {
            return DespacharEntregaMaterialResult.ENTREGA_NOT_FOUND;
        }
        if (!EntregaEstado.PROGRAMADA.name().equalsIgnoreCase(entrega.getEstadoEntrega())) {
            return DespacharEntregaMaterialResult.ESTADO_INVALIDO;
        }

        List<EntregaMaterialDetalleEntity> detalles = entregaMaterialDetalleRepository.findByEntregaId(entrega.getId());
        if (detalles.isEmpty()) {
            return DespacharEntregaMaterialResult.DETALLE_EMPTY;
        }

        for (EntregaMaterialDetalleEntity detalle : detalles) {
            Long materialId = detalle.getMaterial().getId();
            List<LoteIngresoEntity> lotes = loteIngresoRepository.findDisponiblesOrdenFefoByMaterialId(
                materialId, LoteIngresoEstado.DISPONIBLE.name(), LocalDate.now()
            );
            int cantidadNecesaria = detalle.getCantidad();
            int stockDisponible = lotes.stream().mapToInt(LoteIngresoEntity::getCantidadDisponible).sum();
            if (stockDisponible < cantidadNecesaria) {
                return DespacharEntregaMaterialResult.STOCK_INSUFFICIENT;
            }
        }

        entregaMaterialDetalleRepository.deleteByEntregaId(entrega.getId());

        for (EntregaMaterialDetalleEntity detalle : detalles) {
            Long materialId = detalle.getMaterial().getId();
            int cantidadPendiente = detalle.getCantidad();
            List<LoteIngresoEntity> lotes = loteIngresoRepository.findDisponiblesOrdenFefoByMaterialId(
                materialId, LoteIngresoEstado.DISPONIBLE.name(), LocalDate.now()
            );
            int indiceLote = 0;

            while (cantidadPendiente > 0 && indiceLote < lotes.size()) {
                LoteIngresoEntity lote = lotes.get(indiceLote);
                if (lote.getCantidadDisponible() == 0) {
                    indiceLote++;
                    continue;
                }

                int cantidadTomada = Math.min(cantidadPendiente, lote.getCantidadDisponible());

                EntregaMaterialDetalleEntity detalleDespachado = new EntregaMaterialDetalleEntity();
                detalleDespachado.setEntrega(entrega);
                detalleDespachado.setMaterial(detalle.getMaterial());
                detalleDespachado.setLote(lote);
                detalleDespachado.setCantidad(cantidadTomada);
                entregaMaterialDetalleRepository.save(detalleDespachado);

                MovimientoInventarioEntity movimiento = new MovimientoInventarioEntity();
                movimiento.setMaterialAcademico(detalle.getMaterial());
                movimiento.setLote(lote);
                movimiento.setFecha(LocalDate.now());
                movimiento.setTipoMovimiento(TipoAjusteMovimiento.SALIDA.name());
                movimiento.setCantidad(cantidadTomada);
                movimiento.setReferenciaTipo("ENTREGA");
                movimiento.setReferenciaId(entrega.getId());
                movimiento.setComentario(entrega.getComentario());
                movimientoInventarioRepository.save(movimiento);

                lote.setCantidadDisponible(lote.getCantidadDisponible() - cantidadTomada);
                if (lote.getCantidadDisponible() == 0) {
                    lote.setEstado(LoteIngresoEstado.AGOTADO.name());
                    indiceLote++;
                }
                loteIngresoRepository.save(lote);
                cantidadPendiente -= cantidadTomada;
            }
        }

        entrega.setFechaDespacho(LocalDate.now());
        entrega.setEstadoEntrega(EntregaEstado.DESPACHADA.name());
        entregaMaterialRepository.save(entrega);

        SolicitudDistribucionEntity solicitud = entrega.getSolicitud();
        solicitud.setEstado(SolicitudDistribucionEstado.DESPACHADA.name());
        solicitudDistribucionRepository.save(solicitud);

        return DespacharEntregaMaterialResult.UPDATED;
    }

    @Transactional
    public EnRutaEntregaMaterialResult enRutaEntregaMaterial(Long id) {
        EntregaMaterialEntity entrega = entregaMaterialRepository.findById(id).orElse(null);
        if (entrega == null) {
            return EnRutaEntregaMaterialResult.ENTREGA_NOT_FOUND;
        }
        if (!EntregaEstado.DESPACHADA.name().equalsIgnoreCase(entrega.getEstadoEntrega())) {
            return EnRutaEntregaMaterialResult.ESTADO_INVALIDO;
        }

        entrega.setEstadoEntrega(EntregaEstado.EN_RUTA.name());
        entregaMaterialRepository.save(entrega);
        return EnRutaEntregaMaterialResult.UPDATED;
    }

    @Transactional
    public RegistrarRecepcionEntregaMaterialResult registrarRecepcionEntregaMaterial(
        Long id,
        RegistrarRecepcionEntregaDTO dto
    ) {
        EntregaMaterialEntity entrega = entregaMaterialRepository.findById(id).orElse(null);
        if (entrega == null) {
            return RegistrarRecepcionEntregaMaterialResult.ENTREGA_NOT_FOUND;
        }
        if (!EntregaEstado.EN_RUTA.name().equalsIgnoreCase(entrega.getEstadoEntrega())) {
            return RegistrarRecepcionEntregaMaterialResult.ESTADO_INVALIDO;
        }

        entrega.setFechaEntrega(LocalDate.now());
        entrega.setResponsableRecepcion(dto.getResponsableRecepcion().trim());
        entrega.setEstadoEntrega(dto.getConIncidencia() ? EntregaEstado.CON_INCIDENCIA.name() : EntregaEstado.ENTREGADA.name());
        entregaMaterialRepository.save(entrega);

        SolicitudDistribucionEntity solicitud = entrega.getSolicitud();
        solicitud.setEstado(dto.getConIncidencia() ? SolicitudDistribucionEstado.PARCIAL.name() : SolicitudDistribucionEstado.ENTREGADA.name());
        solicitudDistribucionRepository.save(solicitud);

        return RegistrarRecepcionEntregaMaterialResult.UPDATED;
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
