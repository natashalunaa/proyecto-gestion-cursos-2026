package curso.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class ReservarAula implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {

        // Simulacion de consulta a un servicio externo
        Boolean disponible =
            (Boolean) execution.getVariable("aulaDisponible");

        if (Boolean.FALSE.equals(disponible)) {
            throw new IllegalStateException(
                "No hay aulas disponibles"
            );
        }

        execution.setVariable("aulaAsignada", "Aula 101");
        execution.setVariable("aulaReservada", true);

        System.out.println("Aula reservada correctamente");
    }
}
