package pe.incubadora.backend.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudDistribucionDTO {

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    @NotNull(message = "La sede es obligatoria")
    private Long idSede;

    @NotBlank(message = "El periodo académico es obligatorio")
    private String periodoAcademico;

    @NotBlank(message = "La prioridad es obligatoria")
    private String prioridad;

    private String comentarioSolicitud;

    @Valid
    @NotEmpty(message = "La solicitud debe tener al menos un item")
    private List<SolicitudDistribucionDetalleDTO> items;
}
