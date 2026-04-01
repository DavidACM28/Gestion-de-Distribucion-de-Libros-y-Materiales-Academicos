package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntregaMaterialDTO {
    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    @NotNull(message = "La solicitud es obligatoria")
    private Long solicitudId;

    @NotBlank(message = "La fecha programada es obligatoria")
    private String fechaProgramada;

    @NotBlank(message = "El responsable de almacén es obligatorio")
    private String responsableAlmacen;

    private String comentario;
}
