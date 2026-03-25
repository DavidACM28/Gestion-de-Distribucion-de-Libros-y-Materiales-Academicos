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
public class MovimientoInventarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "material_id")
    private MaterialAcademicoEntity materialAcademico;

    @ManyToOne
    @JoinColumn(name = "lote_id")
    private LoteIngresoEntity lote;

    private LocalDate fecha;
    private String tipoMovimiento;
    private Integer cantidad;
    private String referenciaTipo;
    private Long referenciaId;
    private String comentario;

    @CreatedDate
    private Instant createdAt;
}
