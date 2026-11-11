package es.daw.productojdbc.repository;

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


    private Fabricante mapRow(ResultSet rs) throws SQLException {
        return new Fabricante(
                rs.getInt("codigo"),
                rs.getString("nombre"));
    }

}