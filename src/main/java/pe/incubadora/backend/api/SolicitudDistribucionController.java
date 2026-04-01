package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.incubadora.backend.dtos.AprobarSolicitudDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.ObservarSolicitudDTO;
import pe.incubadora.backend.dtos.SolicitudDistribucionDTO;
import pe.incubadora.backend.entities.SolicitudDistribucionEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.services.SolicitudDistribucionService;
import pe.incubadora.backend.services.UsuarioService;
import pe.incubadora.backend.utils.Rol;
import pe.incubadora.backend.utils.solicitudDistribucion.AprobarSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.CancelarSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.CreateSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.EnviarSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.ObservarSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.UpdateSolicitudDistribucionResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
/**
 * Manages distribution request endpoints, including lifecycle transitions and filtered queries.
 */
public class SolicitudDistribucionController {
    @Autowired
    private SolicitudDistribucionService solicitudDistribucionService;
    @Autowired
    private UsuarioService usuarioService;

    /**
     * Handles invalid filter value types sent in query parameters.
     *
     * @return validation error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatchException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegúrese de que los filtros se envíen con el formato correcto"));
    }

    /**
     * Handles invalid enum or argument values used by filters.
     *
     * @return validation error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegúrese de que los filtros se envíen con el formato correcto"));
    }

    /**
     * Handles missing mandatory pagination and sorting parameters.
     *
     * @return validation error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingServletRequestParameterException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Los parámetros: size, page, y sort, son obligatorios"));
    }

    /**
     * Handles invalid date formats for date-based filters.
     *
     * @return validation error response
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Object> handleDateTimeParseException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Fecha invalida. Use formato yyyy-MM-dd"));
    }

    /**
     * Creates a distribution request and validates role-based ownership for branch users.
     *
     * @param dto request payload
     * @param result Bean Validation result
     * @param authentication authenticated principal
     * @return created response or validation/business errors
     */
    @PostMapping("/solicitudes")
    public ResponseEntity<Object> createSolicitudDistribucion(
        @Valid @RequestBody SolicitudDistribucionDTO dto, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            Rol rol = obtenerRol(authentication);
            Long sedeIdUsuario = obtenerSedeIdUsuario(authentication, rol);

            if (rol == Rol.SEDE && sedeIdUsuario == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
            }

            if (rol == Rol.SEDE && !sedeIdUsuario.equals(dto.getIdSede())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ErrorResponseDTO("FORBIDDEN", "No puede crear solicitudes para otra sede"));
            }

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

