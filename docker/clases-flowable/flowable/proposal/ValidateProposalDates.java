
package flowable.proposal;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

public class ValidateProposalDates implements TaskListener {

    @Override
    public void notify(DelegateTask task) {

        LocalDate inicio = convertirFecha(
                task.getVariable("fechaInicioPosible"));

        LocalDate fin = convertirFecha(
                task.getVariable("fechaFinPropuesta"));

        if (inicio == null || fin == null) {
            throw new IllegalArgumentException(
                    "Las fechas de la propuesta son obligatorias.");
        }

        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException(
                    "La fecha de finalizacion no puede ser anterior al inicio.");
        }
    }

    private LocalDate convertirFecha(Object valor) {
        if (valor == null) {
            return null;
        }

        if (valor instanceof Date) {
            return ((Date) valor).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        if (valor instanceof LocalDate) {
            return (LocalDate) valor;
        }

        return LocalDate.parse(valor.toString().substring(0, 10));
    }
}
