package pe.incubadora.backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Handles unauthorized access attempts in the application.
 *
 * <p>This class is invoked by Spring Security when a user tries to access
 * a protected resource without proper authentication (for example,
 * when a JWT token is missing or invalid).</p>
 *
 * <p>It returns an HTTP 401 (Unauthorized) response to the client.</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Called when an unauthenticated user tries to access a secured REST endpoint.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param authException exception thrown during authentication
     * @throws IOException if an input or output error occurs
     * @throws ServletException if the request cannot be handled
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
        throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        ErrorResponseDTO error = new ErrorResponseDTO(
            "UNAUTHORIZED",
            "Debe iniciar sesión para acceder a este recurso"
        );
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
