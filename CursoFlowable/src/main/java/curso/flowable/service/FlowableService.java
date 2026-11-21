package curso.flowable.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import curso.flowable.dto.SolicitudCurso;

@Service
public class FlowableService {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private final HttpClient cliente = HttpClient.newHttpClient();

        @Value("${flowable.rest.url:http://localhost:8080/flowable-ui/process-api}")
        private String flowableUrl;

        @Value("${FLOWABLE_USER:admin}")
        private String flowableUser;

       @Value("${FLOWABLE_PASSWORD:admin}")
        private String flowablePassword;

        public String iniciarProceso(SolicitudCurso solicitud) throws Exception {

                String autenticacion = Base64.getEncoder().encodeToString(
                                (flowableUser + ":" + flowablePassword)
                                                .getBytes(StandardCharsets.UTF_8));

                Map<String, Object> datos = Map.of(
                                "processDefinitionKey", "process-proyecto-curso",
                                "variables", java.util.List.of(
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
                                                                "type", "string")));

                String json = objectMapper.writeValueAsString(datos);

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(flowableUrl + "/runtime/process-instances"))
                                .header("Authorization", "Basic " + autenticacion)
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(json))
                                .build();

                System.out.println("Enviando solicitud a Flowable...");

                HttpResponse<String> respuesta = cliente.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

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
}
