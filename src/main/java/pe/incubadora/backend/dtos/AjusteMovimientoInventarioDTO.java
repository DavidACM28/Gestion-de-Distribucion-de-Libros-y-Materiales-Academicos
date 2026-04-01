package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AjusteMovimientoInventarioDTO {
    @NotNull(message = "El material es obligatorio")
    private Long idMaterial;

    @NotNull(message = "El lote es obligatorio")
    private Long idLote;

    @NotBlank(message = "El tipo de ajuste es obligatorio")
    private String tipoAjuste;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    private String comentario;
}
