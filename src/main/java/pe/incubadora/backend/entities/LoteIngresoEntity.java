package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "lote_ingreso")
/**
 * Represents an inbound stock lot for an academic material.
 */
public class LoteIngresoEntity {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique lot code. */
    @Column(unique = true)
    private String codigoLote;

    /** Material associated with this lot. */
    @ManyToOne
    @JoinColumn(name = "material_id")
    private MaterialAcademicoEntity materialAcademico;

    /** Date when the lot entered inventory. */
    private LocalDate fechaIngreso;
    /** Edition label or reference. */
    private String edicion;
    /** Optional expiration/validity date. */
    private LocalDate fechaVigencia;
    /** Quantity originally received. */
    private Integer cantidadIngresada;
    /** Quantity currently available for dispatch/adjustments. */
    private Integer cantidadDisponible;
    /** Supplier name. */
    private String proveedor;
    /** Lot status (for example DISPONIBLE, AGOTADO, FUERA_VIGENCIA). */
    private String estado;

    /** Creation timestamp populated by Spring Data auditing. */
    @CreatedDate
    private Instant createdAt;
    /** Last update timestamp populated by Spring Data auditing. */
    @LastModifiedDate
    private Instant modifiedAt;
    /** Optimistic locking version used to prevent concurrent stock overwrites. */
    @Version
    private Long version;

}
