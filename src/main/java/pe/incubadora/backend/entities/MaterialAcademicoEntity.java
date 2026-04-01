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
@Table(name = "material_academico")
/**
 * Represents a managed academic material available in inventory.
 */
public class MaterialAcademicoEntity {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique stock keeping unit (SKU). */
    @Column(unique = true)
    private String sku;

    /** Material name. */
    private String nombre;
    /** Material category code. */
    private String categoria;
    /** Academic level code. */
    private String nivel;
    /** Unit-of-measure code. */
    private String unidadMedida;
    /** Minimum stock threshold used for control/alerts. */
    private Integer stockMinimo;
    /** Indicates whether lot validity date is mandatory. */
    private boolean controlVigencia;
    /** Indicates whether the material is enabled for operations. */
    private boolean activo;

    /** Creation timestamp populated by Spring Data auditing. */
    @CreatedDate
    private Instant createdAt;
    /** Last update timestamp populated by Spring Data auditing. */
    @LastModifiedDate
    private Instant updatedAt;
}