    /**
     * Updates a draft request and enforces branch ownership for users with role {@code SEDE}.
     *
     * @param dto patch-like payload (non-null fields are applied)
     * @param id request identifier
     * @param authentication authenticated principal
     * @return updated response or validation/business errors
     */
    @PutMapping("/solicitudes/{id}")
    public ResponseEntity<Object> updateSolicitudDistribucion(
        @RequestBody SolicitudDistribucionDTO dto, @PathVariable Long id, Authentication authentication) {
        try {
            SolicitudDistribucionEntity solicitud = solicitudDistribucionService.getSolicitudDistribucionById(id).orElse(null);
            if (solicitud == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
            }

            Rol rol = obtenerRol(authentication);
            Long sedeIdUsuario = obtenerSedeIdUsuario(authentication, rol);

            if (rol == Rol.SEDE && sedeIdUsuario == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
            }

            if (rol == Rol.SEDE && !sedeIdUsuario.equals(solicitud.getSedeIcpna().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ErrorResponseDTO("FORBIDDEN", "No puede actualizar solicitudes de otra sede"));
            }

            UpdateSolicitudDistribucionResult resultado = solicitudDistribucionService.updateSolicitudDistribucion(dto, id);
            return switch (resultado) {
                case SOLICITUD_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
                case CODIGO_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El código no puede ser vacío"));
                case SEDE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("SEDE_NOT_FOUND", "No se encontró la sede"));
                case SEDE_NOT_ACTIVE -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La sede proporcionada no se encuentra activa"));
                case MATERIAL_REQUIRED -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El material es obligatorio"));
                case MATERIAL_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("MATERIAL_NOT_FOUND", "No se encontró el material"));
                case SOLICITUD_DUPLICADA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Ya existe una solicitud activa para este periodo académico en esta sede"));
                case PERIODO_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Periodo académico inválido, use formato: yyyy-MM"));
                case PRIORIDAD_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Prioridad inválida, use: NORMAL | ALTA | URGENTE"));
                case ITEMS_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La solicitud debe tener al menos un item"));
                case MATERIAL_DUPLICATE -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El material no se puede duplicar"));
                case CANTIDAD_SOLICITADA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Cantidad solicitada inválida, debe ser mayor a 0"));
                case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se actualizo la solicitud con éxito");
            };
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CODIGO_CONFLICT", "Ya existe una solicitud con este código"));
        }
    }

    /**
     * Sends a request for review.
     *
     * @param id request identifier
     * @param authentication authenticated principal
     * @return updated response or validation/business errors
     */
    @PatchMapping("/solicitudes/{id}/enviar")
    public ResponseEntity<Object> enviarSolicitudDistribucion(@PathVariable Long id, Authentication authentication) {
        SolicitudDistribucionEntity solicitud = solicitudDistribucionService.getSolicitudDistribucionById(id).orElse(null);
        if (solicitud == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
        }

        Rol rol = obtenerRol(authentication);
        Long sedeIdUsuario = obtenerSedeIdUsuario(authentication, rol);

        if (rol == Rol.SEDE && sedeIdUsuario == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
        }

        if (rol == Rol.SEDE && !sedeIdUsuario.equals(solicitud.getSedeIcpna().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("FORBIDDEN", "No puede enviar solicitudes de otra sede"));
        }

        EnviarSolicitudDistribucionResult resultado = solicitudDistribucionService.enviarSolicitudDistribucion(id);
        return switch (resultado) {
            case SOLICITUD_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("ESTADO_INVALIDO", "La solicitud solo puede enviarse si está en BORRADOR u OBSERVADA"));
            case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se envió la solicitud con éxito");
        };
    }

    /**
     * Marks a request as observed and stores review comments.
     *
     * @param id request identifier
     * @param dto payload with review comment
     * @return updated response or validation/business errors
     */
    @PatchMapping("/solicitudes/{id}/observar")
    public ResponseEntity<Object> observarSolicitudDistribucion(
        @PathVariable Long id, @RequestBody ObservarSolicitudDTO dto) {
        ObservarSolicitudDistribucionResult resultado =
            solicitudDistribucionService.observarSolicitudDistribucion(id, dto.getComentarioRevision());

        return switch (resultado) {
            case SOLICITUD_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
            case COMENTARIO_REVISION_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El comentario de revisión no puede estar vacío"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("ESTADO_INVALIDO", "La solicitud solo puede pasar a OBSERVADA si está en ENVIADA"));
            case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se observó la solicitud con éxito");
        };
    }

    /**
     * Cancels a request, enforcing branch ownership for {@code SEDE} users.
     *
     * @param id request identifier
     * @param authentication authenticated principal
     * @return updated response or validation/business errors
     */
    @PatchMapping("/solicitudes/{id}/cancelar")
    public ResponseEntity<Object> cancelarSolicitudDistribucion(@PathVariable Long id, Authentication authentication) {
        SolicitudDistribucionEntity solicitud = solicitudDistribucionService.getSolicitudDistribucionById(id).orElse(null);
        if (solicitud == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
        }

        Rol rol = obtenerRol(authentication);
        Long sedeIdUsuario = obtenerSedeIdUsuario(authentication, rol);

        if (rol == Rol.SEDE && sedeIdUsuario == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
        }

        if (rol == Rol.SEDE && !sedeIdUsuario.equals(solicitud.getSedeIcpna().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("FORBIDDEN", "No puede cancelar solicitudes de otra sede"));
        }

        CancelarSolicitudDistribucionResult resultado = solicitudDistribucionService.cancelarSolicitudDistribucion(id);
        return switch (resultado) {
            case SOLICITUD_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("ESTADO_INVALIDO", "La solicitud no puede cancelarse si está en DESPACHADA, ENTREGADA o PARCIAL"));
            case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se canceló la solicitud con éxito");
        };
    }

    /**
     * Approves a request by assigning approved quantities per detail.
     *
     * @param id request identifier
     * @param dto approval payload
     * @return updated response or validation/business errors
     */
    @PatchMapping("/solicitudes/{id}/aprobar")
    public ResponseEntity<Object> aprobarSolicitudDistribucion(
        @PathVariable Long id, @RequestBody AprobarSolicitudDTO dto) {
        AprobarSolicitudDistribucionResult resultado =
            solicitudDistribucionService.aprobarSolicitudDistribucion(id, dto);

        return switch (resultado) {
            case SOLICITUD_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
            case ITEMS_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "La aprobación debe incluir al menos un item"));
            case DETALLE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("DETALLE_NOT_FOUND", "No se encontró el detalle de la solicitud"));
            case DETALLE_NOT_BELONG_TO_SOLICITUD -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El detalle no pertenece a la solicitud"));
            case CANTIDAD_APROBADA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "La cantidad aprobada debe ser mayor o igual a 0, no superar la cantidad solicitada y al menos un item debe aprobarse"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("ESTADO_INVALIDO", "La solicitud solo puede aprobarse si está en ENVIADA u OBSERVADA"));
            case STOCK_INSUFFICIENT -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                new ErrorResponseDTO("STOCK_INSUFFICIENT", "La cantidad aprobada supera el stock disponible del material"));
            case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se aprobó la solicitud con éxito");
        };
    }

    /**
     * Returns one request by id with visibility rules for {@code SEDE} users.
     *
     * @param id request identifier
     * @param authentication authenticated principal
     * @return request response or not found/forbidden errors
     */
    @GetMapping("/solicitudes/{id}")
    public ResponseEntity<Object> getSolicitudById(@PathVariable Long id, Authentication authentication) {
        SolicitudDistribucionEntity solicitud = solicitudDistribucionService.getSolicitudDistribucionById(id).orElse(null);
        if (solicitud == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
        }
        Rol rol = obtenerRol(authentication);
        Long sedeIdUsuario = obtenerSedeIdUsuario(authentication, rol);

        if (rol == Rol.SEDE && sedeIdUsuario == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
        }

        if (rol == Rol.SEDE && !sedeIdUsuario.equals(solicitud.getSedeIcpna().getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("SOLICITUD_NOT_FOUND", "No se encontró la solicitud"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(solicitud);
    }

    /**
     * Returns paginated requests with optional filters and role-based scope restrictions.
     *
     * @param authentication authenticated principal
     * @param sedeId optional branch filter for admin/warehouse users
     * @param periodoAcademico optional period filter in {@code yyyy-MM}
     * @param estado optional status filter
     * @param prioridad optional priority filter
     * @param fechaDesde optional start date filter in {@code yyyy-MM-dd}
     * @param fechaHasta optional end date filter in {@code yyyy-MM-dd}
     * @param page page index
     * @param size page size
     * @param sort sort direction token
     * @return paginated request list
     */
    @GetMapping("/solicitudes")
    public ResponseEntity<Object> getSolicitudes(
        Authentication authentication, @RequestParam(required = false) Long sedeId,
        @RequestParam(required = false) String periodoAcademico, @RequestParam(required = false) String estado,
        @RequestParam(required = false) String prioridad, @RequestParam(required = false) String fechaDesde,
        @RequestParam(required = false) String fechaHasta, @RequestParam int page, @RequestParam int size,
        @RequestParam String sort) {

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
                new ErrorResponseDTO("VALIDATION_ERROR", "La fecha límite de búsqueda no puede ser anterior a la fecha de inicio de búsqueda"));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El número de página no puede ser menor a 0"));
        }
        if (size <= 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El tamaño de página debe ser mayor a 0"));
        }

        Page<SolicitudDistribucionEntity> solicitudes =
            solicitudDistribucionService.getSolicitudesDistribucionByFilters(sedeIdUsuario, sedeId, periodoAcademico,
                estado, prioridad, desde, hasta, page, size, sort);
        return ResponseEntity.status(HttpStatus.OK).body(solicitudes);
    }

    /**
     * Resolves domain role from Spring Security authorities.
     *
     * @param authentication authenticated principal
     * @return domain role
     */
    private Rol obtenerRol(Authentication authentication) {
        String authority = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElseThrow();

        return Rol.valueOf(authority.replace("ROLE_", ""));
    }

    /**
     * Resolves the authenticated user's branch id when role is {@code SEDE}.
     *
     * @param authentication authenticated principal
     * @param rol requester role
     * @return branch id for {@code SEDE}, otherwise {@code null}
     */
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
