
package historico;

import java.util.List;

import org.flowable.engine.history.HistoricProcessInstance;

public class App {

    public static void main(String[] args) {

        java.util.TimeZone.setDefault(
                java.util.TimeZone.getTimeZone("UTC")
        );

        try (FlowableHistoryService flowable =
                     new FlowableHistoryService()) {

            List<HistoricProcessInstance> instancias =
                    flowable.getHistoryService()
                            .createHistoricProcessInstanceQuery()
                            .finished()
                            .list();

            System.out.println("\n=== CONSULTAS HISTÓRICAS ===");
            System.out.println(
                    "Instancias finalizadas: " + instancias.size()
            );

            for (HistoricProcessInstance instancia : instancias) {

                System.out.println("---------------------------");
                System.out.println(
                        "Proceso: " + instancia.getProcessDefinitionKey()
                );
                System.out.println(
                        "ID: " + instancia.getId()
                );
                System.out.println(
                        "Inicio: " + instancia.getStartTime()
                );
                System.out.println(
                        "Fin: " + instancia.getEndTime()
                );
                System.out.println(
                        "Duración (ms): " + instancia.getDurationInMillis()
                );
            }

                ConsultasHistoricas consultas =
                        new ConsultasHistoricas(flowable.getHistoryService());

                consultas.consultarResultadosSolicitudes();
                consultas.consultarTareasPorEmpleado();
                consultas.consultarDuracionPromedioPreparacion();

        } catch (Exception e) {
            System.out.println(
                    "Error al consultar el historial: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }
}
