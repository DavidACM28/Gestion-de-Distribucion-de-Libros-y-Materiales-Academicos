package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.LoteIngresoDTO;
import pe.incubadora.backend.entities.LoteIngresoEntity;
import pe.incubadora.backend.entities.MaterialAcademicoEntity;
import pe.incubadora.backend.repositories.LoteIngresoRepository;
import pe.incubadora.backend.repositories.MaterialAcademicoRepository;
import pe.incubadora.backend.utils.loteIngreso.CreateLoteIngresoResult;
import pe.incubadora.backend.utils.loteIngreso.LoteFueraDeVigenciaResult;
import pe.incubadora.backend.utils.loteIngreso.LoteIngresoEstado;
import pe.incubadora.backend.utils.loteIngreso.UpdateLoteIngresoResult;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class LoteIngresoService {
    @Autowired
    private LoteIngresoRepository loteIngresoRepository;
    @Autowired
    private MaterialAcademicoRepository materialAcademicoRepository;

    @Transactional
    public CreateLoteIngresoResult createLoteIngreso(LoteIngresoDTO dto) {
        MaterialAcademicoEntity material = materialAcademicoRepository.findById(dto.getIdMaterial()).orElse(null);
        LocalDate fechaIngreso;
        LocalDate fechaVigencia = null;
        if (material == null) {
            return CreateLoteIngresoResult.MATERIAL_NOT_FOUND;
        }
        try {
            fechaIngreso = LocalDate.parse(dto.getFechaIngreso());
            if (dto.getFechaVigencia() != null) {
                fechaVigencia = LocalDate.parse(dto.getFechaVigencia());
            }

        } catch (DateTimeParseException e) {
            return CreateLoteIngresoResult.FECHA_NOT_VALID;
        }
        if (material.isControlVigencia() && fechaVigencia == null) {
            return CreateLoteIngresoResult.FIN_VIGENCIA_REQUIRED;
        }
        if (fechaVigencia != null && fechaVigencia.isBefore(fechaIngreso)) {
            return CreateLoteIngresoResult.FECHA_VIGENCIA_NOT_VALID;
        }
        LoteIngresoEntity loteIngreso = new LoteIngresoEntity();
        loteIngreso.setCodigoLote(dto.getCodigoLote());
        loteIngreso.setMaterialAcademico(material);
        loteIngreso.setFechaIngreso(fechaIngreso);
        loteIngreso.setEdicion(dto.getEdicion());
        if (fechaVigencia != null) {
            loteIngreso.setFechaVigencia(fechaVigencia);
        }
        loteIngreso.setCantidadIngresada(dto.getCantidadIngresada());
        loteIngreso.setCantidadDisponible(dto.getCantidadIngresada());
        loteIngreso.setProveedor(dto.getProveedor());
        loteIngreso.setEstado(LoteIngresoEstado.DISPONIBLE.name());
        loteIngresoRepository.save(loteIngreso);
        return CreateLoteIngresoResult.CREATED;
    }

    @Transactional
    public UpdateLoteIngresoResult updateLoteIngreso(LoteIngresoDTO dto, Long id) {
        LoteIngresoEntity loteIngreso = loteIngresoRepository.findById(id).orElse(null);
        if (loteIngreso == null) {
            return UpdateLoteIngresoResult.LOTE_NOT_FOUND;
        }

        MaterialAcademicoEntity material = obtenerMaterialParaUpdate(dto, loteIngreso);
        if (material == null) {
            return UpdateLoteIngresoResult.MATERIAL_NOT_FOUND;
        }

        UpdateLoteIngresoResult result = validateLoteIngresoDTO(dto, loteIngreso, material);
        if (result != null) {
            return result;
        }

        applyChanges(dto, loteIngreso, material);
        loteIngresoRepository.save(loteIngreso);
        return UpdateLoteIngresoResult.UPDATED;
    }

    @Transactional
    public LoteFueraDeVigenciaResult fueraDeVigenciaLoteIngreso(Long id) {
        LoteIngresoEntity loteIngreso = loteIngresoRepository.findById(id).orElse(null);
        if (loteIngreso == null) {
            return LoteFueraDeVigenciaResult.LOTE_NOT_FOUND;
        }
        loteIngreso.setEstado(LoteIngresoEstado.FUERA_VIGENCIA.name());
        loteIngresoRepository.save(loteIngreso);
        return LoteFueraDeVigenciaResult.UPDATED;
    }

    public Page<LoteIngresoEntity> getLotes(Pageable page) {
        return loteIngresoRepository.findAll(page);
    }

    private UpdateLoteIngresoResult validateLoteIngresoDTO(
        LoteIngresoDTO dto,
        LoteIngresoEntity loteIngreso,
        MaterialAcademicoEntity material
    ) {
        LocalDate fechaIngreso = loteIngreso.getFechaIngreso();
        LocalDate fechaVigencia = loteIngreso.getFechaVigencia();

        if (dto.getCodigoLote() != null) {
            if (dto.getCodigoLote().trim().isEmpty()) {
                return UpdateLoteIngresoResult.CODIGO_LOTE_EMPTY;
            }
        }

        if (dto.getFechaIngreso() != null) {
            if (dto.getFechaIngreso().trim().isEmpty()) {
                return UpdateLoteIngresoResult.FECHA_NOT_VALID;
            }
            try {
                fechaIngreso = LocalDate.parse(dto.getFechaIngreso().trim());
            } catch (DateTimeParseException e) {
                return UpdateLoteIngresoResult.FECHA_NOT_VALID;
            }
        }

        if (dto.getFechaVigencia() != null) {
            if (dto.getFechaVigencia().trim().isEmpty()) {
                return UpdateLoteIngresoResult.FECHA_NOT_VALID;
            }
            try {
                fechaVigencia = LocalDate.parse(dto.getFechaVigencia().trim());
            } catch (DateTimeParseException e) {
                return UpdateLoteIngresoResult.FECHA_NOT_VALID;
            }
        }

        if (dto.getCantidadIngresada() != null) {
            if (dto.getCantidadIngresada() <= 0) {
                return UpdateLoteIngresoResult.CANTIDAD_INGRESADA_NOT_VALID;
            }
            if (dto.getCantidadIngresada() < loteIngreso.getCantidadDisponible()) {
                return UpdateLoteIngresoResult.CANTIDAD_INGRESADA_NOT_VALID;
            }
        }

        if (dto.getProveedor() != null) {
            if (dto.getProveedor().trim().isEmpty()) {
                return UpdateLoteIngresoResult.PROVEEDOR_EMPTY;
            }
        }

        if (material.isControlVigencia() && fechaVigencia == null) {
            return UpdateLoteIngresoResult.FIN_VIGENCIA_REQUIRED;
        }
        if (fechaVigencia != null && fechaVigencia.isBefore(fechaIngreso)) {
            return UpdateLoteIngresoResult.FECHA_VIGENCIA_NOT_VALID;
        }

        return null;
    }

    private void applyChanges(
        LoteIngresoDTO dto,
        LoteIngresoEntity loteIngreso,
        MaterialAcademicoEntity material
    ) {
        if (dto.getIdMaterial() != null) {
            loteIngreso.setMaterialAcademico(material);
        }
        if (dto.getCodigoLote() != null) {
            loteIngreso.setCodigoLote(dto.getCodigoLote());
        }
        if (dto.getFechaIngreso() != null) {
            loteIngreso.setFechaIngreso(LocalDate.parse(dto.getFechaIngreso().trim()));
        }
        if (dto.getEdicion() != null) {
            loteIngreso.setEdicion(dto.getEdicion());
        }
        if (dto.getFechaVigencia() != null) {
            loteIngreso.setFechaVigencia(LocalDate.parse(dto.getFechaVigencia().trim()));
        }
        if (dto.getCantidadIngresada() != null) {
            loteIngreso.setCantidadIngresada(dto.getCantidadIngresada());
        }
        if (dto.getProveedor() != null) {
            loteIngreso.setProveedor(dto.getProveedor());
        }
    }

    private MaterialAcademicoEntity obtenerMaterialParaUpdate(LoteIngresoDTO dto, LoteIngresoEntity loteIngreso) {
        if (dto.getIdMaterial() == null) {
            return loteIngreso.getMaterialAcademico();
        }

        return materialAcademicoRepository.findById(dto.getIdMaterial()).orElse(null);
    }
}
