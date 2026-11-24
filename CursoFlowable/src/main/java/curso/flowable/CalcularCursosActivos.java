
package curso.flowable;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.context.Context;

public class CalcularCursosActivos implements JavaDelegate {

    private static final String ACTIVIDAD_HABILITACION = "Activity_1eg7zcl";

    @Override
    public void execute(DelegateExecution execution) {

        RuntimeService runtimeService = Context
                .getProcessEngineConfiguration()
                .getRuntimeService();

        HistoryService historyService = Context
                .getProcessEngineConfiguration()
                .getHistoryService();

        String processDefinitionKey = Context
                .getProcessEngineConfiguration()
                .getRepositoryService()
                .getProcessDefinition(execution.getProcessDefinitionId())
                .getKey();

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
                .filter(instancia ->
                        historyService
                                .createHistoricActivityInstanceQuery()
                                .processInstanceId(instancia.getId())
                                .activityId(ACTIVIDAD_HABILITACION)
                                .finished()
                                .count() > 0
                )
                .count();

        execution.setVariable("cantidadCursosActivos", cantidad);

        System.out.println(
                "Cursos en desarrollo: " + cantidad
        );
    }
}
