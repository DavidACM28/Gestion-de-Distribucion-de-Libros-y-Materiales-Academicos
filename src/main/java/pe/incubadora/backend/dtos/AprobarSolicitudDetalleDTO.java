package pe.incubadora.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AprobarSolicitudDetalleDTO {
    private Long detalleId;
    private Integer cantidadAprobada;
}
