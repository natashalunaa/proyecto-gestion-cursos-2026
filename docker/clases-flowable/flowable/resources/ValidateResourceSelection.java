package flowable.resources;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

// Valida que se seleccione al menos un recurso para el curso.
public class ValidateResourceSelection implements TaskListener {

    // Verifica los recursos al completar la tarea Evaluar Recursos.
    @Override
    public void notify(DelegateTask task) {

        boolean requiereAula = Boolean.TRUE.equals(
                task.getVariable("requiereAula"));

        boolean requierePlataforma = Boolean.TRUE.equals(
                task.getVariable("requierePlataforma"));

        boolean requiereEquipamiento = Boolean.TRUE.equals(
                task.getVariable("requiereEquipamiento"));

        if (!requiereAula &&
            !requierePlataforma &&
            !requiereEquipamiento) {

            throw new IllegalArgumentException(
                    "Debe seleccionar al menos un recurso.");
        }
    }
}