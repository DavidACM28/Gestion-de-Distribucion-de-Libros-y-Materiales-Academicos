package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.incubadora.backend.dtos.EntregaMaterialDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.RegistrarRecepcionEntregaDTO;
import pe.incubadora.backend.entities.EntregaMaterialEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.services.EntregaMaterialService;
import pe.incubadora.backend.services.UsuarioService;
import pe.incubadora.backend.utils.Rol;
import pe.incubadora.backend.utils.entregaMaterial.CreateEntregaMaterialResult;
import pe.incubadora.backend.utils.entregaMaterial.DespacharEntregaMaterialResult;
import pe.incubadora.backend.utils.entregaMaterial.EnRutaEntregaMaterialResult;
import pe.incubadora.backend.utils.entregaMaterial.RegistrarRecepcionEntregaMaterialResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class EntregaMaterialController {
    @Autowired
    private EntregaMaterialService entregaMaterialService;
    @Autowired
    private UsuarioService usuarioService;

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
                    new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontro la solicitud"));
                case SOLICITUD_ESTADO_NOT_VALID -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("ESTADO_INVALIDO", "La solicitud debe estar en estado APROBADA"));
                case FECHA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Fecha invalida, use formato yyyy-MM-dd"));
                case FECHA_PROGRAMADA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La fecha programada no puede ser anterior a la fecha actual"));
                case DETALLE_EMPTY -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                    new ErrorResponseDTO("BUSINESS_RULE_VIOLATION", "La solicitud no tiene items aprobados para crear la entrega"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se creo la entrega con exito");
            };
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CODIGO_CONFLICT", "Ya existe una entrega con este codigo"));
        }
    }

    @GetMapping("/entregas/{id}")
    public ResponseEntity<Object> getEntregaMaterialById(@PathVariable Long id, Authentication authentication) {
        EntregaMaterialEntity entrega = entregaMaterialService.getEntregaMaterialById(id).orElse(null);
        if (entrega == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("ENTREGA_NOT_FOUND", "No se encontro la entrega"));
        }

        Rol rol = obtenerRol(authentication);
        Long sedeIdUsuario = obtenerSedeIdUsuario(authentication, rol);

        if (rol == Rol.SEDE && sedeIdUsuario == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
        }

        if (rol == Rol.SEDE && !sedeIdUsuario.equals(entrega.getSolicitud().getSedeIcpna().getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("ENTREGA_NOT_FOUND", "No se encontro la entrega"));
        }

        return ResponseEntity.status(HttpStatus.OK).body(entrega);
    }

    @GetMapping("/entregas")
    public ResponseEntity<Object> getEntregas(
        Authentication authentication, @RequestParam(required = false) Long solicitudId,
        @RequestParam(required = false) Long sedeId, @RequestParam(required = false) String estadoEntrega,
        @RequestParam(required = false) String fechaDesde, @RequestParam(required = false) String fechaHasta,
        @RequestParam int page, @RequestParam int size, @RequestParam String sort
    ) {
        Rol rol = obtenerRol(authentication);
        Long sedeIdUsuario = obtenerSedeIdUsuario(authentication, rol);

        if (rol == Rol.SEDE && sedeIdUsuario == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
        }

        LocalDate desde = fechaDesde != null ? LocalDate.parse(fechaDesde, DateTimeFormatter.ISO_DATE) : null;
        LocalDate hasta = fechaHasta != null ? LocalDate.parse(fechaHasta, DateTimeFormatter.ISO_DATE) : null;
        if (desde != null && hasta != null && !desde.isBefore(hasta) && !desde.isEqual(hasta)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "La fecha limite de busqueda no puede ser anterior a la fecha de inicio de busqueda"));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El numero de pagina no puede ser menor a 0"));
        }
        if (size <= 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El tamano de pagina debe ser mayor a 0"));
        }

        Page<EntregaMaterialEntity> entregas = entregaMaterialService.getEntregasByFilters(
            sedeIdUsuario, solicitudId, sedeId, estadoEntrega, desde, hasta, page, size, sort);
        return ResponseEntity.status(HttpStatus.OK).body(entregas);
    }

    @PatchMapping("/entregas/{id}/despachar")
    public ResponseEntity<Object> despacharEntregaMaterial(@PathVariable Long id) {
        try {
            DespacharEntregaMaterialResult resultado = entregaMaterialService.despacharEntregaMaterial(id);
            return switch (resultado) {
                case ENTREGA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("ENTREGA_NOT_FOUND", "No se encontro la entrega"));
                case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("ESTADO_INVALIDO", "La entrega solo puede despacharse si esta en PROGRAMADA"));
                case DETALLE_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La entrega debe contener al menos un item para despacho"));
                case LOTE_NOT_VALID -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                    new ErrorResponseDTO("LOTE_NOT_VALID", "No se puede usar un lote invalido para el despacho"));
                case STOCK_INSUFFICIENT -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                    new ErrorResponseDTO("STOCK_INSUFFICIENT", "No hay stock suficiente para despachar toda la entrega"));
                case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se despacho la entrega con exito");
            };
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("STOCK_CONFLICT", "El stock fue actualizado por otra operacion. Intente nuevamente"));
        }
    }

    @PatchMapping("/entregas/{id}/en-ruta")
    public ResponseEntity<Object> enRutaEntregaMaterial(@PathVariable Long id) {
        EnRutaEntregaMaterialResult resultado = entregaMaterialService.enRutaEntregaMaterial(id);
        return switch (resultado) {
            case ENTREGA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("ENTREGA_NOT_FOUND", "No se encontró la entrega"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("ESTADO_INVALIDO", "La entrega solo puede pasar a EN_RUTA si está en DESPACHADA"));
            case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se actualizó la entrega a EN_RUTA con éxito");
        };
    }

    @PatchMapping("/entregas/{id}/registrar-recepcion")
    public ResponseEntity<Object> registrarRecepcionEntregaMaterial(
        @PathVariable Long id,
        @Valid @RequestBody RegistrarRecepcionEntregaDTO dto,
        BindingResult result
    ) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }

        RegistrarRecepcionEntregaMaterialResult resultado = entregaMaterialService.registrarRecepcionEntregaMaterial(
            id, dto);
        return switch (resultado) {
            case ENTREGA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("ENTREGA_NOT_FOUND", "No se encontró la entrega"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("ESTADO_INVALIDO", "La entrega solo puede registrar recepción si esta en EN_RUTA"));
            case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se registró la recepción de la entrega con éxito");
        };
    }

    private Rol obtenerRol(Authentication authentication) {
        String authority = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElseThrow();

        return Rol.valueOf(authority.replace("ROLE_", ""));
    }

    private Long obtenerSedeIdUsuario(Authentication authentication, Rol rol) {
        if (rol != Rol.SEDE) {
            return null;
        }

        UsuarioEntity usuario = usuarioService.findByUsername(authentication.getName());
        if (usuario == null || usuario.getSede() == null) {
            return null;
        }

        return usuario.getSede().getId();
    }
}
