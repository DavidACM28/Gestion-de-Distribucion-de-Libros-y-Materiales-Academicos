package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.incubadora.backend.dtos.AjusteMovimientoInventarioDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.services.MovimientoInventarioService;
import pe.incubadora.backend.utils.movimientoInventario.CreateAjusteMovimientoInventarioResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MovimientoInventarioController {
    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    @PostMapping("/movimientos/ajuste")
    public ResponseEntity<Object> createAjusteMovimientoInventario(
        @Valid @RequestBody AjusteMovimientoInventarioDTO dto, BindingResult result
    ) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            CreateAjusteMovimientoInventarioResult resultado =
                movimientoInventarioService.createAjusteMovimientoInventario(dto);
            return switch (resultado) {
                case MATERIAL_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("MATERIAL_NOT_FOUND", "No se encontró el material"));
                case LOTE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("LOTE_NOT_FOUND", "No se encontró el lote"));
                case LOTE_MATERIAL_CONFLICT -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El lote no corresponde al material"));
                case LOTE_NOT_VALID -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                    new ErrorResponseDTO("LOTE_NOT_VALID", "No se puede registrar ajuste en un lote fuera de vigencia"));
                case TIPO_AJUSTE_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Tipo de ajuste inválido, use INGRESO o SALIDA"));
                case STOCK_INSUFFICIENT -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                    new ErrorResponseDTO("STOCK_INSUFFICIENT", "La salida de ajuste supera el stock disponible"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se registró el ajuste de inventario con éxito");
            };
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("STOCK_CONFLICT", "El stock fue actualizado por otra operación. Intente nuevamente"));
        }
    }
}
