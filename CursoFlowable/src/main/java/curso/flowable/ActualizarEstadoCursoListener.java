
package curso.flowable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

public class ActualizarEstadoCursoListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {

        String processInstanceId = execution.getProcessInstanceId();

        String resultadoProceso =
                (String) execution.getVariable("resultadoProceso");

        if (resultadoProceso == null) {
            throw new IllegalStateException(
                "No se definió el resultado del proceso"
            );
        }

        String url =  "jdbc:postgresql://flowable-postgres:5432/institucion_cursos";
        String usuario = "flowable";
        String password = System.getenv("DB_PASSWORD");

        String sql = "UPDATE cursos SET estado = ? WHERE process_instance_id = ?";

        try (Connection conexion =
                    DriverManager.getConnection(url, usuario, password);
             PreparedStatement sentencia =
                    conexion.prepareStatement(sql)) {

            sentencia.setString(1, resultadoProceso);
            sentencia.setString(2, processInstanceId);

            int filasActualizadas = sentencia.executeUpdate();

            if (filasActualizadas != 1) {
                throw new IllegalStateException(
                    "Se esperaba actualizar un curso, pero se actualizaron "
                    + filasActualizadas
                    + ". Instancia: " + processInstanceId
                );
            }

            System.out.println(
                "Curso actualizado correctamente. Estado: " + resultadoProceso
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                "Error al actualizar el estado del curso", e
            );
        }
    }
}
