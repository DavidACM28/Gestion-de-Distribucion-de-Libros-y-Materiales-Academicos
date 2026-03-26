package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.MaterialAcademicoDTO;
import pe.incubadora.backend.entities.MaterialAcademicoEntity;
import pe.incubadora.backend.repositories.MaterialAcademicoRepository;
import pe.incubadora.backend.utils.materialAcademico.*;

@Service
public class MaterialAcademicoService {
    @Autowired
    private MaterialAcademicoRepository materialAcademicoRepository;

    @Transactional
    public CreateMaterialAcademicoResult createMaterialAcademico(MaterialAcademicoDTO dto) {
        MaterialAcademicoCategoria categoria;
        MaterialAcademicoNivel nivel;
        MaterialAcademicoUnidadMedida unidadMedida;
        try {
            categoria = MaterialAcademicoCategoria.valueOf(dto.getCategoria().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CreateMaterialAcademicoResult.CATEGORIA_NOT_VALID;
        }
        try {
            nivel = MaterialAcademicoNivel.valueOf(dto.getNivel().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CreateMaterialAcademicoResult.NIVEL_NOT_VALID;
        }
        try {
            unidadMedida = MaterialAcademicoUnidadMedida.valueOf(dto.getUnidadMedida().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CreateMaterialAcademicoResult.UNIDAD_MEDIDA_NOT_VALID;
        }

        MaterialAcademicoEntity materialAcademico = new MaterialAcademicoEntity();
        materialAcademico.setSku(dto.getSku());
        materialAcademico.setNombre(dto.getNombre());
        materialAcademico.setCategoria(categoria.name());
        materialAcademico.setNivel(nivel.name());
        materialAcademico.setUnidadMedida(unidadMedida.name());
        materialAcademico.setStockMinimo(dto.getStockMinimo());
        materialAcademico.setControlVigencia(dto.getControlVigencia());
        materialAcademico.setActivo(dto.getActivo());
        materialAcademicoRepository.save(materialAcademico);
        return CreateMaterialAcademicoResult.CREATED;
    }

    @Transactional
    public UpdateMaterialAcademicoResult updateMaterialAcademico(MaterialAcademicoDTO dto, Long id) {
        MaterialAcademicoEntity materialAcademico = materialAcademicoRepository.findById(id).orElse(null);
        if (materialAcademico == null) {
            return UpdateMaterialAcademicoResult.MATERIAL_ACADEMICO_NOT_FOUND;
        }
        UpdateMaterialAcademicoResult result = validateMaterialAcademicoDTO(dto);
        if (result != null) {
            return result;
        }
        applyChanges(dto, materialAcademico);
        materialAcademicoRepository.save(materialAcademico);
        return UpdateMaterialAcademicoResult.UPDATED;
    }

    public Page<MaterialAcademicoEntity> getMaterialesAcademicos(Pageable page) {
        return materialAcademicoRepository.findAll(page);
    }

    private UpdateMaterialAcademicoResult validateMaterialAcademicoDTO(MaterialAcademicoDTO dto) {
        if (dto.getSku() != null) {
            if (dto.getSku().trim().isEmpty()) {
                return UpdateMaterialAcademicoResult.SKU_EMPTY;
            }
        }
        if (dto.getNombre() != null) {
            if (dto.getNombre().trim().length() < 3) {
                return UpdateMaterialAcademicoResult.NOMBRE_NOT_VALID;
            }
        }
        if (dto.getCategoria() != null) {
            try {
                MaterialAcademicoCategoria.valueOf(dto.getCategoria().toUpperCase());
            } catch (IllegalArgumentException e) {
                return UpdateMaterialAcademicoResult.CATEGORIA_NOT_VALID;
            }
        }
        if (dto.getNivel() != null) {
            try {
                MaterialAcademicoNivel.valueOf(dto.getNivel().toUpperCase());
            } catch (IllegalArgumentException e) {
                return UpdateMaterialAcademicoResult.NIVEL_NOT_VALID;
            }
        }
        if (dto.getUnidadMedida() != null) {
            try {
                MaterialAcademicoUnidadMedida.valueOf(dto.getUnidadMedida().toUpperCase());
            } catch (IllegalArgumentException e) {
                return UpdateMaterialAcademicoResult.UNIDAD_MEDIDA_NOT_VALID;
            }
        }
        if (dto.getStockMinimo() != null) {
            if (dto.getStockMinimo() < 0) {
                return UpdateMaterialAcademicoResult.STOCK_MINIMO_NOT_VALID;
            }
        }
        return null;
    }

    private void applyChanges(MaterialAcademicoDTO dto, MaterialAcademicoEntity material) {
        if (dto.getSku() != null) {
            material.setSku(dto.getSku());
        }
        if (dto.getNombre() != null) {
            material.setNombre(dto.getNombre());
        }
        if (dto.getCategoria() != null) {
            material.setCategoria(dto.getCategoria().toUpperCase());
        }
        if (dto.getNivel() != null) {
            material.setNivel(dto.getNivel().toUpperCase());
        }
        if (dto.getUnidadMedida() != null) {
            material.setUnidadMedida(dto.getUnidadMedida().toUpperCase());
        }
        if (dto.getStockMinimo() != null) {
            material.setStockMinimo(dto.getStockMinimo());
        }
        if (dto.getControlVigencia() != null) {
            material.setControlVigencia(dto.getControlVigencia());
        }
        if (dto.getActivo() != null) {
            material.setActivo(dto.getActivo());
        }
    }
}
