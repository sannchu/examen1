package es.daw.productojdbc.exception;

/** Se lanza cuando findById devuelve Optional.empty() y necesitamos el objeto. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}