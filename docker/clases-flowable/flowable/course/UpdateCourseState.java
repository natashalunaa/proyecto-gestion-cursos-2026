package flowable.course;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.DelegateHelper;

public class UpdateCourseState implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {

        String estado = String.valueOf(
            DelegateHelper.getFieldExpression(execution, "estado")
                .getValue(execution)
        );

        String url = "jdbc:postgresql://flowable-postgres:5432/institucion_cursos";
        String usuario = "flowable";
        String password = System.getenv("DB_PASSWORD");

        String sql = "UPDATE cursos SET estado = ? WHERE process_instance_id = ?";

        try (Connection conexion =
                DriverManager.getConnection(url, usuario, password);
             PreparedStatement sentencia =
                conexion.prepareStatement(sql)) {

            sentencia.setString(1, estado);
            sentencia.setString(2, execution.getProcessInstanceId());

            if (sentencia.executeUpdate() != 1) {
                throw new IllegalStateException(
                    "No se pudo actualizar el curso."
                );
            }

            System.out.println("Estado actualizado: " + estado);

        } catch (SQLException e) {
            throw new RuntimeException(
                "Error al actualizar el estado del curso", e
            );
        }
    }
}
