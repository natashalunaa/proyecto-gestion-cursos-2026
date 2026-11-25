package flowable.report;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.HashMap;
import java.util.Map;

public class SaveReport implements TaskListener {

    @Override
    public void notify(DelegateTask task) {

        String processInstanceId =
                task.getProcessInstanceId();

        String clave =
                System.getenv("FLOWABLE_INTEGRATION_SECRET");

        if (clave == null || clave.isBlank()) {

            throw new IllegalStateException(
                    "Falta FLOWABLE_INTEGRATION_SECRET.");
        }

        Object informe =
                task.getVariable("informeResultados");

        Object adjunto =
                task.getVariable("informeAdjunto");

        try {

            Map<String, Object> datos = new HashMap<>();

            datos.put("processInstanceId", processInstanceId);

            datos.put("informeResultados",
                    informe == null ? "" : informe.toString());

            datos.put("informeAdjunto",
                    Boolean.TRUE.equals(adjunto));

            ObjectMapper mapper = new ObjectMapper();

            String json = mapper.writeValueAsString(datos);

            HttpClient cliente = HttpClient.newHttpClient();

            HttpRequest solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://host.docker.internal:8081"
                            + "/api/interno/cursos/guardar-informe"))
                    .header("Content-Type", "application/json")
                    .header("X-Flowable-Secret", clave)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> respuesta = cliente.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() != 200) {

                throw new IllegalStateException(
                        "No se pudo guardar el informe. HTTP "
                        + respuesta.statusCode()
                        + ": " + respuesta.body());
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "La comunicacion con Spring Boot fue interrumpida.",
                    e);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error al guardar el informe de resultados.",
                    e);
        }
    }
}