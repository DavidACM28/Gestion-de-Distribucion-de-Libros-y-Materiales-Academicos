package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoteIngresoDTO {

    @NotBlank(message = "El código de lote es obligatorio")
    private String codigoLote;

    @NotNull(message = "La id del material es obligatoria")
    private Long idMaterial;

    @NotBlank(message = "La fecha es obligatoria")
    private String fechaIngreso;

    @NotNull(message = "La cantidad ingresada es obligatoria")
    @Min(value = 1, message = "La cantidad ingresada debe ser mayor a 0")
    private Integer cantidadIngresada;

    @NotBlank(message = "El proveedor es obligatorio")
    private String proveedor;

    private String edicion;
    private String fechaVigencia;

}
