package pe.incubadora.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AprobarSolicitudDTO {
    private List<AprobarSolicitudDetalleDTO> items;
    private String comentarioRevision;
}
