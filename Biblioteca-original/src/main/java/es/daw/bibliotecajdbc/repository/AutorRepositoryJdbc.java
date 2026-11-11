package es.daw.bibliotecajdbc.repository;

import es.daw.bibliotecajdbc.exception.AccesoDatosException;
import es.daw.bibliotecajdbc.model.Autor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AutorRepositoryJdbc implements AutorRepository {

    private final DataSource dataSource;


    @Override
    public Optional<Autor> findById(Integer id) {
        String sql = "SELECT id, nombre FROM autor WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el autor " + id, e);
        }
    }

    @Override
    public List<Autor> findAll() {
        String sql = "SELECT id, nombre FROM autor ORDER BY nombre";
        List<Autor> autores = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                autores.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los autores", e);
        }
        return autores;
    }

    @Override
    public void save(Autor autor) {
        String sql = "INSERT INTO autor (nombre) VALUES (?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, autor.getNombre());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    autor.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al guardar el autor", e);
        }
    }

    @Override
    public void update(Autor autor) {
        String sql = "UPDATE autor SET nombre = ? WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, autor.getNombre());
            ps.setInt(2, autor.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el autor " + autor.getId(), e);
        }
    }

    /**
     * Solo borra de la tabla autor.
     * Sus libros los borra la BD por el ON DELETE CASCADE de schema.sql.
     */
    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM autor WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al borrar el autor " + id, e);
        }
    }

    private Autor mapRow(ResultSet rs) throws SQLException {
        return new Autor(rs.getInt("id"), rs.getString("nombre"));
    }
}