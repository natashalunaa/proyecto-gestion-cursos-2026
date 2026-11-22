
package curso.flowable.service;

import curso.flowable.dto.SolicitudCurso;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class FlowableService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient cliente = HttpClient.newHttpClient();

    @Value("${flowable.rest.url:http://localhost:8080/flowable-ui/process-api}")
    private String flowableUrl;

    @Value("${FLOWABLE_USER:admin}")
    private String flowableUser;

    @Value("${FLOWABLE_PASSWORD:test}")
    private String flowablePassword;

    private String autenticacion() {
        return "Basic " + Base64.getEncoder().encodeToString(
                (flowableUser + ":" + flowablePassword)
                        .getBytes(StandardCharsets.UTF_8));
    }

    private String urlTarea(String tareaId) {
        return flowableUrl + "/runtime/tasks/"
                + URLEncoder.encode(tareaId, StandardCharsets.UTF_8);
    }

    public String iniciarProceso(SolicitudCurso solicitud) throws Exception {

        Map<String, Object> datos = Map.of(
                "processDefinitionKey", "process-proyecto-curso",
                "variables", List.of(
                        Map.of(
                                "name", "tematica",
                                "value", solicitud.tematica(),
                                "type", "string"),
                        Map.of(
                                "name", "descripcion",
                                "value", solicitud.descripcion(),
                                "type", "string"),
                        Map.of(
                                "name", "cantidadParticipantes",
                                "value", solicitud.cantidadParticipantes(),
                                "type", "integer"),
                        Map.of(
                                "name", "fechasPreferidas",
                                "value", solicitud.fechasPreferidas(),
                                "type", "string")
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(flowableUrl + "/runtime/process-instances"))
                .header("Authorization", autenticacion())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(datos)))
                .build();

        HttpResponse<String> respuesta = cliente.send(
                request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Flowable respondio.");
        System.out.println("Codigo HTTP: " + respuesta.statusCode());
        System.out.println("Respuesta: " + respuesta.body());

        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Flowable no pudo iniciar el proceso: " + respuesta.body());
        }

        JsonNode resultado = objectMapper.readTree(respuesta.body());
        String processInstanceId = resultado.path("id").asText(null);

        if (processInstanceId == null || processInstanceId.isBlank()) {
            throw new IllegalStateException(
                    "Flowable no devolvio el ID de la instancia.");
        }

        return processInstanceId;
    }

    public JsonNode obtenerTareasUsuario(String usuario) throws Exception {

        String url = flowableUrl + "/runtime/tasks?assignee="
                + URLEncoder.encode(usuario, StandardCharsets.UTF_8)
                + "&size=100";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", autenticacion())
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Error al consultar tareas de Flowable. HTTP "
                            + respuesta.statusCode());
        }

        return objectMapper.readTree(respuesta.body());
    }

    public JsonNode obtenerTareasCandidatas(String usuario) throws Exception {

        String url = flowableUrl + "/runtime/tasks?candidateUser="
                + URLEncoder.encode(usuario, StandardCharsets.UTF_8)
                + "&size=100";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", autenticacion())
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Error al consultar tareas candidatas. HTTP "
                            + respuesta.statusCode());
        }

        return objectMapper.readTree(respuesta.body());
    }

    public JsonNode obtenerTareaPorId(String tareaId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlTarea(tareaId)))
                .header("Authorization", autenticacion())
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 404) {
            return null;
        }

        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Error al consultar la tarea. HTTP "
                            + respuesta.statusCode()
                            + ": " + respuesta.body());
        }

        return objectMapper.readTree(respuesta.body());
    }

    // Completar una tarea normal, sin variables adicionales.
    public void completarTarea(String tareaId) throws Exception {

        Map<String, Object> datos = Map.of("action", "complete");
        enviarCompletarTarea(tareaId, datos);
    }

    // Completar la propuesta enviando sus variables a Flowable.
    public void completarTareaConPropuesta(
            String tareaId,
            String programa,
            BigDecimal presupuesto,
            LocalDate fechaInicio,
            LocalDate fechaFin) throws Exception {

        Map<String, Object> datos = Map.of(
                "action", "complete",
                "variables", List.of(
                        Map.of(
                                "name", "programa",
                                "value", programa,
                                "type", "string"),
                        Map.of(
                                "name", "presupuesto",
                                "value", presupuesto.longValueExact(),
                                "type", "long"),
                        Map.of(
                                "name", "fechaInicioPosible",
                                "value", fechaInicio.toString(),
                                "type", "string"),
                        Map.of(
                                "name", "fechaFinPropuesta",
                                "value", fechaFin.toString(),
                                "type", "string")
                )
        );

        enviarCompletarTarea(tareaId, datos);
    }

    // Método compartido para enviar la orden a Flowable.
    private void enviarCompletarTarea(
            String tareaId,
            Map<String, Object> datos) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlTarea(tareaId)))
                .header("Authorization", autenticacion())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(datos)))
                .build();

        HttpResponse<String> respuesta = cliente.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            throw new IllegalStateException(
                    "No se pudo completar la tarea. HTTP "
                            + respuesta.statusCode()
                            + ": " + respuesta.body());
        }
    }
}
