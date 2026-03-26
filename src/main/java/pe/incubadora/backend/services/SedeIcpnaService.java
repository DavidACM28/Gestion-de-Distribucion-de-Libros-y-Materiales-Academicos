package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.SedeIcpnaDTO;
import pe.incubadora.backend.entities.SedeIcpnaEntity;
import pe.incubadora.backend.repositories.SedeIcpnaRepository;
import pe.incubadora.backend.utils.CreateSedeResult;
import pe.incubadora.backend.utils.SedeEstado;

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
}
