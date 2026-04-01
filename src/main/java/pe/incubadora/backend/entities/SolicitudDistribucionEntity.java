package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "solicitud_distribucion")
/**
 * Represents a distribution request made by a branch for academic materials.
 */
public class SolicitudDistribucionEntity {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique request code. */
    @Column(unique = true)
    private String codigo;

    /** Branch requesting the distribution. */
    @ManyToOne
    @JoinColumn(name = "sede_id")
    private SedeIcpnaEntity sedeIcpna;

    /** Detail lines requested for this distribution request. */
    @JsonManagedReference
    @OneToMany(mappedBy = "solicitud")
    private List<SolicitudDistribucionDetalleEntity> detalles;

    /** Academic period in {@code yyyy-MM}. */
    private String periodoAcademico;
    /** Date when the request was created. */
    private LocalDate fechaSolicitud;
    /** Priority code (NORMAL, ALTA, URGENTE). */
    private String prioridad;
    /** Current request status. */
    private String estado;
    /** Deadline for review based on priority. */
    private LocalDate fechaLimiteRevision;
    /** Optional comment from branch side. */
    private String comentarioSolicitud;
    /** Optional review comment from administrative side. */
    private String comentarioRevision;

    /** Creation timestamp populated by Spring Data auditing. */
    @CreatedDate
    private Instant createdAt;
    /** Last update timestamp populated by Spring Data auditing. */
    @LastModifiedDate
    private Instant lastModified;
}
