package es.daw.productojdbc.repository;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones CRUD comunes a cualquier entidad.
 *
 * @param <T>  tipo de la entidad (Producto, Fabricante...)
 * @param <ID> tipo de su clave primaria (Integer, Long, String...)
 */
public interface GenericRepository<T, ID> {

    void save(T entidad);

    Optional<T> findById(ID id);

    List<T> findAll();

    void update(T entidad);

    void delete(ID id);
}
