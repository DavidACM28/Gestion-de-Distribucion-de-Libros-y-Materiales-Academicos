package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SedeIcpnaDTO {

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, message = "El nombre debe contener 3 caracteres como mínimo")
    private String nombre;

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(min = 5, message = "La dirección debe tener 5 caracteres como mínimo")
    private String direccion;

    @NotBlank(message = "El responsable es obligatorio")
    private String responsableLogistica;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    private String contacto;
}
