package es.daw.productojdbc.repository;

import es.daw.productojdbc.dto.FabricanteResumen;
import es.daw.productojdbc.exception.AccesoDatosException;
import es.daw.productojdbc.model.Fabricante;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FabricanteRepositoryJdbc implements FabricanteRepository {

    private final DataSource dataSource;

//    public FabricanteRepositoryJdbc(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }

    @Override
    public Optional<Fabricante> findById(Integer codigo) {
        String sql = "SELECT codigo, nombre FROM fabricante WHERE codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el fabricante " + codigo, e);
        }
    }

    @Override
    public List<Fabricante> findAll() {
        String sql = "SELECT codigo, nombre FROM fabricante ORDER BY nombre";
        List<Fabricante> fabricantes = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                fabricantes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los fabricantes", e);
        }
        return fabricantes;
    }

    @Override
    public void save(Fabricante fabricante) {
        String sql = "INSERT INTO fabricante (nombre) VALUES (?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fabricante.getNombre());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    fabricante.setCodigo(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al guardar el fabricante", e);
        }
    }

    @Override
    public void update(Fabricante fabricante) {
        String sql = "UPDATE fabricante SET nombre = ? WHERE codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fabricante.getNombre());
            ps.setInt(2, fabricante.getCodigo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el fabricante " + fabricante.getCodigo(), e);
        }
    }

    /**
     * El servicio comprueba antes que no tenga productos.
     * Si aun así los tuviera, la FK lo impide y se lanza AccesoDatosException:
     * la BD es la última barrera.
     */
    @Override
    public void delete(Integer codigo) {
        String sql = "DELETE FROM fabricante WHERE codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al borrar el fabricante " + codigo, e);
        }
    }


    // ----------- RETO 3 ------------------
    @Override
    public boolean existsByNombreIgnoreCase(String nombre) {
        // En bd tengo ASUS y llega asus -> true
        // En bd tengo ASUS y llega Asus -> true
        // En bd tengo ASUS y llega ASUS -> true
        // En bd tengo asus y llega ASUS  -> true
        // .......
        String sql = "SELECT 1 FROM fabricante WHERE LOWER(nombre) = LOWER(?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // TRUE si encuentra el fabricante con ese nombre...
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al comprobar el nombre del fabricante: " + nombre, e);
        }

    }

    @Override
    public boolean existsByNombreIgnoreCaseAndCodigoNot(String nombre, Integer codigo) {
        String sql = "SELECT 1 FROM fabricante WHERE LOWER(nombre) = LOWER(?) AND codigo <> ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // TRUE si encuentra el fabricante con ese nombre...
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al comprobar el nombre del fabricante: " + nombre, e);
        }

    }

    // --------------- RETO 1 ----------------------------
    @Override
    public List<FabricanteResumen> findAllWithNumProductos() {
        String sql = """
                        SELECT f.codigo, f.nombre, COUNT(p.codigo) AS num_productos
                        FROM fabricante f
                        LEFT JOIN producto p ON p.codigo_fabricante = f.codigo
                        GROUP BY f.codigo, f.nombre
                        ORDER BY f.nombre                        
                        """;

        List<FabricanteResumen> fabricantes = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                fabricantes.add(new FabricanteResumen(
                        rs.getInt("codigo"),
                        rs.getString("nombre"),
                        rs.getInt("num_productos")
                ));
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los fabricantes con su nº de productos", e);
        }
        return fabricantes;
    }



    // ----------------
    private Fabricante mapRow(ResultSet rs) throws SQLException {
        return new Fabricante(
                rs.getInt("codigo"),
                rs.getString("nombre"));
    }
}