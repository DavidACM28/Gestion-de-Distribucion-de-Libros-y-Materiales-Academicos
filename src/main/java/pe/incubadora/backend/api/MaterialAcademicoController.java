package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.MaterialAcademicoDTO;
import pe.incubadora.backend.services.MaterialAcademicoService;
import pe.incubadora.backend.utils.materialAcademico.CreateMaterialAcademicoResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MaterialAcademicoController {
    @Autowired
    private MaterialAcademicoService materialAcademicoService;

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
}
