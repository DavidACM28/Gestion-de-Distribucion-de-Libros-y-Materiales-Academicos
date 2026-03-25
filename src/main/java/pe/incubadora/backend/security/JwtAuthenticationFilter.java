package pe.incubadora.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter executed once for every HTTP request.
 *
 * <p>This filter intercepts incoming requests, extracts the JWT token
 * from the Authorization header, validates it, and sets the authenticated
 * user in the Spring Security context.</p>
 *
 * <p>If the token is valid, the corresponding user details are loaded
 * and an authenticated {@link UsernamePasswordAuthenticationToken}
 * is created and stored in the {@link SecurityContextHolder}.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Service used to load user details from the database.
     */
    @Autowired
    private CustomUsersDetailsService customUsersDetailsService;

    /**
     * Utility class responsible for generating and validating JWT tokens.
     */
    @Autowired
    private JwtGenerador jwtGenerador;

    /**
     * Extracts the JWT token from the Authorization header.
     *
     * <p>The expected format is:</p>
     *
     * <pre>
     * Authorization: Bearer {token}
     * </pre>
     *
     * @param request the HTTP request
     * @return the JWT token or {@code null} if it is missing or invalid
     */
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        return null;
    }

    /**
     * Core filtering logic executed for each request.
     *
     * <p>This method:</p>
     * <ul>
     * <li>Extracts the JWT token</li>
     * <li>Validates the token</li>
     * <li>Loads the corresponding user</li>
     * <li>Sets authentication in the security context</li>
     * </ul>
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain to continue request processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        String token = getToken(request);

        try {

            if (StringUtils.hasText(token) && jwtGenerador.ValidarToken(token)) {

                String username = jwtGenerador.obtenerUsernameDeJwt(token);

                UserDetails userDetails =
                    customUsersDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource()
                        .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                    .setAuthentication(authenticationToken);
            }

        } catch (Exception e) {

            // If token validation fails, clear authentication context
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
