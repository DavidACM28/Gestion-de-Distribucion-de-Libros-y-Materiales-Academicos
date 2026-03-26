package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.MaterialAcademicoEntity;

@Repository
public interface MaterialAcademicoRepository extends JpaRepository<MaterialAcademicoEntity, Long> {
}
