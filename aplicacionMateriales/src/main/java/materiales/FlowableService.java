
package materiales;

import java.util.List;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;

public class FlowableService implements AutoCloseable {

    private final ProcessEngine processEngine;
    private final TaskService taskService;

    public FlowableService() {

        processEngine = ProcessEngineConfiguration
                .createStandaloneProcessEngineConfiguration()
                .setJdbcDriver("org.postgresql.Driver")
                .setJdbcUrl("jdbc:postgresql://127.0.0.1:5433/flowable")
                .setJdbcUsername("flowable")
                .setJdbcPassword("flow10")
                .setDatabaseSchemaUpdate(
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE
                )
                .setAsyncExecutorActivate(false)
                .buildProcessEngine();

        taskService = processEngine.getTaskService();
    }

    
    public List<Task> listarTareasAsignadas(String usuario) {

        return taskService.createTaskQuery()
                .taskAssignee(usuario)
                .taskName("Preparar materiales")
                .list();
    }


    
    public List<Task> listarTareasDisponibles(String usuario) {

        return taskService.createTaskQuery()
                .taskCandidateUser(usuario)
                .taskName("Preparar materiales")
                .list();
    }


    public void reclamarTarea(String tareaId, String usuario) {

        taskService.claim(tareaId, usuario);
    }

    public void completarTarea(String tareaId, String usuario) {

        Task tarea = taskService.createTaskQuery()
                .taskId(tareaId)
                .singleResult();

        if (tarea == null) {
            throw new IllegalArgumentException(
                    "La tarea ya no existe."
            );
        }

        if (!usuario.equals(tarea.getAssignee())) {
            throw new IllegalStateException(
                    "La tarea no está asignada al usuario."
            );
        }

        taskService.complete(tareaId);
    }

    @Override
    public void close() {
        processEngine.close();
    }
}

