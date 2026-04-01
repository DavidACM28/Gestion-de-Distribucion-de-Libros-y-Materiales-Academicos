package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.incubadora.backend.dtos.AjusteMovimientoInventarioDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.entities.MovimientoInventarioEntity;
import pe.incubadora.backend.services.MovimientoInventarioService;
import pe.incubadora.backend.utils.movimientoInventario.CreateAjusteMovimientoInventarioResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MovimientoInventarioController {
    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatchException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegurese de que los filtros se envien con el formato correcto"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegurese de que los filtros se envien con el formato correcto"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingServletRequestParameterException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Los parametros: size, page, y sort, son obligatorios"));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Object> handleDateTimeParseException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Fecha invalida. Use formato yyyy-MM-dd"));
    }

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

    @GetMapping("/movimientos/{id}")
    public ResponseEntity<Object> getMovimientoInventarioById(@PathVariable Long id) {
        MovimientoInventarioEntity movimiento = movimientoInventarioService.getMovimientoInventarioById(id).orElse(null);
        if (movimiento == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("MOVIMIENTO_NOT_FOUND", "No se encontró el movimiento de inventario"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(movimiento);
    }

    @GetMapping("/movimientos")
    public ResponseEntity<Object> getMovimientos(
        @RequestParam(required = false) Long materialId, @RequestParam(required = false) Long loteId,
        @RequestParam(required = false) String tipoMovimiento, @RequestParam(required = false) String fechaDesde,
        @RequestParam(required = false) String fechaHasta, @RequestParam int page, @RequestParam int size,
        @RequestParam String sort
    ) {
        LocalDate desde = fechaDesde != null ? LocalDate.parse(fechaDesde, DateTimeFormatter.ISO_DATE) : null;
        LocalDate hasta = fechaHasta != null ? LocalDate.parse(fechaHasta, DateTimeFormatter.ISO_DATE) : null;
        if (desde != null && hasta != null && !desde.isBefore(hasta) && !desde.isEqual(hasta)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "La fecha limite de búsqueda no puede ser anterior a la fecha de inicio de busqueda"));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El número de pagina no puede ser menor a 0"));
        }
        if (size <= 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El tamaño de página debe ser mayor a 0"));
        }

        Page<MovimientoInventarioEntity> movimientos = movimientoInventarioService.getMovimientosByFilters(
            materialId, loteId, tipoMovimiento, desde, hasta, page, size, sort
        );
        return ResponseEntity.status(HttpStatus.OK).body(movimientos);
    }
}
