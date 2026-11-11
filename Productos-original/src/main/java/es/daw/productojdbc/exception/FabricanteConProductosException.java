package es.daw.productojdbc.exception;

/** Regla de negocio: no se borra un fabricante que tiene productos. */
public class FabricanteConProductosException extends RuntimeException {

    public FabricanteConProductosException(Integer codigo) {
        super("No se puede borrar el fabricante " + codigo + ": tiene productos asociados.");
    }
}
