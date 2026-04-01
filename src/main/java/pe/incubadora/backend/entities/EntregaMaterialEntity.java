package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "entrega_material")
/**
 * Represents a material delivery generated from an approved distribution request.
 */
public class EntregaMaterialEntity {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique delivery code. */
    @Column(unique = true)
    private String codigo;

    /** Source distribution request. */
    @ManyToOne
    @JoinColumn(name = "solicitud_id")
    private SolicitudDistribucionEntity solicitud;

    /** Planned date for delivery execution. */
    private LocalDate fechaProgramada;
    /** Actual dispatch date. */
    private LocalDate fechaDespacho;
    /** Actual reception date. */
    private LocalDate fechaEntrega;
    /** Delivery state in the logistics lifecycle. */
    private String estadoEntrega;
    /** Warehouse operator responsible for dispatch. */
    private String responsableAlmacen;
    /** Branch operator responsible for reception. */
    private String responsableRecepcion;
    /** Optional delivery-level comment. */
    private String comentario;

    /** Creation timestamp populated by Spring Data auditing. */
    @CreatedDate
    private LocalDateTime createdAt;
    /** Last update timestamp populated by Spring Data auditing. */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
