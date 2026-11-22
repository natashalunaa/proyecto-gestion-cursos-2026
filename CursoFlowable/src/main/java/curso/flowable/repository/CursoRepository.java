
package curso.flowable.repository;

import curso.flowable.dto.SolicitudCurso;

import java.util.Map;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public class CursoRepository {

    private final JdbcTemplate jdbcTemplate;

    public CursoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Guardar una solicitud de curso en PostgreSQL
    public void guardarCurso(
            SolicitudCurso solicitud,
            String processInstanceId) {

        jdbcTemplate.update(
                "INSERT INTO cursos "
                        + "(empresa_id, process_instance_id, tematica, "
                        + "descripcion, cantidad_participantes, "
                        + "fechas_preferidas, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'en_evaluacion')",

                solicitud.empresaId(),
                processInstanceId,
                solicitud.tematica(),
                solicitud.descripcion(),
                solicitud.cantidadParticipantes(),
                solicitud.fechasPreferidas());

    }

    // Buscar un curso por su ID en PostgreSQL
    public Map<String, Object> buscarCursoPorId(Integer cursoId) {

        String sql = "SELECT * FROM cursos WHERE id = ?";

        List<Map<String, Object>> resultados = jdbcTemplate.queryForList(sql, cursoId);

        if (resultados.isEmpty()) {
            return null;
        }

        return resultados.get(0);
    }

    // Buscar un curso por la instancia del proceso en Flowable
    public Map<String, Object> buscarCursoPorProcessInstanceId(
            String processInstanceId) {

        String sql = "SELECT c.*, e.razon_social AS empresa "
                + "FROM cursos c "
                + "LEFT JOIN empresas e ON c.empresa_id = e.id "
                + "WHERE c.process_instance_id = ?";
        List<Map<String, Object>> resultados = jdbcTemplate.queryForList(sql, processInstanceId);

        if (resultados.isEmpty()) {
            return null;
        }

        return resultados.get(0);
    }

    // Guardar los datos de la propuesta de un curso
    public int guardarPropuesta(
            String processInstanceId,
            String programa,
            BigDecimal presupuesto,
            LocalDate fechaInicioPosible,
            LocalDate fechaFinPropuesta) {

        String sql = "UPDATE cursos "
                + "SET programa = ?, "
                + "presupuesto = ?, "
                + "fecha_inicio_posible = ?, "
                + "fecha_fin_propuesta = ? "
                + "WHERE process_instance_id = ?";

        return jdbcTemplate.update(
                sql,
                programa,
                presupuesto,
                fechaInicioPosible,
                fechaFinPropuesta,
                processInstanceId);
    }
}
