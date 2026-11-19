
package curso.flowable;

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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CursoController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient cliente = HttpClient.newHttpClient();

    @Value("${flowable.rest.url:http://localhost:8080/flowable-ui/process-api}")
    private String flowableUrl;

    @Value("${FLOWABLE_USER:admin}")
    private String flowableUser;

    @Value("${FLOWABLE_PASSWORD}")
    private String flowablePassword;

    public CursoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/cursos/saludo")
    public String saludo() {
        return "API de Gestion de Cursos funcionando correctamente";
    }

    @PostMapping("/api/cursos/iniciar")
    public ResponseEntity<?> iniciarCurso(@RequestBody SolicitudCurso solicitud) {

        if (solicitud.empresaId() == null
                || solicitud.tematica() == null
                || solicitud.tematica().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Debe indicar empresaId y tematica.");
        }

        Integer cantidadEmpresas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM empresas WHERE id = ?",
                Integer.class,
                solicitud.empresaId()
        );

        if (cantidadEmpresas == null || cantidadEmpresas == 0) {
            return ResponseEntity.badRequest()
                    .body("La empresa indicada no existe.");
        }

        String processInstanceId = null;

        try {
            String autenticacion = Base64.getEncoder().encodeToString(
                    (flowableUser + ":" + flowablePassword)
                            .getBytes(StandardCharsets.UTF_8)
            );

            
            Map<String, Object> datos = Map.of(
                    "message", "Solicitud de curso"
            );

            String json = objectMapper.writeValueAsString(datos);


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(flowableUrl + "/runtime/process-instances"))
                    .header("Authorization", "Basic " + autenticacion)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> respuesta = cliente.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                return ResponseEntity.status(502)
                        .body("Flowable no pudo iniciar el proceso: "
                                + respuesta.body());
            }

            JsonNode resultado = objectMapper.readTree(respuesta.body());
            processInstanceId = resultado.path("id").asText(null);

            if (processInstanceId == null || processInstanceId.isBlank()) {
                return ResponseEntity.status(502)
                        .body("Flowable no devolvio el ID de la instancia.");
            }

            
            jdbcTemplate.update(
                    "INSERT INTO cursos "
                    + "(empresa_id, process_instance_id, tematica, "
                    + "descripcion, cantidad_participantes, "
                    + "fechas_preferidas, estado) "
                    + "VALUES (?, ?, ?, ?, ?, ?, 'en_evaluacion')",
                    solicitud.empresaId(),
                    processInstanceId,
                    solicitud.tematica(),
                    solicitud.descripcion(),
                    solicitud.cantidadParticipantes(),
                    solicitud.fechasPreferidas()
            );


            return ResponseEntity.ok(Map.of(
                    "mensaje", "Solicitud de curso registrada correctamente",
                    "processInstanceId", processInstanceId
            ));

        } catch (Exception e) {
            // No se elimina automáticamente la instancia: si la inserción
            // falló, hay que revisar su estado antes de reintentar.
            return ResponseEntity.internalServerError()
                    .body("Error al iniciar o registrar el curso: "
                            + e.getMessage()
                            + (processInstanceId == null
                                ? ""
                                : ". Instancia creada: " + processInstanceId));
        }
    }

   public static class SolicitudCurso {

    private Integer empresaId;
    private String tematica;
    private String descripcion;
    private Integer cantidadParticipantes;
    private String fechasPreferidas;

    public Integer empresaId() {
        return empresaId;
    }

    public String tematica() {
        return tematica;
    }

    public String descripcion() {
        return descripcion;
    }

    public Integer cantidadParticipantes() {
        return cantidadParticipantes;
    }

    public String fechasPreferidas() {
        return fechasPreferidas;
    }

    public void setEmpresaId(Integer empresaId) {
        this.empresaId = empresaId;
    }

    public void setTematica(String tematica) {
        this.tematica = tematica;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setCantidadParticipantes(Integer cantidadParticipantes) {
        this.cantidadParticipantes = cantidadParticipantes;
    }

    public void setFechasPreferidas(String fechasPreferidas) {
        this.fechasPreferidas = fechasPreferidas;
    }
}
}
