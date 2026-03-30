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
import pe.incubadora.backend.dtos.SolicitudDistribucionDTO;
import pe.incubadora.backend.services.SolicitudDistribucionService;
import pe.incubadora.backend.utils.solicitudDistribucion.CreateSolicitudDistribucionResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SolicitudDistribucionController {
    @Autowired
    private SolicitudDistribucionService solicitudDistribucionService;

    @PostMapping("/solicitudes")
    public ResponseEntity<Object> createSolicitudDistribucion(
        @Valid @RequestBody SolicitudDistribucionDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            CreateSolicitudDistribucionResult resultado = solicitudDistribucionService.createSolicitudDistribucion(dto);
            return switch (resultado) {
                case SEDE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("SEDE_NOT_FOUND", "Sede no encontrada"));
                case SEDE_NOT_ACTIVE -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La sede proporcionada no se encuentra activa"));
                case SOLICITUD_DUPLICADA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Ya existe una solicitud activa para este periodo académico en esta sede"));
                case ITEMS_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La solicitud debe tener al menos un item"));
                case PERIODO_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Periodo académico inválido, use formato: yyyy-MM"));
                case PRIORIDAD_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Prioridad inválida, use: NORMAL | ALTA | URGENTE"));
                case CANTIDAD_SOLICITADA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Cantidad solicitada inválida, debe ser mayor a 0"));
                case MATERIAL_DUPLICATE -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El material no se puede duplicar"));
                case MATERIAL_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "No se encontró el material"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se creó la solicitud con éxito");
            };
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CODIGO_CONFLICT", "Ya existe una solicitud con este código"));
        }
    }
}
