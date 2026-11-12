package es.daw.productojdbc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fila de la tabla FABRICANTE.
 * POJO sin anotaciones de persistencia: el mapeo fila → objeto lo hacemos a mano con JDBC.
 * (Las anotaciones de validación son para el formulario web, no para la BD).
 */
@Data // getter, setter, constructor de atributos final, equals, hashcode, toString
@NoArgsConstructor // constructor vacío
@AllArgsConstructor // constructor completo
public class Fabricante {

    private Integer codigo;
    private String nombre;
}