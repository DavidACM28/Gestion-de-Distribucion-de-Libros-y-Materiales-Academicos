package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sede_icpna")
/**
 * Represents an ICPNA branch used by requests and branch-scoped users.
 */
public class SedeIcpnaEntity {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique branch code. */
    @Column(unique = true)
    private String codigo;

    /** Branch display name. */
    private String nombre;
    /** City where the branch operates. */
    private String ciudad;
    /** Physical branch address. */
    private String direccion;
    /** Logistics contact person. */
    private String responsableLogistica;
    /** Contact details (phone/email) for logistics coordination. */
    private String contacto;
    /** Branch status value (for example ACTIVA, INACTIVA or SUSPENDIDA). */
    private String estado;
    /** Creation timestamp populated by Spring Data auditing. */
    @CreatedDate
    private Instant createdAt;
    /** Last update timestamp populated by Spring Data auditing. */
    @LastModifiedDate
    private Instant updatedAt;
}
