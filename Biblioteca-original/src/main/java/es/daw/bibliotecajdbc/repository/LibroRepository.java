package es.daw.bibliotecajdbc.repository;

import es.daw.bibliotecajdbc.model.Libro;

import java.util.List;
import java.util.Optional;

public interface LibroRepository extends GenericRepository<Libro, Integer> {

    /** El ISBN es la clave de negocio: el usuario identifica los libros por él. */
    Optional<Libro> findByIsbn(String isbn);

    /** Cuántos libros tiene un autor. Lo usa AutorService antes de borrar. */
    int countByAutorId(Integer autorId);

}