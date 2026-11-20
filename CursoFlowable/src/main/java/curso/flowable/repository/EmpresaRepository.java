
package curso.flowable.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class EmpresaRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmpresaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Contar todas las empresas registradas
    public Integer contarEmpresas() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM empresas",
                Integer.class);
    }

    // Comprobar si existe una empresa por su ID
    public boolean existeEmpresaPorId(Integer empresaId) {
        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM empresas WHERE id = ?",
                Integer.class,
                empresaId);

        return cantidad != null && cantidad > 0;
    }

    // Buscar el ID de una empresa por su CUIT
    public Integer buscarIdPorCuit(String cuit) {
        return jdbcTemplate.query(
                "SELECT id FROM empresas WHERE cuit = ?",
                rs -> rs.next() ? rs.getInt("id") : null,
                cuit);
    }

    // Registrar una empresa nueva y devolver su ID
    public Integer registrarEmpresa(
            String cuit,
            String razonSocial,
            String contacto,
            String email,
            String telefono) {

        String sql = "INSERT INTO empresas "
                + "(cuit, razon_social, nombre_contacto, email_contacto, telefono) "
                + "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "id" });
            ps.setString(1, cuit);
            ps.setString(2, razonSocial);
            ps.setString(3, contacto);
            ps.setString(4, email);
            ps.setString(5, telefono);

            return ps;

        }, keyHolder);

        return keyHolder.getKey().intValue();
    }
}
