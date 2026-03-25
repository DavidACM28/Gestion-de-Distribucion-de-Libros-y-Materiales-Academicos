package pe.incubadora.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.repositories.UsuarioRepository;
import pe.incubadora.backend.utils.Rol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom implementation of {@link UserDetailsService} used by Spring Security
 * to load user-specific data during the authentication process.
 *
 * <p>This service retrieves a {@link UsuarioEntity} from the database using the
 * provided username and converts it into a {@link UserDetails} object required
 * by Spring Security.</p>
 *
 * <p>It also maps the application roles ({@link Rol}) into Spring Security
 * authorities by prefixing them with {@code ROLE_}.</p>
 *
 * <p>This class is part of the authentication mechanism used with
 * Spring Security and JWT.</p>
 */
@Service
public class CustomUsersDetailsService implements UserDetailsService {

    /**
     * Repository used to retrieve user information from the database.
     */
    private final UsuarioRepository usuarioRepository;

    /**
     * Constructor-based dependency injection.
     *
     * @param usuarioRepository repository used to access user data
     */
    public CustomUsersDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Converts application roles into Spring Security authorities.
     *
     * <p>Spring Security expects roles to follow the convention
     * {@code ROLE_<ROLE_NAME>}. This method transforms the {@link Rol}
     * enum values into {@link GrantedAuthority} objects.</p>
     *
     * @param roles list of roles assigned to the user
     * @return a collection of {@link GrantedAuthority} used by Spring Security
     */
    public Collection<GrantedAuthority> mapAuthorities(List<Rol> roles) {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .collect(Collectors.toList());
    }

    /**
     * Loads a user from the database using the provided username.
     *
     * <p>This method is called by Spring Security during the authentication
     * process. If the user is found, it converts the {@link UsuarioEntity}
     * into a {@link UserDetails} object containing username, password,
     * and authorities.</p>
     *
     * @param username the username identifying the user whose data is required
     * @return a fully populated {@link UserDetails} object
     * @throws UsernameNotFoundException if the user cannot be found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        List<Rol> roles = new ArrayList<>();
        roles.add(usuario.getRol());

        return new User(
            usuario.getUsername(),
            usuario.getPassword(),
            mapAuthorities(roles)
        );
    }
}
