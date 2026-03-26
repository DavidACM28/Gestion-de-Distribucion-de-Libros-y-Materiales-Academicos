package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.MaterialAcademicoDTO;
import pe.incubadora.backend.entities.MaterialAcademicoEntity;
import pe.incubadora.backend.repositories.MaterialAcademicoRepository;
import pe.incubadora.backend.utils.materialAcademico.CreateMaterialAcademicoResult;
import pe.incubadora.backend.utils.materialAcademico.MaterialAcademicoCategoria;
import pe.incubadora.backend.utils.materialAcademico.MaterialAcademicoNivel;
import pe.incubadora.backend.utils.materialAcademico.MaterialAcademicoUnidadMedida;

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
}
