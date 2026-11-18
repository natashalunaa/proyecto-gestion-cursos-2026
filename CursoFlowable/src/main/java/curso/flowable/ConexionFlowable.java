package curso.flowable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ConexionFlowable {

    public static void main(String[] args) {
        String url = "http://localhost:8080/flowable-ui/process-api/management/engine";

        String usuario = "admin";
        String contrasena = "test";

        String credenciales = usuario + ":" + contrasena;
        String autenticacion = Base64.getEncoder()
                .encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));

        try {
            HttpClient cliente = HttpClient.newHttpClient();

            HttpRequest solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + autenticacion)
                    .GET()
                    .build();

            HttpResponse<String> respuesta = cliente.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Codigo HTTP: " + respuesta.statusCode());
            System.out.println("Respuesta de Flowable:");
            System.out.println(respuesta.body());

        } catch (Exception e) {
            System.out.println("Error al conectar con Flowable:");
            e.printStackTrace();
        }
    }
}
