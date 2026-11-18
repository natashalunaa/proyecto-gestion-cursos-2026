
package curso.flowable;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.context.Context;

public class CalcularCursosActivos implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {

        RuntimeService runtimeService = Context
                .getProcessEngineConfiguration()
                .getRuntimeService();

        String processDefinitionKey = Context
                .getProcessEngineConfiguration()
                .getRepositoryService()
                .getProcessDefinition(execution.getProcessDefinitionId())
                .getKey();

        // Contar las otras instancias activas
        long cantidad = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .active()
                .list()
                .stream()
                .filter(instancia ->
                        !instancia.getId().equals(
                                execution.getProcessInstanceId()
                        )
                )
                .count();

        // Guardar el resultado para la DMN
        execution.setVariable("cantidadCursosActivos", cantidad);

        System.out.println(
                "Otras instancias activas: " + cantidad
        );
    }
}
