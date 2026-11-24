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
import curso.flowable.repository.EmpresaRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private String flowableUrl = "http://localhost:8080/flowable-ui/process-api";
    private final HttpClient client = HttpClient.newHttpClient();

    private final EmpresaRepository empresaRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public static class Credenciales {
        private String usuario;
        private String contrasena;

        public String getUsuario() {
            return usuario;
        }

        public void setUsuario(String usuario) {
            this.usuario = usuario;
        }

        public String getContrasena() {
            return contrasena;
        }

        public void setContrasena(String contrasena) {
            this.contrasena = contrasena;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credenciales datos, HttpServletRequest servletRequest) {
        if (datos == null || datos.getUsuario() == null || datos.getUsuario().isBlank()
                || datos.getContrasena() == null || datos.getContrasena().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ingresá usuario y contraseña."));
        }

        // Primero comprobar si el usuario pertenece a una empresa
        try {
            Map<String, Object> empresa = empresaRepository.buscarPorUsuarioAcceso(datos.getUsuario());

            if (empresa != null) {

                String passwordHash = (String) empresa.get("password_acceso");

                // Verificar la contraseña con BCrypt
                if (passwordHash == null ||
                        !passwordEncoder.matches(datos.getContrasena(), passwordHash)) {

                    return ResponseEntity.status(401)
                            .body(Map.of("error", "Usuario o contraseña incorrectos."));
                }

                // Evitar reutilizar una sesión anterior
                HttpSession anterior = servletRequest.getSession(false);

                if (anterior != null) {
                    anterior.invalidate();
                }

                // Crear la sesión de la empresa
                HttpSession session = servletRequest.getSession(true);

                session.setAttribute(
                        "empresaId",
                        ((Number) empresa.get("id")).intValue());

                session.setAttribute("tipoUsuario", "empresa");

                return ResponseEntity.ok(Map.of(
                        "usuario", datos.getUsuario(),
                        "tipoUsuario", "empresa"));
            }

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",
                            "No se pudo verificar el acceso de la empresa."));
        }

        // Si no es una empresa, continúa el login existente de Flowable
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
            if (anterior != null)
                anterior.invalidate();
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

        if (session == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "No hay sesión activa."));
        }

        // Sesión de una empresa
        Integer empresaId = (Integer) session.getAttribute("empresaId");

        if (empresaId != null) {
            return ResponseEntity.ok(Map.of(
                    "tipoUsuario", "empresa",
                    "empresaId", empresaId));
        }

        // Sesión de un usuario interno de Flowable
        String usuario = (String) session.getAttribute("usuarioFlowable");

        if (usuario != null) {
            return ResponseEntity.ok(Map.of(
                    "usuario", usuario,
                    "tipoUsuario", "flowable"));
        }

        return ResponseEntity.status(401)
                .body(Map.of("error", "No hay sesión activa."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
