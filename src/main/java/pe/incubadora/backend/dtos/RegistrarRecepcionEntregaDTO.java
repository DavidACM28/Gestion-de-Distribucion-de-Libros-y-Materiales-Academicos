package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrarRecepcionEntregaDTO {
    @NotBlank(message = "El responsable de la recepción es obligatorio")
    private String responsableRecepcion;

    @NotNull(message = "conIncidencia es obligatorio")
    private Boolean conIncidencia;
}
