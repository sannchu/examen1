package es.daw.bibliotecajdbc.exception;

/** Regla de negocio: no puede haber dos libros con el mismo ISBN. */
public class IsbnDuplicadoException extends RuntimeException {

    public IsbnDuplicadoException(String isbn) {
        super("Ya existe un libro con el ISBN " + isbn);
    }
}