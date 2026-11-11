package es.daw.productojdbc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fila de la tabla PRODUCTO.
 *
 * En la tabla hay una columna codigo_fabricante (un INT, la FK).
 * En Java NO guardamos ese INT suelto: guardamos el objeto Fabricante completo.
 * Rellenarlo es trabajo nuestro (JOIN + mapeo manual). En la UT04, JPA lo hará con @ManyToOne.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    private Integer codigo;
    private String nombre;
    private double precio;
    private Fabricante fabricante;
}