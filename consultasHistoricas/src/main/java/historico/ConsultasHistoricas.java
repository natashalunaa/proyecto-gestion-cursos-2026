
package historico;


import java.util.HashMap;
import org.flowable.variable.api.history.HistoricVariableInstance;
import java.util.List;
import java.util.Map;

import java.util.TreeMap;

import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;

public class ConsultasHistoricas {

    private final HistoryService historyService;

    private static final String PROCESO = "process-proyecto-curso";

    public ConsultasHistoricas(HistoryService historyService) {
        this.historyService = historyService;
    }

    public void consultarResultadosSolicitudes() {

        List<HistoricProcessInstance> instancias =
                historyService.createHistoricProcessInstanceQuery()
                        .processDefinitionKey(PROCESO)
                        .finished()
                        .list();

        int noViables = 0;
        int rechazadas = 0;
        int sinRespuesta = 0;
        int canceladas = 0;
        int realizadas = 0;
        int sinClasificar = 0;

        for (HistoricProcessInstance instancia : instancias) {

            HistoricVariableInstance variable =
                    historyService.createHistoricVariableInstanceQuery()
                            .processInstanceId(instancia.getId())
                            .variableName("resultadoProceso")
                            .singleResult();

            if (variable == null || variable.getValue() == null
                    || instancia.getDeleteReason() != null) {
                sinClasificar++;
                continue;
            }

            String resultado = variable.getValue().toString();

            switch (resultado) {
                case "no_viable":
                    noViables++;
                    break;
                case "rechazado":
                    rechazadas++;
                    break;
                case "sin_respuesta":
                    sinRespuesta++;
                    break;
                case "cancelado":
                    canceladas++;
                    break;
                case "finalizado":
                    realizadas++;
                    break;
                default:
                    sinClasificar++;
            }
        }

        int total = instancias.size();

        System.out.println("\n=== RESULTADOS DE SOLICITUDES ===");
        System.out.println("Total de instancias finalizadas: " + total);

        mostrarResultado("No viables", noViables, total);
        mostrarResultado("Rechazadas", rechazadas, total);
        mostrarResultado("Sin respuesta", sinRespuesta, total);
        mostrarResultado("Canceladas", canceladas, total);
        mostrarResultado("Cursos realizados", realizadas, total);
        mostrarResultado("Sin clasificar", sinClasificar, total);
    }


    public void consultarTareasPorEmpleado() {

        List<HistoricTaskInstance> tareas =
                historyService.createHistoricTaskInstanceQuery()
                        .processDefinitionKey(PROCESO)
                        .finished()
                        .list();

        Map<String, Integer> tareasPorEmpleado = new TreeMap<>();

        for (HistoricTaskInstance tarea : tareas) {

            if (tarea.getDeleteReason() != null) {
                continue;
            }

            String empleado = tarea.getAssignee();

            if (empleado != null && !empleado.isBlank()) {
                tareasPorEmpleado.merge(empleado, 1, Integer::sum);
            }
        }

        System.out.println("\n=== TAREAS COMPLETADAS POR EMPLEADO ===");

        if (tareasPorEmpleado.isEmpty()) {
            System.out.println(
                    "No hay tareas completadas del proceso principal."
            );
            return;
        }

        for (Map.Entry<String, Integer> entrada :
                tareasPorEmpleado.entrySet()) {

            System.out.println(
                    "Empleado: " + entrada.getKey()
                    + " | Tareas completadas: " + entrada.getValue()
            );
        }
    }


    public void consultarDuracionPromedioPreparacion() {

        List<HistoricProcessInstance> instancias =
                historyService.createHistoricProcessInstanceQuery()
                        .processDefinitionKey(PROCESO)
                        .list();

        long totalInscripciones = 0;
        long totalMateriales = 0;
        long totalRecursos = 0;

        int cantidadInscripciones = 0;
        int cantidadMateriales = 0;
        int cantidadRecursos = 0;

        for (HistoricProcessInstance instancia : instancias) {

            List<HistoricActivityInstance> actividades =
                    historyService.createHistoricActivityInstanceQuery()
                            .processInstanceId(instancia.getId())
                            .finished()
                            .list();

            Map<String, HistoricActivityInstance> porActividad =
                    new HashMap<>();

            for (HistoricActivityInstance actividad : actividades) {
                porActividad.put(actividad.getActivityId(), actividad);
            }

            // Excluir preparaciones que no terminaron normalmente.
            if (!porActividad.containsKey("sid-85D22149-CBE0-424A-9E0D-9C03CFB275CD")) {
                continue;
            }

            // 1. INSCRIPCIONES: Nomina -> Lista
            HistoricActivityInstance nomina = porActividad.get("Nomina");
            HistoricActivityInstance lista = porActividad.get("Lista");

            if (nomina != null && lista != null
                    && nomina.getStartTime() != null
                    && lista.getEndTime() != null) {

                long duracion = lista.getEndTime().getTime()
                        - nomina.getStartTime().getTime();

                if (duracion >= 0) {
                    totalInscripciones += duracion;
                    cantidadInscripciones++;
                }
            }

            // 2. MATERIALES: inicio y fin de Material
            HistoricActivityInstance material = porActividad.get("Material");

            if (material != null
                    && material.getStartTime() != null
                    && material.getEndTime() != null) {

                long duracion = material.getEndTime().getTime()
                        - material.getStartTime().getTime();

                if (duracion >= 0) {
                    totalMateriales += duracion;
                    cantidadMateriales++;
                }
            }

            // 3. RECURSOS: Evaluar recursos hasta la última reserva.
            HistoricActivityInstance evaluar =
                    porActividad.get("Activity_12i2hpf");

            if (evaluar != null && evaluar.getStartTime() != null) {

                long finRecursos = -1;

                String[] actividadesRecursos = {
                    "Aula", "Virtual", "Equipo"
                };

                for (String id : actividadesRecursos) {

                    HistoricActivityInstance actividad =
                            porActividad.get(id);

                    if (actividad != null && actividad.getEndTime() != null) {

                        long fin = actividad.getEndTime().getTime();

                        if (fin > finRecursos) {
                            finRecursos = fin;
                        }
                    }
                }

                if (finRecursos >= evaluar.getStartTime().getTime()) {

                    long duracion = finRecursos
                            - evaluar.getStartTime().getTime();

                    totalRecursos += duracion;
                    cantidadRecursos++;
                }
            }
        }

        System.out.println("\n=== DURACIÓN PROMEDIO DE PREPARACIÓN ===");

        mostrarPromedio(
                "Inscripciones",
                totalInscripciones,
                cantidadInscripciones
        );

        mostrarPromedio(
                "Materiales",
                totalMateriales,
                cantidadMateriales
        );

        mostrarPromedio(
                "Recursos",
                totalRecursos,
                cantidadRecursos
        );
    }

    private void mostrarPromedio(
            String nombre,
            long total,
            int cantidad) {

        if (cantidad == 0) {
            System.out.println(nombre + ": Sin datos suficientes");
            return;
        }

        double promedioMs = (double) total / cantidad;
        double promedioMinutos = promedioMs / 60000.0;

        System.out.printf(
                "%s: %.2f minutos (%d preparaciones)%n",
                nombre,
                promedioMinutos,
                cantidad
        );
    }



    private void mostrarResultado(String nombre, int cantidad, int total) {

        double porcentaje = total == 0
                ? 0
                : (cantidad * 100.0) / total;

        System.out.printf(
                "%-20s: %d (%.2f%%)%n",
                nombre,
                cantidad,
                porcentaje
        );
    }
}
