package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "entrega_material_detalle")
/**
 * Represents one delivered quantity line, optionally tied to a specific lot after dispatch.
 */
public class EntregaMaterialDetalleEntity {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent delivery. */
    @ManyToOne
    @JoinColumn(name = "entrega_id")
    private EntregaMaterialEntity entrega;

    /** Material delivered in this line. */
    @ManyToOne
    @JoinColumn(name = "material_id")
    private MaterialAcademicoEntity material;

    /** Source lot used for dispatch allocation (nullable before dispatch split). */
    @ManyToOne
    @JoinColumn(name = "lote_id")
    private LoteIngresoEntity lote;

    /** Quantity delivered for this line. */
    private Integer cantidad;
}
