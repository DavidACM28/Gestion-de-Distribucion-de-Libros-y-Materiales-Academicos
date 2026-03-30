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
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.SolicitudDistribucionDTO;
import pe.incubadora.backend.entities.SolicitudDistribucionEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.services.SolicitudDistribucionService;
import pe.incubadora.backend.services.UsuarioService;
import pe.incubadora.backend.utils.Rol;
import pe.incubadora.backend.utils.solicitudDistribucion.CreateSolicitudDistribucionResult;
import pe.incubadora.backend.utils.solicitudDistribucion.UpdateSolicitudDistribucionResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SolicitudDistribucionController {
    @Autowired
    private SolicitudDistribucionService solicitudDistribucionService;
    @Autowired
    private UsuarioService usuarioService;

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatchException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegúrese de que los filtros se envíen con el formato correcto"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegúrese de que los filtros se envíen con el formato correcto"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingServletRequestParameterException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Los parámetros: size, page, y sort, son obligatorios"));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Object> handleDateTimeParseException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Fecha invalida. Use formato yyyy-MM-dd"));
    }

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
