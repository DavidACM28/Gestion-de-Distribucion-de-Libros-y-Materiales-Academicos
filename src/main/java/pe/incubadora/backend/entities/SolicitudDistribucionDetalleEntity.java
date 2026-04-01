package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "solicitud_distribucion_detalle")
/**
 * Represents one requested material line within a distribution request.
 */
public class SolicitudDistribucionDetalleEntity {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent distribution request. */
    @ManyToOne
    @JoinColumn(name = "solicitud_id")
    @JsonBackReference
    private SolicitudDistribucionEntity solicitud;

    /** Requested material. */
    @ManyToOne
    @JoinColumn(name = "material_id")
    private MaterialAcademicoEntity material;

    /** Quantity requested by the branch. */
    private Integer cantidadSolicitada;
    /** Quantity approved by review process. */
    private Integer cantidadAprobada;
    /** Optional item-specific comment. */
    private String comentarioItem;
}
