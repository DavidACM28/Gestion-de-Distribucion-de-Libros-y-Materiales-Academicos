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
import pe.incubadora.backend.dtos.EntregaMaterialDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.services.EntregaMaterialService;
import pe.incubadora.backend.utils.entregaMaterial.CreateEntregaMaterialResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class EntregaMaterialController {
    @Autowired
    private EntregaMaterialService entregaMaterialService;

    @PostMapping("/entregas")
    public ResponseEntity<Object> createEntregaMaterial(@Valid @RequestBody EntregaMaterialDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            CreateEntregaMaterialResult resultado = entregaMaterialService.createEntregaMaterial(dto);
            return switch (resultado) {
                case SOLICITUD_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
                case SOLICITUD_ESTADO_NOT_VALID -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("ESTADO_INVALIDO", "La solicitud debe estar en estado APROBADA"));
                case FECHA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Fecha inválida, use formato yyyy-MM-dd"));
                case FECHA_PROGRAMADA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La fecha programada no puede ser anterior a la fecha actual"));
                case DETALLE_EMPTY -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                    new ErrorResponseDTO("BUSINESS_RULE_VIOLATION", "La solicitud no tiene items aprobados para crear la entrega"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se creo la entrega con éxito");
            };
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CODIGO_CONFLICT", "Ya existe una entrega con este código"));
        }
    }
}
