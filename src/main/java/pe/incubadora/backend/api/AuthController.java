package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.UsuarioDTO;
import pe.incubadora.backend.dtos.auth.LoginAdministrativoResponseDTO;
import pe.incubadora.backend.dtos.auth.LoginDTO;
import pe.incubadora.backend.dtos.auth.LoginSedeResponseDTO;
import pe.incubadora.backend.dtos.auth.RegisterDTO;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.security.JwtGenerador;
import pe.incubadora.backend.services.UsuarioService;
import pe.incubadora.backend.utils.RegisterUsuarioResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
/**
 * Exposes authentication endpoints for user registration and login.
 */
public class AuthController {

    @Autowired
    UsuarioService usuarioService;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtGenerador jwtGenerador;

    /**
     * Registers a new user account using the requested role and optional branch assignment.
     *
     * @param registerDTO payload with username, password, role and optional branch id
     * @param result validation result produced by Bean Validation
     * @return success response or a validation/business error response
     */
    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterDTO registerDTO, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            RegisterUsuarioResult resultado = usuarioService.register(registerDTO);
            return switch (resultado) {
                case ROL_NOT_FOUND -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "No se encontró el rol"));
                case SIN_SEDE -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El rol sede debe tener una Id de sede"));
                case SEDE_NOT_FOUND -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "No se encontró la sede"));
                case SEDE_NOT_REQUIRED -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Solo el rol sede puede contener una Id de sede"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se creó el usuario");
            };
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("USERNAME_CONFLICT", "El nombre de usuario ya existe"));
        }
    }

    /**
     * Authenticates a user and returns a JWT plus user data when the role belongs to a branch user.
     *
     * @param loginDTO credentials payload
     * @return token response or unauthorized error when the user cannot be resolved
     */
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginDTO loginDTO) {
        Authentication authentication =
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtGenerador.generarToken(authentication);
        UsuarioEntity usuarioEntity = usuarioService.findByUsername(loginDTO.getUsername());
        if (usuarioEntity == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El usuario no existe"));
        }
        if (usuarioEntity.getSede() == null) {
            return ResponseEntity.status(HttpStatus.OK).body(
                new LoginAdministrativoResponseDTO(token, "Bearer "));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new LoginSedeResponseDTO(token, "Bearer ",
            new UsuarioDTO(
                usuarioEntity.getId(),
                usuarioEntity.getSede(),
                usuarioEntity.getUsername(),
                usuarioEntity.getRol(),
                usuarioEntity.isActivo()
            )));
    }
}
