package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "movimiento_inventario")
/**
 * Represents an inventory movement record used for stock traceability.
 */
public class MovimientoInventarioEntity {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Material affected by the movement. */
    @ManyToOne
    @JoinColumn(name = "material_id")
    private MaterialAcademicoEntity materialAcademico;

    /** Lot affected by the movement. */
    @ManyToOne
    @JoinColumn(name = "lote_id")
    private LoteIngresoEntity lote;

    /** Operational date of the movement. */
    private LocalDate fecha;
    /** Movement type label (for example AJUSTE, SALIDA). */
    private String tipoMovimiento;
    /** Absolute quantity moved. */
    private Integer cantidad;
    /** Business reference type (for example ENTREGA, AJUSTE). */
    private String referenciaTipo;
    /** Business reference identifier. */
    private Long referenciaId;
    /** Optional operation comment. */
    private String comentario;

    /** Creation timestamp populated by Spring Data auditing. */
    @CreatedDate
    private Instant createdAt;
}
