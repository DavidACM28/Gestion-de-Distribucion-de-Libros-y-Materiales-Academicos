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
import pe.incubadora.backend.utils.UpdateSedeResult;

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

    @Transactional
    public UpdateSedeResult updateSede(SedeIcpnaDTO dto, Long id) {
        SedeIcpnaEntity sede = sedeIcpnaRepository.findById(id).orElse(null);
        if (sede == null) {
            return UpdateSedeResult.SEDE_NOT_FOUND;
        }
        UpdateSedeResult result = validateSedeDTO(dto);
        if (result != null) {
            return result;
        }
        applyChanges(dto, sede);
        sedeIcpnaRepository.save(sede);
        return UpdateSedeResult.UPDATED;
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

    private UpdateSedeResult validateSedeDTO(SedeIcpnaDTO dto) {
        if (dto.getCodigo() != null) {
            if (dto.getCodigo().trim().isEmpty()) {
                return UpdateSedeResult.CODIGO_EMPTY;
            }
        }
        if (dto.getNombre() != null) {
            if (dto.getNombre().trim().length() < 3) {
                return UpdateSedeResult.NOMBRE_NOT_VALID;
            }
        }
        if (dto.getCiudad() != null) {
            if (dto.getCiudad().trim().isEmpty()) {
                return UpdateSedeResult.CIUDAD_EMPTY;
            }
        }
        if (dto.getDireccion() != null) {
            if (dto.getDireccion().trim().length() < 5) {
                return UpdateSedeResult.DIRECCION_NOT_VALID;
            }
        }
        if (dto.getResponsableLogistica() != null) {
            if (dto.getResponsableLogistica().trim().isEmpty()) {
                return  UpdateSedeResult.RESPONSABLE_EMPTY;
            }
        }
        if  (dto.getEstado() != null) {
            try {
                SedeEstado.valueOf(dto.getEstado().toUpperCase());
            } catch (IllegalArgumentException e) {
                return UpdateSedeResult.ESTADO_NOT_VALID;
            }
        }
        return null;
    }

    private void applyChanges(SedeIcpnaDTO dto, SedeIcpnaEntity sede) {
        if (dto.getCodigo() != null) {
            sede.setCodigo(dto.getCodigo());
        }
        if (dto.getNombre() != null) {
            sede.setNombre(dto.getNombre());
        }
        if (dto.getCiudad() != null) {
            sede.setCiudad(dto.getCiudad());
        }
        if (dto.getDireccion() != null) {
            sede.setDireccion(dto.getDireccion());
        }
        if (dto.getResponsableLogistica() != null) {
            sede.setResponsableLogistica(dto.getResponsableLogistica());
        }
        if (dto.getEstado() != null) {
            sede.setEstado(dto.getEstado().toUpperCase());
        }
        if (dto.getContacto() != null) {
            sede.setContacto(dto.getContacto());
        }
    }
}
