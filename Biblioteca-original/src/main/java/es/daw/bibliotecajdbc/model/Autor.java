package es.daw.bibliotecajdbc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fila de la tabla AUTOR.
 * POJO sin anotaciones: ni de persistencia ni de validación.
 * Las validaciones del formulario están en AutorDto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Autor {

    private Integer id;
    private String nombre;
}