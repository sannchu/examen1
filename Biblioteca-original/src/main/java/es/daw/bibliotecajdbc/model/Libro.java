package es.daw.bibliotecajdbc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Fila de la tabla LIBRO.
 *
 * - id: clave técnica. Se usa internamente y no se publica.
 * - isbn: clave de negocio. Es lo que ve el usuario y lo que viaja en las URL.
 * - autor: objeto completo, no el INT de la columna autor_id.
 * - fechaPublicacion: LocalDate, una fecha sin hora ni zona horaria,
 *   que es el equivalente en Java al tipo DATE de la base de datos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Libro {

    private Integer id;
    private String isbn;
    private String titulo;
    private Autor autor;
    private LocalDate fechaPublicacion;
}
