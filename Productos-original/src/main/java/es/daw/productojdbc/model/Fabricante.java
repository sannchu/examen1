package es.daw.productojdbc.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fila de la tabla FABRICANTE.
 * POJO sin anotaciones de persistencia: el mapeo fila → objeto lo hacemos a mano con JDBC.
 * (Las anotaciones de validación son para el formulario web, no para la BD).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fabricante {

    private Integer codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;
}