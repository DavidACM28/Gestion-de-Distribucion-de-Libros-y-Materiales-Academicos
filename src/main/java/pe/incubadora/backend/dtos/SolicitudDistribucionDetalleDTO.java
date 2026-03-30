package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudDistribucionDetalleDTO {

    @NotNull(message = "El material es obligatorio")
    private Long idMaterial;

    @NotNull(message = "La cantidad solicitada es obligatoria")
    @Positive(message = "La cantidad solicitada debe ser mayor a 0")
    private Integer cantidadSolicitada;

    private String comentarioItem;
}
