package es.daw.productojdbc.exception;
public class FabricanteDuplicadoException extends RuntimeException {
    public FabricanteDuplicadoException() { super("Ya existe un fabricante con ese nombre"); }
}
