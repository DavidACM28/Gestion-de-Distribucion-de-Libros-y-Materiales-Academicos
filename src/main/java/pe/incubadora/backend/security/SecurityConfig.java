package pe.incubadora.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Main Spring Security configuration for the application.
 *
 * <p>This class defines:</p>
 * <ul>
 * <li>Authentication manager</li>
 * <li>Password encryption strategy</li>
 * <li>JWT authentication filter</li>
 * <li>Access control rules for API endpoints</li>
 * </ul>
 *
 * <p>The application uses stateless authentication with JWT tokens,
 * meaning no HTTP sessions are stored on the server.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Entry point used when authentication fails.
     */
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * JWT filter responsible for validating tokens on every request.
     */
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Provides the authentication manager used by Spring Security.
     *
     * @param authenticationConfiguration configuration provided by Spring
     * @return the authentication manager instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration)
        throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Password encoder used for hashing user passwords.
     *
     * <p>BCrypt is recommended because it includes salting
     * and adaptive hashing.</p>
     *
     * @return password encoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the Spring Security filter chain and access rules.
     *
     * <p>This configuration:</p>
     * <ul>
     * <li>Disables CSRF (for REST APIs)</li>
     * <li>Enforces stateless JWT authentication</li>
     * <li>Defines endpoint authorization rules by role</li>
     * <li>Adds the JWT filter before Spring's authentication filter</li>
     * </ul>
     *
     * @param http HTTP security configuration
     * @return configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")
                    )
            )
            .sessionManagement(sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    .requestMatchers(HttpMethod.POST,
                        "/api/v1/sedes"
                    ).hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST,
                        "/api/v1/materiales",
                        "/api/v1/lotes"
                    ).hasAnyRole("ADMIN", "ALMACEN")
                    .requestMatchers(HttpMethod.POST,
                        "/api/v1/solicitudes"
                    ).hasAnyRole("ADMIN", "SEDE")
                    .requestMatchers(HttpMethod.PUT,
                        "/api/v1/sedes/{id}"
                    ).hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT,
                        "/api/v1/materiales/{id}",
                        "/api/v1/lotes/{id}"
                    ).hasAnyRole("ADMIN", "ALMACEN")
                    .requestMatchers(HttpMethod.PUT,
                        "/api/v1/solicitudes/{id}"
                    ).hasAnyRole("ADMIN", "SEDE")
                    .requestMatchers(HttpMethod.PATCH,
                        "/api/v1/lotes/{id}/fuera-vigencia"
                    ).hasAnyRole("ADMIN", "ALMACEN")
                    .requestMatchers(HttpMethod.PATCH,
                        "/api/v1/solicitudes/{id}/enviar"
                    ).hasAnyRole("ADMIN", "SEDE")
                    .requestMatchers(HttpMethod.PATCH,
                        "/api/v1/solicitudes/{id}/observar"
                    ).hasAnyRole("ADMIN", "ALMACEN")
                    .requestMatchers(HttpMethod.PATCH,
                        "/api/v1/solicitudes/{id}/cancelar"
                    ).hasAnyRole("ADMIN", "SEDE")
                    .requestMatchers(
                        "/api/v1/auth/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()

                    .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
