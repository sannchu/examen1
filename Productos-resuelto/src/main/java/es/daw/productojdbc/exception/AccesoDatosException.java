package es.daw.productojdbc.exception;

/**
 * Error técnico al acceder a la BD.
 * Envuelve la SQLException original (causa) para no perder la información del error.
 */
public class AccesoDatosException extends RuntimeException {

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}