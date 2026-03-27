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
import pe.incubadora.backend.dtos.LoteIngresoDTO;
import pe.incubadora.backend.services.LoteIngresoService;
import pe.incubadora.backend.utils.loteIngreso.CreateLoteIngresoResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LoteIngresoController {
    @Autowired
    private LoteIngresoService loteIngresoService;

    @PostMapping("/lotes")
    public ResponseEntity<Object> createLoteIngreso(@Valid @RequestBody LoteIngresoDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            CreateLoteIngresoResult resultado = loteIngresoService.createLoteIngreso(dto);
            return switch (resultado) {
                case MATERIAL_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("MATERIAL_NOT_FOUND", "No se encontró el material"));
                case FECHA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Fecha inválida, use formato yyyy-MM-dd"));
                case FIN_VIGENCIA_REQUIRED ->  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR","La fecha de fin de vigencia es requerida para este material"));
                case FECHA_VIGENCIA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La fecha de fin de vigencia no puede ser antes de la fecha de ingreso"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se creó el lote de ingreso con éxito");
            };
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CODIGO_CONFLICT", "Ya existe un lote de ingreso con este código"));
        }
    }
}
