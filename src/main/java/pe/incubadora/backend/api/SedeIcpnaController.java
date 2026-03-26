package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.SedeIcpnaDTO;
import pe.incubadora.backend.entities.SedeIcpnaEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.services.SedeIcpnaService;
import pe.incubadora.backend.services.UsuarioService;
import pe.incubadora.backend.utils.sedeIcpna.CreateSedeResult;
import pe.incubadora.backend.utils.Rol;
import pe.incubadora.backend.utils.sedeIcpna.UpdateSedeResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SedeIcpnaController {
    @Autowired
    private SedeIcpnaService sedeIcpnaService;
    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/sedes")
    public ResponseEntity<Object> createSede(@Valid @RequestBody SedeIcpnaDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            CreateSedeResult resultado = sedeIcpnaService.createSede(dto);
            return switch (resultado) {
                case ESTADO_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El estado no es válido"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se creó la sede con éxito");
            };
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CODIGO_CONFLICT", "Ya existe una sede con este código"));
        }
    }

    @PutMapping("/sedes/{id}")
    public ResponseEntity<Object> updateSede(@RequestBody SedeIcpnaDTO dto, @PathVariable Long id) {
        try {
            UpdateSedeResult resultado = sedeIcpnaService.updateSede(dto, id);
            return switch (resultado) {
                case SEDE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("SEDE_NOT_FOUND", "No se encontró la sede"));
                case CODIGO_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El código no puede ser vacío"));
                case NOMBRE_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El nombre debe contener 3 caracteres como mínimo"));
                case CIUDAD_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La ciudad no puede ser vacía"));
                case DIRECCION_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La dirección debe contener 5 caracteres como mínimo"));
                case RESPONSABLE_EMPTY -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El responsable no puede ser vacío"));
                case ESTADO_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Estado inválido, use: ACTIVA | INACTIVA | SUSPENDIDA"));
                case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("Se actualizó con éxito");
            };
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CODIGO_CONFLICT", "Ya existe una sede con este código"));
        }
    }

    @GetMapping("/sedes")
    public ResponseEntity<Object> getSedes(@RequestParam int page, Authentication authentication) {
        Pageable pageable = Pageable.ofSize(10).withPage(page);
        Rol rol = obtenerRol(authentication);
        Long sedeId = obtenerSedeIdUsuario(authentication, rol);

        if (rol == Rol.SEDE && sedeId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
        }

        return ResponseEntity.ok().body(sedeIcpnaService.getSedes(pageable, rol, sedeId));
    }

    @GetMapping("/sedes/{idSede}")
    public ResponseEntity<Object> getSedeById(@PathVariable Long idSede, Authentication authentication) {
        Rol rol = obtenerRol(authentication);
        Long sedeIdUsuario = obtenerSedeIdUsuario(authentication, rol);

        if (rol == Rol.SEDE && sedeIdUsuario == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
        }

        SedeIcpnaEntity sede = sedeIcpnaService.getSedeById(rol, idSede, sedeIdUsuario).orElse(null);
        if (sede == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("SEDE_NOT_FOUND", "No se encontró la sede"));
        }

        return ResponseEntity.ok().body(sede);
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
