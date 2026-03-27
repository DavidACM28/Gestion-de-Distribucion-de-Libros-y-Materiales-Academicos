package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.LoteIngresoDTO;
import pe.incubadora.backend.entities.LoteIngresoEntity;
import pe.incubadora.backend.entities.MaterialAcademicoEntity;
import pe.incubadora.backend.repositories.LoteIngresoRepository;
import pe.incubadora.backend.repositories.MaterialAcademicoRepository;
import pe.incubadora.backend.utils.loteIngreso.CreateLoteIngresoResult;
import pe.incubadora.backend.utils.loteIngreso.LoteIngresoEstado;

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
}
