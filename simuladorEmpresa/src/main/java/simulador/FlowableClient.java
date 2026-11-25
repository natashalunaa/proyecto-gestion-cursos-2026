package simulador;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FlowableClient {

    private static final String BASE_URL = "http://localhost:8080/flowable-ui/process-api";

    private static final String USUARIO = "admin";
    private static final String PASSWORD = "test";

    private final HttpClient cliente = HttpClient.newHttpClient();

    private final ObjectMapper mapper = new ObjectMapper();

    private String obtenerAutorizacion() {

        String credenciales = USUARIO + ":" + PASSWORD;

        String codificado = Base64.getEncoder()
                .encodeToString(
                        credenciales.getBytes(StandardCharsets.UTF_8));

        return "Basic " + codificado;
    }

    // Consulta las instancias activas del proceso.
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
                    HttpResponse.BodyHandlers.ofString());

            System.out.println(
                    "Código HTTP: " + respuesta.statusCode());

            System.out.println(respuesta.body());

        } catch (IOException e) {

            System.out.println(
                    "No se pudo conectar con Flowable.");

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.out.println("La consulta fue interrumpida.");
        }
    }

    // Consulta las ejecuciones que esperan un mensaje.

    public List<String> listarEjecuciones(String mensaje) {

        String url = BASE_URL
                + "/runtime/executions"
                + "?messageEventSubscriptionName=" + mensaje
                + "&size=100";

        JsonNode json = consultarJson(url);

        if (json == null) {
            return null;
        }

        List<String> ids = new ArrayList<>();

        JsonNode ejecuciones = json.path("data");

        if (!ejecuciones.isArray()) {

            System.out.println(
                    "Respuesta inesperada de Flowable.");

            return null;
        }

        for (JsonNode ejecucion : ejecuciones) {

            String id = ejecucion.path("id").asText("");

            String processInstanceId = ejecucion
                    .path("processInstanceId")
                    .asText("");

            if (id.isBlank()) {

                System.out.println(
                        "Una ejecución no tiene ID válido.");

                return null;
            }

            if (processInstanceId.isBlank()) {

                processInstanceId = obtenerInstanciaDeEjecucion(id);
            }

            String datosCurso = "Empresa no identificada";

            if (processInstanceId != null
                    && !processInstanceId.isBlank()) {

                datosCurso = obtenerNombreEmpresaYCurso(processInstanceId);
            }

            ids.add(id);

            System.out.println(
                    ids.size() + ". " + datosCurso
                            + " | ID: " + id);
        }

        return ids;
    }

    // Obtiene la instancia de proceso de una ejecución.
    public String obtenerInstanciaDeEjecucion(
            String executionId) {

        String url = BASE_URL
                + "/runtime/executions/"
                + executionId;

        JsonNode ejecucion = consultarJson(url);

        if (ejecucion == null) {
            return null;
        }

        String processInstanceId = ejecucion
                .path("processInstanceId")
                .asText("");

        if (processInstanceId.isBlank()) {
            return null;
        }

        return processInstanceId;
    }

    // Consulta las variables de una instancia del proceso.
    public JsonNode obtenerVariablesProceso(
            String processInstanceId) {

        String url = BASE_URL
                + "/runtime/process-instances/"
                + processInstanceId
                + "/variables";

        return consultarJson(url);
    }

    // Busca el valor de una variable por su nombre.
    public String obtenerValorVariable(
            JsonNode variables,
            String nombre) {

        if (variables == null) {
            return null;
        }

        JsonNode lista = variables.isArray()
                ? variables
                : variables.path("data");

        if (!lista.isArray()) {
            return null;
        }

        for (JsonNode variable : lista) {

            if (nombre.equals(
                    variable.path("name").asText())) {

                JsonNode valor = variable.get("value");

                if (valor == null || valor.isNull()) {
                    return null;
                }

                return valor.asText();
            }
        }

        return null;
    }

    // Busca una ejecución que espera un mensaje
    // dentro de una instancia específica.
    public String buscarEjecucionPorInstancia(
            String mensaje,
            String processInstanceId) {

        String url = BASE_URL
                + "/runtime/executions"
                + "?messageEventSubscriptionName=" + mensaje
                + "&processInstanceId=" + processInstanceId
                + "&size=100";

        JsonNode json = consultarJson(url);

        if (json == null) {
            return null;
        }

        JsonNode ejecuciones = json.path("data");

        if (!ejecuciones.isArray()) {
            return null;
        }

        for (JsonNode ejecucion : ejecuciones) {

            String instancia = ejecucion
                    .path("processInstanceId")
                    .asText("");

            if (processInstanceId.equals(instancia)) {

                String id = ejecucion.path("id").asText("");

                if (!id.isBlank()) {
                    return id;
                }
            }
        }

        return null;
    }

    // Envía un mensaje sin variables adicionales.
    public void enviarMensaje(
            String executionId,
            String mensaje) {

        Map<String, Object> datos = Map.of(
                "action", "messageEventReceived",
                "messageName", mensaje);

        enviarSolicitudMensaje(executionId, datos);
    }

    // Envía la aceptación junto con la fecha acordada.
    public boolean enviarMensajeConFecha(
            String executionId,
            String mensaje,
            String fechaCurso) {

        Map<String, Object> variableFecha = Map.of(
                "name", "fechaCurso",
                "type", "string",
                "value", fechaCurso);

        Map<String, Object> datos = Map.of(
                "action", "messageEventReceived",
                "messageName", mensaje,
                "variables", List.of(variableFecha));

        return enviarSolicitudMensaje(executionId, datos);
    }

    // Método común para enviar mensajes a Flowable.
    private boolean enviarSolicitudMensaje(
            String executionId,
            Map<String, Object> datos) {

        String url = BASE_URL
                + "/runtime/executions/"
                + executionId;

        try {

            String cuerpo = mapper.writeValueAsString(datos);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", obtenerAutorizacion())
                    .PUT(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();

            HttpResponse<String> respuesta = cliente.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println(
                    "Código HTTP: " + respuesta.statusCode());

            if (respuesta.statusCode() >= 200
                    && respuesta.statusCode() < 300) {

                System.out.println(
                        "Mensaje enviado correctamente.");

                return true;

            } else {

                System.out.println(
                        "Error: " + respuesta.body());

                return false;
            }

        } catch (IOException e) {

            System.out.println(
                    "No se pudo conectar con Flowable: "
                            + e.getMessage());

            return false;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.out.println("Envío interrumpido.");

            return false;
        }
    }

    // Método común para realizar consultas GET.
    private JsonNode consultarJson(String url) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", obtenerAutorizacion())
                .header("Accept", "application/json")
                .GET()
                .build();

        try {

            HttpResponse<String> respuesta = cliente.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() != 200) {

                System.out.println(
                        "Error HTTP: " + respuesta.statusCode());

                System.out.println(respuesta.body());

                return null;
            }

            return mapper.readTree(respuesta.body());

        } catch (IOException e) {

            System.out.println(
                    "No se pudo consultar Flowable: "
                            + e.getMessage());

            return null;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.out.println(
                    "La consulta fue interrumpida.");

            return null;
        }
    }

    private String obtenerNombreEmpresaYCurso(String processInstanceId) {

        String url = "jdbc:postgresql://localhost:5433/institucion_cursos";
        String usuario = "flowable";
        String password = System.getenv("DB_PASSWORD");

        String sql = """
                SELECT e.razon_social, c.tematica
                FROM cursos c
                JOIN empresas e ON c.empresa_id = e.id
                WHERE c.process_instance_id = ?
                """;

        try (Connection conexion = DriverManager.getConnection(url, usuario, password);

                PreparedStatement consulta = conexion.prepareStatement(sql)) {

            consulta.setString(1, processInstanceId);

            try (ResultSet resultado = consulta.executeQuery()) {

                if (resultado.next()) {

                    return resultado.getString("razon_social")
                            + " | "
                            + resultado.getString("tematica");
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "No se pudieron consultar los datos del curso: "
                            + e.getMessage());
        }

        return "Empresa no identificada";
    }
}