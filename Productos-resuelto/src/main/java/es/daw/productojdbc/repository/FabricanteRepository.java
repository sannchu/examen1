package es.daw.productojdbc.repository;

import es.daw.productojdbc.model.Fabricante;

public interface FabricanteRepository extends GenericRepository<Fabricante, Integer> {
    java.util.List<es.daw.productojdbc.dto.FabricanteResumen> findAllConNumeroProductos();
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndCodigoNot(String nombre, Integer codigo);
}
