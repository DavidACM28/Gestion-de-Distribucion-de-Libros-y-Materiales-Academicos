package pe.incubadora.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.incubadora.backend.entities.SedeIcpnaEntity;
import pe.incubadora.backend.utils.Rol;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO {

    private Long id;
    private SedeIcpnaEntity sede;
    private String username;
    private Rol rol;
    private boolean activo;
}
