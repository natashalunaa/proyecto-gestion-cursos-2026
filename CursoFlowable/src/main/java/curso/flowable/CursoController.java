
package curso.flowable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CursoController {

    @GetMapping("/api/cursos/saludo")
    public String saludo() {
        return "API de Gestion de Cursos funcionando correctamente";
    }

    @PostMapping("/api/cursos/iniciar")
    public ResponseEntity<String> iniciarCurso() {
        try {
            String url = "http://localhost:8080/flowable-ui/process-api/runtime/process-instances";

            String credenciales = "admin:test";
            String autenticacion = Base64.getEncoder()
                    .encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));

           String json = "{\"processDefinitionKey\":\"Process_Curso\"}";

            HttpRequest solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + autenticacion)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient cliente = HttpClient.newHttpClient();

            HttpResponse<String> respuesta = cliente.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString()
            );

            return ResponseEntity.status(respuesta.statusCode())
                    .body(respuesta.body());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al comunicarse con Flowable: " + e.getMessage());
        }
    }
}
