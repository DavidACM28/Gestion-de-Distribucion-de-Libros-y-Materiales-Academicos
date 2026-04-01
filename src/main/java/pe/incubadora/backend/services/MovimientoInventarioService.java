package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class MovimientoInventarioService {
    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Autowired
    private MaterialAcademicoRepository materialAcademicoRepository;
    @Autowired
    private LoteIngresoRepository loteIngresoRepository;

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
}
