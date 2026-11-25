package flowable.proposal;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

public class IncrementProposalVersion implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {

        Object valor = execution.getVariable("versionPropuesta");

        int version = valor == null
                ? 1
                : Integer.parseInt(valor.toString());

        execution.setVariable("versionPropuesta", version + 1);

        System.out.println("Nueva version de propuesta: " + (version + 1));
    }
}
