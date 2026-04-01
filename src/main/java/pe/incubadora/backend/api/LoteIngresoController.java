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
import pe.incubadora.backend.dtos.LoteIngresoDTO;
import pe.incubadora.backend.entities.LoteIngresoEntity;
import pe.incubadora.backend.services.LoteIngresoService;
import pe.incubadora.backend.utils.loteIngreso.CreateLoteIngresoResult;
import pe.incubadora.backend.utils.loteIngreso.LoteFueraDeVigenciaResult;
import pe.incubadora.backend.utils.loteIngreso.UpdateLoteIngresoResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
/**
 * Exposes endpoints for inbound stock batches (lotes de ingreso).
 */
public class LoteIngresoController {
    @Autowired
    private LoteIngresoService loteIngresoService;

    /**
     * Creates a new inbound batch.
     *
     * @param dto inbound batch payload
     * @param result validation result
     * @return created response or validation/business errors
     */
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
                case FIN_VIGENCIA_REQUIRED -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La fecha de fin de vigencia es requerida para este material"));
                case FECHA_VIGENCIA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La fecha de fin de vigencia no puede ser antes de la fecha de ingreso"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se creó el lote de ingreso con éxito");
            };
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CODIGO_CONFLICT", "Ya existe un lote de ingreso con este código"));
        }
    }

    /**
     * Updates an inbound batch.
     *
     * @param dto patch-like payload (non-null fields are applied)
     * @param id batch identifier
     * @return updated response or validation/business errors
     */
    @PutMapping("/lotes/{id}")
    public ResponseEntity<Object> updateLoteIngreso(@RequestBody LoteIngresoDTO dto, @PathVariable Long id) {
        try {
            UpdateLoteIngresoResult resultado = loteIngresoService.updateLoteIngreso(dto, id);
            return switch (resultado) {
                case LOTE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("LOTE_NOT_FOUND", "No se encontró el lote de ingreso"));
                case MATERIAL_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("MATERIAL_NOT_FOUND", "No se encontró el material"));
                case CODIGO_LOTE_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El código de lote no puede ser vacío"));
                case FECHA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Fecha inválida, use formato yyyy-MM-dd"));
                case CANTIDAD_INGRESADA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La cantidad ingresada debe ser mayor a 0 y no puede ser menor a la cantidad disponible"));
                case PROVEEDOR_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El proveedor no puede ser vacío"));
                case FIN_VIGENCIA_REQUIRED -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La fecha de fin de vigencia es requerida para este material"));
                case FECHA_VIGENCIA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La fecha de fin de vigencia no puede ser antes de la fecha de ingreso"));
                case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se actualizó el lote de ingreso con éxito");
            };
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CODIGO_CONFLICT", "Ya existe un lote de ingreso con este código"));
        }
    }

    /**
     * Marks an inbound batch as out of validity.
     *
     * @param id batch identifier
     * @return updated response or not found error
     */
    @PatchMapping("/lotes/{id}/fuera-vigencia")
    public ResponseEntity<Object> quitarVigenciaLoteIngreso(@PathVariable Long id) {
        LoteFueraDeVigenciaResult resultado = loteIngresoService.fueraDeVigenciaLoteIngreso(id);
        return switch (resultado) {
            case LOTE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("LOTE_NOT_FOUND", "No se encontró lote de ingreso"));
            case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se actualizó el estado a fuera de vigencia");
        };
    }

    /**
     * Returns paginated inbound batches.
     *
     * @param page page index
     * @return paginated batch list
     */
    @GetMapping("/lotes")
    public ResponseEntity<Object> getLotes(@RequestParam int page) {
        Pageable pageable = Pageable.ofSize(10).withPage(page);
        return ResponseEntity.status(HttpStatus.OK).body(loteIngresoService.getLotes(pageable));
    }

    /**
     * Returns one inbound batch by id.
     *
     * @param id batch identifier
     * @return batch response or not found error
     */
    @GetMapping("/lotes/{id}")
    public ResponseEntity<Object> getLoteIngresoById(@PathVariable Long id) {
        LoteIngresoEntity lote = loteIngresoService.getLoteIngresoById(id).orElse(null);
        if (lote == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("LOTE_NOT_FOUND", "No se encontró el lote"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(lote);
    }
}
