package es.daw.bibliotecajdbc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos del autor entre la vista y el controlador.
 * El id viaja porque el autor no tiene clave de negocio: lo necesitan las URL
 * de edición y borrado y el value del select. Nunca se muestra como dato.
 */
@Data
@NoArgsConstructor
public class AutorDto {

    private Integer id;      // null = alta, con valor = edición

    @NotBlank(message = "{autor.nombre.obligatorio}")
    @Size(max = 100, message = "{autor.nombre.tamanio}")
    private String nombre;
}