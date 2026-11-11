package es.daw.bibliotecajdbc.service;

import es.daw.bibliotecajdbc.exception.RecursoNoEncontradoException;
import es.daw.bibliotecajdbc.model.Autor;
import es.daw.bibliotecajdbc.repository.AutorRepository;
import es.daw.bibliotecajdbc.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutorService {

    private final AutorRepository autorRepository;
    private final LibroRepository libroRepository;


    public List<Autor> listar() {
        return autorRepository.findAll();
    }

    public Autor buscarPorId(Integer id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el autor indicado"));
    }

    public Autor crear(Autor autor) {
        autor.setNombre(autor.getNombre().trim());
        autorRepository.save(autor);
        return autor;
    }

    public Autor actualizar(Autor autor) {
        buscarPorId(autor.getId());
        autor.setNombre(autor.getNombre().trim());
        autorRepository.update(autor);
        return autor;
    }

    /**
     * Borra el autor y devuelve cuántos libros tenía.
     * Los libros NO se borran aquí: lo hace la BD por el ON DELETE CASCADE.
     * Por eso hay que contarlos antes: después ya no existen.
     */
    public int borrar(Integer id) {
        buscarPorId(id);
        int numLibros = libroRepository.countByAutorId(id);
        autorRepository.delete(id);
        return numLibros;
    }
}