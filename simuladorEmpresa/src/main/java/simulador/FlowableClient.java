
package simulador;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public class FlowableClient {

    private static final String BASE_URL =
            "http://localhost:8080/flowable-ui/process-api";
    
    private static final String USUARIO = "admin";
    private static final String PASSWORD = "test";

    private String obtenerAutorizacion() {

        String credenciales = USUARIO + ":" + PASSWORD;

        String codificado = Base64.getEncoder()
                .encodeToString(
                        credenciales.getBytes(StandardCharsets.UTF_8)
                );

        return "Basic " + codificado;
    }

    private final HttpClient cliente = HttpClient.newHttpClient();

    // Este método consulta las instancias activas del proceso
    public void listarInstancias() {

        String url = BASE_URL + "/runtime/process-instances";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", obtenerAutorizacion())
                .GET()
                .build();

        try {
            HttpResponse<String> respuesta = cliente.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Código HTTP: " + respuesta.statusCode());
            System.out.println(respuesta.body());

        } catch (IOException e) {
            System.out.println("No se pudo conectar con Flowable.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("La consulta fue interrumpida.");
        }
    }

    // Este método consulta las ejecuciones que esperan un mensaje.
    
    public List<String> listarEjecuciones(String mensaje) {

        String url = BASE_URL
                + "/runtime/executions"
                + "?messageEventSubscriptionName=" + mensaje;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", obtenerAutorizacion())
                .GET()
                .build();

        try {
            HttpResponse<String> respuesta = cliente.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() != 200) {
                System.out.println("Error HTTP: " + respuesta.statusCode());
                System.out.println(respuesta.body());
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(respuesta.body());

            List<String> ids = new ArrayList<>();
            JsonNode ejecuciones = json.path("data");

            if (!ejecuciones.isArray()) {
                System.out.println("Respuesta inesperada de Flowable.");
                return null;
            }

            for (JsonNode ejecucion : ejecuciones) {
                String id = ejecucion.path("id").asText();
                String actividad = ejecucion.path("activityId").asText();

                if (id.isBlank()) {
                    System.out.println("Una ejecución no tiene ID válido.");
                    return null;
                }

                ids.add(id);

                System.out.println(
                        ids.size() + ". Actividad: " + actividad
                        + " | ID: " + id
                );
            }

            return ids;

        } catch (IOException e) {
            System.out.println(
                    "No se pudo consultar Flowable: " + e.getMessage()
            );
            return null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("La consulta fue interrumpida.");
            return null;
        }
    }


    // Este método envía un mensaje a una ejecución específica.
    public void enviarMensaje(String executionId, String mensaje) {

        String url = BASE_URL + "/runtime/executions/" + executionId;

        String cuerpo = """
                {
                    "action": "messageEventReceived",
                    "messageName": "%s"
                }
                """.formatted(mensaje);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", obtenerAutorizacion())
                .PUT(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();

        try {
            HttpResponse<String> respuesta = cliente.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Código HTTP: " + respuesta.statusCode());

            if (respuesta.statusCode() >= 200
                    && respuesta.statusCode() < 300) {
                System.out.println("Mensaje enviado correctamente.");
            } else {
                System.out.println("Error: " + respuesta.body());
            }

        } catch (IOException e) {
            System.out.println(
                    "No se pudo conectar con Flowable: " + e.getMessage()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Envío interrumpido.");
        }
    }


}

