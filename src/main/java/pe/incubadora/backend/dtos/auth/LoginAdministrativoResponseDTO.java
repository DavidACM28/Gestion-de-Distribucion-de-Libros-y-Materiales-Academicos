package pe.incubadora.backend.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginAdministrativoResponseDTO {

    private String token;
    private String tokenTpe;
}
