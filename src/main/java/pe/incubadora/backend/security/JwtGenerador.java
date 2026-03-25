package pe.incubadora.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Utility component responsible for JWT token operations.
 *
 * <p>This class handles:</p>
 * <ul>
 * <li>JWT token generation after successful authentication</li>
 * <li>Extraction of user information from the token</li>
 * <li>Validation of JWT tokens</li>
 * </ul>
 *
 * <p>The token is signed using the secret key defined in
 * {@link SecurityConstants}.</p>
 */
@Component
public class JwtGenerador {

    /**
     * Generates a JWT token after successful authentication.
     *
     * <p>The token contains:</p>
     * <ul>
     * <li>Username as subject</li>
     * <li>Issue timestamp</li>
     * <li>Expiration timestamp</li>
     * </ul>
     *
     * @param authentication authentication object containing user credentials
     * @return a signed JWT token
     */
    public String generarToken(Authentication authentication) {

        String username = authentication.getName();

        Date tiempoActual = new Date();

        Date expiracionToken =
            new Date(tiempoActual.getTime() +
                SecurityConstants.JWT_EXPIRATION_TOKEN);

        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(expiracionToken)
            .signWith(SignatureAlgorithm.HS512,
                SecurityConstants.JWT_FIRMA)
            .compact();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT token
     * @return the username stored in the token
     */
    public String obtenerUsernameDeJwt(String token) {

        JwtParser parser = Jwts.parserBuilder()
            .setSigningKey(SecurityConstants.JWT_FIRMA)
            .build();

        Claims claims = parser.parseClaimsJws(token).getBody();

        return claims.getSubject();
    }

    /**
     * Validates a JWT token.
     *
     * <p>This method verifies:</p>
     * <ul>
     * <li>Token signature</li>
     * <li>Token expiration</li>
     * <li>Token integrity</li>
     * </ul>
     *
     * @param token the JWT token
     * @return {@code true} if the token is valid, otherwise {@code false}
     */
    public Boolean ValidarToken(String token) {

        try {

            JwtParser parser = Jwts.parserBuilder()
                .setSigningKey(SecurityConstants.JWT_FIRMA)
                .build();

            parser.parseClaimsJws(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}
