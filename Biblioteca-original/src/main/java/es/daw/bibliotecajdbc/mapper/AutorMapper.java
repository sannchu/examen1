package es.daw.bibliotecajdbc.mapper;

import es.daw.bibliotecajdbc.dto.AutorDto;
import es.daw.bibliotecajdbc.model.Autor;
import org.springframework.stereotype.Component;

/** Convierte entre AutorDto y Autor. Solo convierte: no usa servicios ni repositorios. */
@Component
public class AutorMapper {

    public AutorDto toDto(Autor autor) {
        AutorDto dto = new AutorDto();
        dto.setId(autor.getId());
        dto.setNombre(autor.getNombre());
        return dto;
    }

    public Autor toModelEntity(AutorDto dto) {
        return new Autor(dto.getId(), dto.getNombre());
    }
}
