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
public class EntregaMaterialDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "entrega_id")
    private EntregaMaterialEntity entrega;

    @ManyToOne
    @JoinColumn(name = "material_id")
    private MaterialAcademicoEntity material;

    @ManyToOne
    @JoinColumn(name = "lote_id")
    private LoteIngresoEntity lote;

    private Integer cantidad;
}
