package flowable.proposal;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CreateCourseOnAcceptance implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {

        String processInstanceId = execution.getProcessInstanceId();

        String clave = System.getenv("FLOWABLE_INTEGRATION_SECRET");

        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException(
                    "Falta FLOWABLE_INTEGRATION_SECRET en el contenedor Flowable.");
        }

        String json = "{\"processInstanceId\":\""
                + processInstanceId + "\"}";

        try {
            HttpClient cliente = HttpClient.newHttpClient();

            HttpRequest solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://host.docker.internal:8081/api/interno/empresas/habilitar-acceso"))
                    .header("Content-Type", "application/json")
                    .header("X-Flowable-Secret", clave)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> respuesta = cliente.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() != 200) {
                throw new IllegalStateException(
                        "Spring Boot devolvio HTTP "
                                + respuesta.statusCode()
                                + ": " + respuesta.body());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "La comunicacion con Spring Boot fue interrumpida.", e);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al habilitar el acceso de la empresa.", e);
        }
    }
}
