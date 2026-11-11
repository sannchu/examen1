package es.daw.bibliotecajdbc.exception;

/** Se pide un libro o un autor que no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
