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
@Table(name = "solicitud_distribucion")
public class SolicitudDistribucionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String codigo;

    @ManyToOne
    @JoinColumn(name = "sede_id")
    private SedeIcpnaEntity sedeIcpna;

    private String periodoAcademico;
    private LocalDate fechaSolicitud;
    private String prioridad;
    private String estado;
    private LocalDate fechaLimiteRevision;
    private String comentarioSolicitud;
    private String comentarioRevision;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant lastModified;
}
