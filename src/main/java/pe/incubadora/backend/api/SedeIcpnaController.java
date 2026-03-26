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
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.services.SedeIcpnaService;
import pe.incubadora.backend.services.UsuarioService;
import pe.incubadora.backend.utils.CreateSedeResult;
import pe.incubadora.backend.utils.Rol;

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

    @GetMapping("/sedes")
    public ResponseEntity<Object> getSedes(@RequestParam int page, Authentication authentication) {
        Pageable pageable = Pageable.ofSize(10).withPage(page);

        String authority = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElseThrow();

        Rol rol = Rol.valueOf(authority.replace("ROLE_", ""));
        Long sedeId = null;

        if (rol == Rol.SEDE) {
            UsuarioEntity usuario = usuarioService.findByUsername(authentication.getName());
            if (usuario == null || usuario.getSede() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ErrorResponseDTO("FORBIDDEN", "El usuario no tiene una sede asociada"));
            }
            sedeId = usuario.getSede().getId();
        }
        return ResponseEntity.ok().body(sedeIcpnaService.getSedes(pageable, rol, sedeId));
    }
}
