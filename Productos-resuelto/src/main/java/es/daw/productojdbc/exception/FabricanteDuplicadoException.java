package es.daw.productojdbc.exception;

public class FabricanteDuplicadoException extends RuntimeException {
    public FabricanteDuplicadoException(String nombre) {
        //super("Ya existe un fabricante con el nombre: " + nombre);
        super("Ya existe un fabricante llamado \""+nombre+"\".");
    }
}
