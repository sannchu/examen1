package es.daw.productojdbc.dto;

import es.daw.productojdbc.model.Fabricante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // añade getter, setter, toString, equals, hashcode, contructor para atributos final
@NoArgsConstructor
public class FabricanteDto {
    private Integer codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    // ------------MAPEO --------
    /** Fabricante (modelo) → formulario (vista). Se usa al editar. */
    public static FabricanteDto toDto(Fabricante f) {
        FabricanteDto fabricanteDto = new FabricanteDto();
        fabricanteDto.setCodigo(f.getCodigo());
        fabricanteDto.setNombre(f.getNombre());
        return fabricanteDto;
    }

    /** Formulario (vista) → Fabricante (modelo).*/
    public static Fabricante toModelEntity(FabricanteDto f) {

        return new Fabricante(f.getCodigo(), f.getNombre());
    }

}
