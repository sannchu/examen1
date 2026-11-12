package es.daw.productojdbc.dto;

import es.daw.productojdbc.model.Fabricante;
import es.daw.productojdbc.model.Producto;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto del formulario de producto (lo que viaja entre la vista y el controlador).
 *
 * ¿Por qué no usamos Producto directamente?
 * El <select> solo envía el código del fabricante (un número), no un Fabricante completo.
 * Aquí lo recogemos como Integer y lo validamos; después lo convertimos a Producto.
 */
@Data // añade getter, setter, toString, equals, hashcode, contructor para atributos final
@NoArgsConstructor
public class ProductoDto {

    private Integer codigo;            // null = alta, con valor = edición

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor que 0")
    private Double precio;

    @NotNull(message = "Selecciona un fabricante")
    private Integer codigoFabricante;


    // RETO 4
    @NotNull(message ="El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;



    // ----------- MAPEO ----------
    /** Producto (modelo) → formulario (vista). Se usa al editar. */
    public static ProductoDto toDto(Producto p) {
        ProductoDto form = new ProductoDto();
        form.setCodigo(p.getCodigo());
        form.setNombre(p.getNombre());
        form.setPrecio(p.getPrecio());
        form.setStock(p.getStock());
        form.setCodigoFabricante(p.getFabricante().getCodigo());
        return form;
    }

    /** Formulario (vista) → Producto (modelo). Del fabricante solo tenemos el código. */
    public Producto toModelEntity() {
        Fabricante fabricante = new Fabricante(codigoFabricante, null);
        return new Producto(codigo, nombre, precio, stock, fabricante);
    }
}
