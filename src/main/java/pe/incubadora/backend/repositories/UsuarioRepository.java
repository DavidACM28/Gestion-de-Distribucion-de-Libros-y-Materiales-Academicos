package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.incubadora.backend.entities.UsuarioEntity;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByUsername(String username);
}
