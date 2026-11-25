package flowable.report;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

public class ValidateReport implements TaskListener {

    @Override
    public void notify(DelegateTask task) {

        Object informe = task.getVariable("informeResultados");

        Object adjunto = task.getVariable("informeAdjunto");

        boolean tieneTexto = informe != null
                && !informe.toString().isBlank();

        boolean tieneAdjunto = Boolean.TRUE.equals(adjunto);

        if (!tieneTexto && !tieneAdjunto) {
            throw new IllegalArgumentException(
                    "Debe ingresar un informe de resultados "
                    + "o adjuntar un archivo PDF.");
        }
    }
}