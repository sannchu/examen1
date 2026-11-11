package es.daw.productojdbc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// ---------------- RETO 1 --------------

@Data
@AllArgsConstructor
public class FabricanteResumen {

    private Integer codigo;
    private String nombre;
    private int numProductos;
}