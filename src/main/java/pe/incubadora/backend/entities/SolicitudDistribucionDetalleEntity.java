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
public class SolicitudDistribucionDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "solicitud_id")
    @JsonBackReference
    private SolicitudDistribucionEntity solicitud;

    @ManyToOne
    @JoinColumn(name = "material_id")
    private MaterialAcademicoEntity material;

    private Integer cantidadSolicitada;
    private Integer cantidadAprobada;
    private String comentarioItem;
}
