package curso.flowable;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalcularCursosActivosTest {

    @Test
    public void probarCalculoCursosActivos() {

        // Crear un motor Flowable con base de datos H2
        ProcessEngine motor = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:pruebaCursos;DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate("true")
                .buildProcessEngine();

        try {

            // Proceso BPMN mínimo para probar la Service Task
            String bpmn =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                    "xmlns:flowable=\"http://flowable.org/bpmn\" " +
                    "targetNamespace=\"http://www.flowable.org/processdef\">" +
                    "<process id=\"pruebaCalculo\" isExecutable=\"true\">" +
                    "<startEvent id=\"inicio\"/>" +
                    "<sequenceFlow id=\"flujo1\" sourceRef=\"inicio\" targetRef=\"calcular\"/>" +
                    "<serviceTask id=\"calcular\" " +
                    "flowable:class=\"curso.flowable.CalcularCursosActivos\"/>" +
                    "<sequenceFlow id=\"flujo2\" sourceRef=\"calcular\" targetRef=\"esperar\"/>" +
                    "<userTask id=\"esperar\" name=\"Revisar resultado\"/>" +
                    "</process></definitions>";

            // Desplegar el proceso de prueba
            motor.getRepositoryService()
                    .createDeployment()
                    .addString("pruebaCalculo.bpmn20.xml", bpmn)
                    .deploy();

            // Iniciar el proceso
            RuntimeService runtime = motor.getRuntimeService();

            ProcessInstance instancia = runtime
                    .startProcessInstanceByKey("pruebaCalculo");

            // Obtener la variable guardada por la Service Task
            Object resultado = runtime.getVariable(
                    instancia.getId(),
                    "cantidadCursosActivos"
            );

            // Comprobar que el valor sea 0
            assertEquals(0, resultado);

            System.out.println(
                    "PRUEBA EXITOSA: cantidadCursosActivos = " + resultado
            );

        } finally {
            motor.close();
        }
    }
}
