package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.auth.RegisterDTO;
import pe.incubadora.backend.entities.SedeIcpnaEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.repositories.SedeIcpnaRepository;
import pe.incubadora.backend.repositories.UsuarioRepository;
import pe.incubadora.backend.utils.RegisterUsuarioResult;
import pe.incubadora.backend.utils.Rol;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SedeIcpnaRepository sedeIcpnaRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public RegisterUsuarioResult register(RegisterDTO dto) {
        Rol rol;
        SedeIcpnaEntity sede;
        try {
            rol = Rol.valueOf(dto.getRol().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RegisterUsuarioResult.ROL_NOT_FOUND;
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(dto.getUsername());
        usuario.setPassword(encoder.encode(dto.getPassword()));
        usuario.setRol(rol);
        usuario.setActivo(true);

        switch (rol) {
            case SEDE:
                if (dto.getSedeId() == null) {
                    return RegisterUsuarioResult.SIN_SEDE;
                }
                sede = sedeIcpnaRepository.findById(dto.getSedeId()).orElse(null);
                if (sede == null) {
                    return RegisterUsuarioResult.SEDE_NOT_FOUND;
                }
                usuario.setSede(sede);
                break;
            case ADMIN, ALMACEN:
                if (dto.getSedeId() != null) {
                    return RegisterUsuarioResult.SEDE_NOT_REQUIRED;
                }
                break;
        }
        usuarioRepository.save(usuario);
        return RegisterUsuarioResult.CREATED;
    }
}
