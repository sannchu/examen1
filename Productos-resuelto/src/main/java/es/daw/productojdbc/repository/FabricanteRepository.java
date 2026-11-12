package es.daw.productojdbc.repository;

import es.daw.productojdbc.dto.FabricanteResumen;
import es.daw.productojdbc.model.Fabricante;

import java.util.List;

public interface FabricanteRepository extends GenericRepository<Fabricante, Integer> {

    //------------ RETO 3 ----------------
    public abstract boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndCodigoNot(String nombre, Integer codigo);

    // -----------------------------------------

    // ------------- RETO 1 -------------
    List<FabricanteResumen> findAllWithNumProductos();


}
