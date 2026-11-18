
package curso.flowable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class AsignarAdministrativoListenerTest {

    @Test
    void asignaAlAdministrativoConMenosTareas() {

        // Creamos un motor Flowable temporal para esta prueba.
        ProcessEngine engine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setJdbcUrl("jdbc:h2:mem:pruebaAsignacion;DB_CLOSE_DELAY=-1")
                .buildProcessEngine();

        try {
            IdentityService identityService = engine.getIdentityService();
            RepositoryService repositoryService = engine.getRepositoryService();
            RuntimeService runtimeService = engine.getRuntimeService();
            TaskService taskService = engine.getTaskService();

            // Creamos el grupo Administracion.
            org.flowable.idm.api.Group grupo = identityService.newGroup("Administracion");
            grupo.setName("Administracion");
            identityService.saveGroup(grupo);

            // Creamos dos administrativos.
            for (String id : new String[] {"admin1", "admin2"}) {
                org.flowable.idm.api.User usuario = identityService.newUser(id);
                identityService.saveUser(usuario);
                identityService.createMembership(id, "Administracion");
            }

            // Proceso para generar tareas pendientes de prueba.
            String procesoPendientes =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
                    + "targetNamespace=\"http://ejemplo.com\">"
                    + "<process id=\"procesoPendientes\" isExecutable=\"true\">"
                    + "<startEvent id=\"inicio\"/>"
                    + "<sequenceFlow id=\"f1\" sourceRef=\"inicio\" targetRef=\"tarea\"/>"
                    + "<userTask id=\"tarea\" name=\"Tarea pendiente\"/>"
                    + "<sequenceFlow id=\"f2\" sourceRef=\"tarea\" targetRef=\"fin\"/>"
                    + "<endEvent id=\"fin\"/>"
                    + "</process></definitions>";

            // Proceso que utiliza nuestro listener.
            String procesoAsignacion =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
                    + "xmlns:flowable=\"http://flowable.org/bpmn\" "
                    + "targetNamespace=\"http://ejemplo.com\">"
                    + "<process id=\"procesoAsignacion\" isExecutable=\"true\">"
                    + "<startEvent id=\"inicio\"/>"
                    + "<sequenceFlow id=\"f1\" sourceRef=\"inicio\" targetRef=\"lista\"/>"
                    + "<userTask id=\"lista\" name=\"Confeccionar lista definitiva\">"
                    + "<extensionElements>"
                    + "<flowable:taskListener event=\"create\" "
                    + "class=\"curso.flowable.AsignarAdministrativoListener\"/>"
                    + "</extensionElements>"
                    + "</userTask>"
                    + "<sequenceFlow id=\"f2\" sourceRef=\"lista\" targetRef=\"fin\"/>"
                    + "<endEvent id=\"fin\"/>"
                    + "</process></definitions>";

            Deployment deployment = repositoryService.createDeployment()
                    .addString("pendientes.bpmn20.xml", procesoPendientes)
                    .addString("asignacion.bpmn20.xml", procesoAsignacion)
                    .deploy();

            // admin1 tendrá dos tareas pendientes.
            for (int i = 0; i < 2; i++) {
                ProcessInstance instancia =
                        runtimeService.startProcessInstanceByKey("procesoPendientes");

                Task tarea = taskService.createTaskQuery()
                        .processInstanceId(instancia.getId())
                        .singleResult();

                taskService.setAssignee(tarea.getId(), "admin1");
            }

            // admin2 tendrá una sola tarea pendiente.
            ProcessInstance instancia =
                    runtimeService.startProcessInstanceByKey("procesoPendientes");

            Task tarea = taskService.createTaskQuery()
                    .processInstanceId(instancia.getId())
                    .singleResult();

            taskService.setAssignee(tarea.getId(), "admin2");

            // Iniciamos el proceso que ejecuta nuestro listener.
            ProcessInstance proceso =
                    runtimeService.startProcessInstanceByKey("procesoAsignacion");

            Task tareaAsignada = taskService.createTaskQuery()
                    .processInstanceId(proceso.getId())
                    .singleResult();

            assertNotNull(tareaAsignada);
            assertEquals("admin2", tareaAsignada.getAssignee());

            System.out.println("PRUEBA EXITOSA: tarea asignada a admin2");

        } finally {
            engine.close();
        }
    }
}

