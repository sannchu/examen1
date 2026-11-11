package es.daw.productojdbc.repository;

import es.daw.productojdbc.exception.AccesoDatosException;
import es.daw.productojdbc.model.Fabricante;
import es.daw.productojdbc.model.Producto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductoRepositoryJdbc implements ProductoRepository {

    /*
     * SELECT común a findById y findAll.
     * Hacemos JOIN con fabricante para traer también su nombre y poder construir
     * el objeto Fabricante dentro de cada Producto.
     * Los alias (f_codigo, f_nombre) evitan la ambigüedad: las dos tablas tienen "codigo" y "nombre".
     */
    private static final String SELECT_BASE = """
            SELECT p.codigo, p.nombre, p.precio, p.stock,
                   f.codigo AS f_codigo, f.nombre AS f_nombre
            FROM producto p
            JOIN fabricante f ON p.codigo_fabricante = f.codigo
            """;

    private final DataSource dataSource;

    // Spring crea el DataSource a partir de application.properties y lo inyecta aquí
//    public ProductoRepositoryJdbc(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }

    // ---------------------------------------------------------------- READ (uno)
    @Override
    public Optional<Producto> findById(Integer codigo) {
        String sql = SELECT_BASE + "WHERE p.codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();

                //---------------------------
                // si devuelve una fila, es que ha encontrado el producto por código
//                if (rs.next()) {
//                    Fabricante fabricante = new Fabricante(
//                            rs.getInt("f_codigo"),
//                            rs.getString("f_nombre"));
//
//                    Producto producto=  new Producto(
//                            rs.getInt("codigo"),
//                            rs.getString("nombre"),
//                            rs.getDouble("precio"),
//                            fabricante);
//
//                    return Optional.of(producto);
//                }else{ // no ha encontrado el producto por código
//                    return Optional.empty();
//                }
                //---------------------------

            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el producto " + codigo, e);
        }
    }

    // ---------------------------------------------------------------- READ (todos)
    @Override
    public List<Producto> findAll() {
        String sql = SELECT_BASE + "ORDER BY p.codigo";
        List<Producto> productos = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapRow(rs));

            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los productos", e);
        }
        return productos;
    }

    // ---------------------------------------------------------------- CREATE
    @Override
    public void save(Producto producto) {
        String sql = "INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES (?, ?, ?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, producto.getNombre());
            ps.setDouble(2, producto.getPrecio());
            ps.setInt(3, producto.getStock());
            ps.setInt(4, producto.getFabricante().getCodigo());
            ps.executeUpdate();

            // Recuperamos el código que ha generado AUTO_INCREMENT y se lo asignamos al objeto
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    producto.setCodigo(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al guardar el producto", e);
        }
    }

    // ---------------------------------------------------------------- UPDATE
    @Override
    public void update(Producto producto) {
        String sql = "UPDATE producto SET nombre = ?, precio = ?, stock = ?, codigo_fabricante = ? WHERE codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, producto.getNombre());
            ps.setDouble(2, producto.getPrecio());
            ps.setInt(3, producto.getStock());
            ps.setInt(4, producto.getFabricante().getCodigo());
            ps.setInt(5, producto.getCodigo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el producto " + producto.getCodigo(), e);
        }
    }

    // ---------------------------------------------------------------- DELETE
    @Override
    public void delete(Integer codigo) {
        String sql = "DELETE FROM producto WHERE codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al borrar el producto " + codigo, e);
        }
    }

    // ---------------------------------------------------------------- Consulta propia
    @Override
    public boolean existsByFabricanteCodigo(Integer codigoFabricante) {
        String sql = "SELECT 1 FROM producto WHERE codigo_fabricante = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigoFabricante);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();   // con que haya una fila, basta
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al comprobar los productos del fabricante " + codigoFabricante, e);
        }
    }

    // ---------------------------------------------------------------- Mapeo fila → objeto
    private Producto mapRow(ResultSet rs) throws SQLException {
        Fabricante fabricante = new Fabricante(
                rs.getInt("f_codigo"),
                rs.getString("f_nombre"));

        return new Producto(
                rs.getInt("codigo"),
                rs.getString("nombre"),
                rs.getDouble("precio"),
                rs.getInt("stock"),
                fabricante);
    }
    @Override
    public List<Producto> findByFabricanteCodigo(Integer codigoFabricante) {
        String sql = SELECT_BASE + " WHERE p.codigo_fabricante = ? ORDER BY p.codigo";
        List<Producto> lista = new ArrayList<>();
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigoFabricante);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) lista.add(mapRow(rs)); }
            return lista;
        } catch (SQLException e) { throw new AccesoDatosException("Error al buscar productos por fabricante", e); }
    }
}