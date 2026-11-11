package es.daw.bibliotecajdbc.service;

import es.daw.bibliotecajdbc.exception.IsbnDuplicadoException;
import es.daw.bibliotecajdbc.exception.RecursoNoEncontradoException;
import es.daw.bibliotecajdbc.model.Autor;
import es.daw.bibliotecajdbc.model.Libro;
import es.daw.bibliotecajdbc.repository.AutorRepository;
import es.daw.bibliotecajdbc.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;



    /** Todos los filtros son opcionales. */
    /** Todos los filtros son opcionales: si llega null, ese filtro no se aplica. */
    public List<Libro> buscar(Integer autorId, Integer anioDesde, Integer anioHasta) {
        return libroRepository.findAll().stream()
                .filter(l -> autorId == null || l.getAutor().getId().equals(autorId))
                .filter(l -> anioDesde == null || l.getFechaPublicacion().getYear() >= anioDesde)
                .filter(l -> anioHasta == null || l.getFechaPublicacion().getYear() <= anioHasta)
                .toList();
    }

    public Libro buscarPorIsbn(String isbn) {
        return libroRepository.findByIsbn(isbn)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el libro con ISBN " + isbn));
    }

    public Libro crear(Libro libro) {
        // Regla de negocio: el ISBN no se puede repetir
        if (libroRepository.findByIsbn(libro.getIsbn()).isPresent()) {
            throw new IsbnDuplicadoException(libro.getIsbn());
        }
        prepararParaGuardar(libro);
        libroRepository.save(libro);
        return libro;
    }

    /**
     * El libro llega del mapper sin id, porque el DTO no lo lleva.
     * Lo localizamos por ISBN para recuperar su id, que es lo que usa el repositorio.
     */
    public Libro actualizar(Libro libro) {
        Libro existente = buscarPorIsbn(libro.getIsbn());
        libro.setId(existente.getId());
        prepararParaGuardar(libro);
        libroRepository.update(libro);
        return libro;
    }

    public void borrar(String isbn) {
        Libro libro = buscarPorIsbn(isbn);
        libroRepository.delete(libro.getId());
    }

    // Reglas comunes a crear y actualizar
    private void prepararParaGuardar(Libro libro) {
        libro.setTitulo(libro.getTitulo().trim());

        // Regla de negocio: el autor tiene que existir
        Integer autorId = libro.getAutor().getId();
        Autor autor = autorRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el autor indicado"));
        libro.setAutor(autor);   // ahora lleva también el nombre
    }
}