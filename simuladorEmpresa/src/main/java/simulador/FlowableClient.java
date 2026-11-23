
package simulador;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
    public int listarEjecuciones(String mensaje) {

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

            System.out.println("Código HTTP: " + respuesta.statusCode());

            if (respuesta.statusCode() != 200) {
                System.out.println("Error al consultar Flowable.");
                System.out.println(respuesta.body());
                return -1;
            }

            String cuerpo = respuesta.body();
            System.out.println(cuerpo);

            // Buscar el valor de "total" en la respuesta JSON
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\"total\"\\s*:\\s*(\\d+)")
                    .matcher(cuerpo);

            if (!matcher.find()) {
                System.out.println("No se pudo interpretar la respuesta de Flowable.");
                return -1;
            }

            return Integer.parseInt(matcher.group(1));

        } catch (IOException e) {
            System.out.println(
                    "No se pudo conectar con Flowable: " + e.getMessage()
            );
            return -1;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("La consulta fue interrumpida.");
            return -1;
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

