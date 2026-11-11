package es.daw.bibliotecajdbc.repository;

import es.daw.bibliotecajdbc.exception.AccesoDatosException;
import es.daw.bibliotecajdbc.model.Autor;
import es.daw.bibliotecajdbc.model.Libro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LibroRepositoryJdbc implements LibroRepository {

    /*
     * SELECT común a todas las consultas.
     * Los alias a_id y a_nombre evitan la ambigüedad con el id del libro.
     */
    private static final String SELECT_BASE = """
            SELECT l.id, l.isbn, l.titulo, l.fecha_publicacion,
                   a.id AS a_id, a.nombre AS a_nombre
            FROM libro l
            JOIN autor a ON l.autor_id = a.id
            """;

    private final DataSource dataSource;


    @Override
    public Optional<Libro> findById(Integer id) {
        String sql = SELECT_BASE + "WHERE l.id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el libro " + id, e);
        }
    }

    @Override
    public Optional<Libro> findByIsbn(String isbn) {
        String sql = SELECT_BASE + "WHERE l.isbn = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el libro con ISBN " + isbn, e);
        }
    }

    @Override
    public List<Libro> findAll() {
        String sql = SELECT_BASE + "ORDER BY l.fecha_publicacion DESC, l.titulo";
        List<Libro> libros = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                libros.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los libros", e);
        }
        return libros;
    }


    @Override
    public void save(Libro libro) {
        String sql = """
                INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, libro.getIsbn());
            ps.setString(2, libro.getTitulo());
            ps.setInt(3, libro.getAutor().getId());
            ps.setObject(4, libro.getFechaPublicacion());   // LocalDate → DATE
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    libro.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al guardar el libro", e);
        }
    }

    /** El ISBN no se modifica: no aparece en el SET. */
    @Override
    public void update(Libro libro) {
        String sql = """
                UPDATE libro
                SET titulo = ?, autor_id = ?, fecha_publicacion = ?
                WHERE id = ?
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, libro.getTitulo());
            ps.setInt(2, libro.getAutor().getId());
            ps.setObject(3, libro.getFechaPublicacion());
            ps.setInt(4, libro.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el libro " + libro.getIsbn(), e);
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM libro WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al borrar el libro " + id, e);
        }
    }

    @Override
    public int countByAutorId(Integer autorId) {
        String sql = "SELECT COUNT(*) FROM libro WHERE autor_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, autorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();   // COUNT siempre devuelve una fila
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al contar los libros del autor " + autorId, e);
        }
    }

    private Libro mapRow(ResultSet rs) throws SQLException {
        Autor autor = new Autor(rs.getInt("a_id"), rs.getString("a_nombre"));

        return new Libro(
                rs.getInt("id"),
                rs.getString("isbn"),
                rs.getString("titulo"),
                autor,
                // getObject con el tipo: convierte DATE directamente a LocalDate
                rs.getObject("fecha_publicacion", LocalDate.class));
    }
}