package es.daw.bibliotecajdbc.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Datos del libro entre la vista y el controlador.
 *
 * - No lleva id: el id del libro no se publica nunca.
 * - edicion: false = alta, true = edición. Viaja oculto en el formulario.
 * - autorId: lo que envía el select.
 * - autorNombre: solo para mostrar en el listado.
 */
@Data
@NoArgsConstructor
public class LibroDto {

    // Con @NotNull + @Pattern, un campo vacío da un único error.
    // Con @NotBlank + @Pattern saldrían dos errores a la vez.
    @NotNull(message = "{libro.isbn.formato}")
    @Pattern(regexp = "\\d{13}", message = "{libro.isbn.formato}")
    private String isbn;

    private boolean edicion;

    @NotBlank(message = "{libro.titulo.obligatorio}")
    @Size(max = 150, message = "{libro.titulo.tamanio}")
    private String titulo;

    @NotNull(message = "{libro.autor.obligatorio}")
    private Integer autorId;

    private String autorNombre;

    // El navegador envía la fecha como 2026-11-03.
    // @DateTimeFormat le indica a Spring ese formato, tanto al recibir el dato
    // como al escribirlo en el <input type="date"> al editar.
    @NotNull(message = "{libro.fecha.obligatoria}")
    @PastOrPresent(message = "{libro.fecha.futura}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaPublicacion;
}
