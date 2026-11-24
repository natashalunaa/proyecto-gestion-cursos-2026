package curso.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class LiberarAula implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {

        if (Boolean.TRUE.equals(
                execution.getVariable("aulaReservada"))) {

            System.out.println(
                "Liberando aula: "
                + execution.getVariable("aulaAsignada")
            );

            execution.setVariable("aulaReservada", false);
            execution.removeVariable("aulaAsignada");
        }
    }
}
