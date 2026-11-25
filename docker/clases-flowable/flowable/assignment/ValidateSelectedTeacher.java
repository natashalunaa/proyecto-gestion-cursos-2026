package flowable.assignment;

import org.flowable.engine.ProcessEngines;
import org.flowable.engine.IdentityService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

public class ValidateSelectedTeacher implements TaskListener {

    @Override
    public void notify(DelegateTask task) {

        Object valor = task.getVariable("docenteSeleccionado");

        if (valor == null || valor.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Debe seleccionar un docente."
            );
        }

        String docente = valor.toString().trim();

        IdentityService identityService =
            ProcessEngines.getDefaultProcessEngine()
                .getIdentityService();

        long cantidad = identityService.createUserQuery()
            .userId(docente)
            .memberOfGroup("Docentes")
            .count();

        if (cantidad == 0) {
            throw new IllegalArgumentException(
                "El usuario seleccionado no pertenece al grupo Docentes."
            );
        }

        task.setVariable("docenteSeleccionado", docente);

        System.out.println("Docente seleccionado: " + docente);
    }
}
