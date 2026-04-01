package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.incubadora.backend.utils.Rol;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "usuario")
/**
 * Represents an application user and its role-based access scope.
 */
public class UsuarioEntity {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Branch association used for users with role {@code SEDE}. */
    @ManyToOne
    @JoinColumn(name = "sede_id")
    SedeIcpnaEntity sede;

    /** Unique username used for login. */
    @Column(unique = true)
    private String username;

    /** Encoded password hash. */
    private String password;
    /** Functional role used by authorization rules. */
    private Rol rol;
    /** Flag indicating whether the account is active. */
    private boolean activo;
}
