package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialAcademicoDTO {

    @NotBlank(message = "El sku es obligatorio")
    private String sku;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;

    @NotBlank(message = "El nivel es obligatorio")
    private String nivel;

    @NotBlank(message = "La unidad de medida es obligatoria")
    private String unidadMedida;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0)
    private Integer stockMinimo;

    @NotNull(message = "El control de vigencia es obligatorio")
    private Boolean controlVigencia;

    @NotNull(message = "El activo es obligatorio")
    private Boolean activo;
}
