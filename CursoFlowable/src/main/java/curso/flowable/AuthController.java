package curso.flowable;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private String flowableUrl = "http://localhost:8080/flowable-ui/process-api";
    private final HttpClient client = HttpClient.newHttpClient();

    public static class Credenciales {
        private String usuario;
        private String contrasena;

        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getContrasena() { return contrasena; }
        public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credenciales datos, HttpServletRequest servletRequest) {
        if (datos == null || datos.getUsuario() == null || datos.getUsuario().isBlank()
                || datos.getContrasena() == null || datos.getContrasena().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ingresá usuario y contraseña."));
        }
        try {
            String token = Base64.getEncoder().encodeToString(
                    (datos.getUsuario() + ":" + datos.getContrasena()).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(flowableUrl + "/runtime/tasks?size=1"))
                    .header("Authorization", "Basic " + token)
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return ResponseEntity.status(401).body(Map.of("error", "Credenciales incorrectas o sin acceso."));
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ResponseEntity.status(502).body(Map.of("error", "Flowable no pudo validar el acceso."));
            }
            HttpSession anterior = servletRequest.getSession(false);
            if (anterior != null) anterior.invalidate();
            HttpSession session = servletRequest.getSession(true);
            session.setAttribute("usuarioFlowable", datos.getUsuario());
            return ResponseEntity.ok(Map.of("usuario", datos.getUsuario()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo conectar con Flowable."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioFlowable") == null)
            return ResponseEntity.status(401).body(Map.of("error", "No hay sesión activa."));
        return ResponseEntity.ok(Map.of("usuario", session.getAttribute("usuarioFlowable")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
