package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.MaterialAcademicoDTO;
import pe.incubadora.backend.entities.MaterialAcademicoEntity;
import pe.incubadora.backend.services.MaterialAcademicoService;
import pe.incubadora.backend.utils.materialAcademico.CreateMaterialAcademicoResult;
import pe.incubadora.backend.utils.materialAcademico.UpdateMaterialAcademicoResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
/**
 * Exposes CRUD-style endpoints for academic materials.
 */
public class MaterialAcademicoController {
    @Autowired
    private MaterialAcademicoService materialAcademicoService;

    /**
     * Creates an academic material.
     *
     * @param dto material payload
     * @param result validation result
     * @return created response or validation/conflict errors
     */
    @PostMapping("/materiales")
    public ResponseEntity<Object> createMaterial(@Valid @RequestBody MaterialAcademicoDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            CreateMaterialAcademicoResult resultado = materialAcademicoService.createMaterialAcademico(dto);
            return switch (resultado) {
                case CATEGORIA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO(
                        "VALIDATION_ERROR",
                        "Categoría inválida, use: STUDENT_BOOK, WORKBOOK, READER, EXAM_PACK, DICCIONARIO. OTROS"));
                case NIVEL_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO(
                        "VALIDATION_ERROR",
                        "Nivel inválido, use: BASICO, INTERMEDIO, AVANZADO, KIDS, JUNIORS, GENERAL"));
                case UNIDAD_MEDIDA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO(
                        "VALIDATION_ERROR",
                        "Unidad de medida inválida, use: UNIDAD, PAQUETE, CAJA, KIT"));
                case CREATED ->
                    ResponseEntity.status(HttpStatus.CREATED).body("Se creó el material académico con éxito");
            };

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("SKU_CONFLICT", "Ya existe un material con este SKU"));
        }
    }

    /**
     * Updates an academic material.
     *
     * @param dto patch-like payload (non-null fields are applied)
     * @param id material identifier
     * @return updated response or business/validation errors
     */
    @PutMapping("/materiales/{id}")
    public ResponseEntity<Object> updateMaterial(@RequestBody MaterialAcademicoDTO dto, @PathVariable Long id) {
        try {
            UpdateMaterialAcademicoResult result = materialAcademicoService.updateMaterialAcademico(dto, id);
            return switch (result) {
                case MATERIAL_ACADEMICO_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("MATERIAL_ACADEMICO_NOT_FOUND", "No se encontró el material académico"));
                case SKU_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El SKU no puede ser vacío"));
                case NOMBRE_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El nombre debe tener 3 caracteres como mínimo"));
                case CATEGORIA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO(
                        "VALIDATION_ERROR",
                        "Categoría inválida, use: STUDENT_BOOK, WORKBOOK, READER, EXAM_PACK, DICCIONARIO. OTROS"));
                case NIVEL_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO(
                        "VALIDATION_ERROR",
                        "Nivel inválido, use: BASICO, INTERMEDIO, AVANZADO, KIDS, JUNIORS, GENERAL"));
                case UNIDAD_MEDIDA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO(
                        "VALIDATION_ERROR",
                        "Unidad de medida inválida, use: UNIDAD, PAQUETE, CAJA, KIT"));
                case STOCK_MINIMO_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El stock mínimo debe ser mayor o igual a 0"));
                case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se actualizó con éxito");
            };
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("SKU_CONFLICT", "Ya existe un material con este SKU"));
        }
    }

    /**
     * Returns paginated academic materials.
     *
     * @param page page index
     * @return paginated materials
     */
    @GetMapping("/materiales")
    public ResponseEntity<Object> getMateriales(@RequestParam int page) {
        Pageable pageable = Pageable.ofSize(10).withPage(page);
        return ResponseEntity.status(HttpStatus.OK).body(materialAcademicoService.getMaterialesAcademicos(pageable));
    }

    /**
     * Returns one academic material by id.
     *
     * @param id material identifier
     * @return material response or not found error
     */
    @GetMapping("/materiales/{id}")
    public ResponseEntity<Object> getMaterialById(@PathVariable Long id) {
        MaterialAcademicoEntity material = materialAcademicoService.getMaterialAcademico(id).orElse(null);
        if (material == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("MATERIAL_ACADEMICO_NOT_FOUND", "No se encontró el material académico"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(material);
    }
}
