package pe.incubadora.backend.security;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Security-related constants used in JWT authentication.
 *
 * <p>This class defines:</p>
 * <ul>
 * <li>JWT token expiration time</li>
 * <li>Secret key used for signing tokens</li>
 * </ul>
 *
 * <p>The secret key is used to generate a {@link SecretKey}
 * for HMAC-based JWT signing.</p>
 */
public class SecurityConstants {

    /**
     * Token expiration time in milliseconds.
     * (20 hours)
     */
    public static final long JWT_EXPIRATION_TOKEN = 72000000;

    /**
     * Secret string used to generate the JWT signing key.
     */
    private static final String SECRET_STRING =
        "clave_secreta_lazarus_technologies_incubadora_de_talento_laboratorio_05_david_alonso_castro_moreno";
    /**
     * Secret key used to sign and validate JWT tokens.
     */
    public static final SecretKey JWT_FIRMA =
        Keys.hmacShaKeyFor(
            SECRET_STRING.getBytes(StandardCharsets.UTF_8)
        );
}
