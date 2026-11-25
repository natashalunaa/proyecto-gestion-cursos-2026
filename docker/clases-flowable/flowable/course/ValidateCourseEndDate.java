package flowable.course;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;

import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

public class ValidateCourseEndDate implements TaskListener {

    @Override
    public void notify(DelegateTask task) {

        Object valor = task.getVariable("fechaCurso");

        if (valor == null) {
            throw new IllegalArgumentException(
                "No se encontro la fecha de inicio del curso."
            );
        }

        Instant inicio;

        if (valor instanceof Date) {
            inicio = ((Date) valor).toInstant();
        } else {
            inicio = OffsetDateTime.parse(valor.toString()).toInstant();
        }

        Instant fin = Instant.now();

        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException(
                "El curso no puede finalizar antes de su fecha de inicio."
            );
        }

        task.setVariable("fechaFinCurso", Date.from(fin));

        System.out.println("Fecha de finalizacion registrada: " + fin);
    }
}
