package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.SedeIcpnaDTO;
import pe.incubadora.backend.entities.SedeIcpnaEntity;
import pe.incubadora.backend.repositories.SedeIcpnaRepository;
import pe.incubadora.backend.utils.CreateSedeResult;
import pe.incubadora.backend.utils.Rol;
import pe.incubadora.backend.utils.SedeEstado;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SedeIcpnaService {
    @Autowired
    private SedeIcpnaRepository sedeIcpnaRepository;

    @Transactional
    public CreateSedeResult createSede(SedeIcpnaDTO dto) {
        SedeEstado estado;
        try {
            estado = SedeEstado.valueOf(dto.getEstado().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CreateSedeResult.ESTADO_NOT_VALID;
        }
        SedeIcpnaEntity sede = new SedeIcpnaEntity();
        sede.setCodigo(dto.getCodigo());
        sede.setNombre(dto.getNombre());
        sede.setCiudad(dto.getCiudad());
        sede.setDireccion(dto.getDireccion());
        sede.setResponsableLogistica(dto.getResponsableLogistica());
        sede.setContacto(dto.getContacto());
        sede.setEstado(estado.name());
        sedeIcpnaRepository.save(sede);
        return CreateSedeResult.CREATED;
    }

    public Page<SedeIcpnaEntity> getSedes(Pageable page, Rol rol, Long sedeId) {
        if (rol == Rol.SEDE) {
            if (sedeId == null) {
                return Page.empty(page);
            }
            SedeIcpnaEntity sede = sedeIcpnaRepository.findById(sedeId).orElse(null);
            if (sede == null) {
                return Page.empty(page);
            }
            return new PageImpl<>(List.of(sede), page, 1);
        }
        return sedeIcpnaRepository.findAll(page);
    }

    public Optional<SedeIcpnaEntity> getSedeById(Rol rol, Long sedeId, Long sedeIdUsuario) {
        if (rol == Rol.SEDE) {
            if (!Objects.equals(sedeId, sedeIdUsuario)) {
                return Optional.empty();
            }
        }
        return sedeIcpnaRepository.findById(sedeId);
    }
}
