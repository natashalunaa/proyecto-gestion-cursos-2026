
package curso.flowable;

import java.util.List;

import org.flowable.engine.TaskService;
import org.flowable.engine.impl.context.Context;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.engine.delegate.TaskListener;

public class AsignarAdministrativoListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {

        // Obtener los servicios de Flowable
        TaskService taskService = Context
                .getProcessEngineConfiguration()
                .getTaskService();

        // Buscar los usuarios del grupo Administracion
        List<org.flowable.idm.api.User> usuarios = Context
        .getProcessEngineConfiguration()
        .getIdentityService()
        .createUserQuery()
        .memberOfGroup("Administracion")
        .list();

        String elegido = null;
        long menorCantidad = Long.MAX_VALUE;

        // Revisar cuantas tareas tiene cada administrativo
        for (org.flowable.idm.api.User usuario : usuarios) {

            String usuarioId = usuario.getId();

            long cantidad = taskService
                    .createTaskQuery()
                    .taskAssignee(usuarioId)
                    .count();

            if (cantidad < menorCantidad) {
                menorCantidad = cantidad;
                elegido = usuarioId;
            }
        }

        // Asignar la tarea al administrativo elegido
        if (elegido != null) {
            delegateTask.setAssignee(elegido);

            System.out.println(
                    "Tarea asignada a: " + elegido
                    + " (tareas pendientes: " + menorCantidad + ")"
            );
        }
    }
}

