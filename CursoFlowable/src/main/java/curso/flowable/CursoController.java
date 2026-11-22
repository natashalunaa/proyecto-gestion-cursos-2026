
package curso.flowable;

import curso.flowable.dto.SolicitudCurso;
import curso.flowable.repository.EmpresaRepository;
import curso.flowable.service.CursoService;
import curso.flowable.service.FlowableService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class CursoController {

    private final EmpresaRepository empresaRepository;
    private final CursoService cursoService;
    private final FlowableService flowableService;

    public CursoController(
            EmpresaRepository empresaRepository,
            CursoService cursoService,
            FlowableService flowableService) {

        this.empresaRepository = empresaRepository;
        this.cursoService = cursoService;
        this.flowableService = flowableService;
    }

    @GetMapping("/api/cursos/saludo")
    public String saludo() {
        return "API de Gestion de Cursos funcionando correctamente";
    }

    @GetMapping("/api/cursos/probar-bdd")
    public ResponseEntity<?> probarBaseDeDatos() {
        try {
            Integer cantidadEmpresas = empresaRepository.contarEmpresas();

            return ResponseEntity.ok(
                    "Conexion con PostgreSQL correcta. Empresas registradas: "
                            + cantidadEmpresas);

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body("No se pudo consultar PostgreSQL.");
        }
    }

    // Registrar una solicitud e iniciar el proceso BPMN.
    @PostMapping("/api/cursos/iniciar")
    public ResponseEntity<?> iniciarCurso(
            @RequestBody SolicitudCurso solicitud) {

        if (solicitud.getCuit() == null
                || solicitud.getCuit().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Debe indicar el CUIT de la empresa.");
        }

        if (solicitud.tematica() == null
                || solicitud.tematica().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Debe indicar la tematica del curso.");
        }

        Integer empresaId = cursoService.buscarEmpresaPorCuit(
                solicitud.getCuit());

        if (empresaId == null) {

            if (solicitud.getEmpresa() == null
                    || solicitud.getEmpresa().isBlank()
                    || solicitud.getContacto() == null
                    || solicitud.getContacto().isBlank()
                    || solicitud.getEmail() == null
                    || solicitud.getEmail().isBlank()) {

                return ResponseEntity.badRequest()
                        .body("Debe completar los datos de la empresa.");
            }

            try {
                empresaId = cursoService.registrarEmpresa(
                        solicitud.getCuit(),
                        solicitud.getEmpresa(),
                        solicitud.getContacto(),
                        solicitud.getEmail(),
                        solicitud.getTelefono());

            } catch (Exception e) {
                e.printStackTrace();

                return ResponseEntity.internalServerError()
                        .body("No se pudo registrar la empresa.");
            }
        }

        solicitud.setEmpresaId(empresaId);

        try {
            String processInstanceId =
                    cursoService.registrarSolicitud(solicitud);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Solicitud de curso registrada correctamente",
                    "processInstanceId", processInstanceId));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body("Error al iniciar o registrar el curso.");
        }
    }

    @GetMapping("/api/cursos/{id}")
    public ResponseEntity<?> obtenerCurso(
            @PathVariable("id") Integer id) {

        Map<String, Object> curso = cursoService.buscarCursoPorId(id);

        if (curso == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(curso);
    }

    // Consultar tareas asignadas y candidatas del usuario.
    @GetMapping("/api/tareas/mis-tareas")
    public ResponseEntity<?> obtenerMisTareas(HttpSession sesion) {

        String usuario =
                (String) sesion.getAttribute("usuarioFlowable");

        if (usuario == null || usuario.isBlank()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Debes iniciar sesion."));
        }

        try {
            JsonNode asignadas =
                    flowableService.obtenerTareasUsuario(usuario);

            JsonNode candidatas =
                    flowableService.obtenerTareasCandidatas(usuario);

            Map<String, JsonNode> tareasUnicas =
                    new LinkedHashMap<>();

            for (JsonNode tarea : asignadas.path("data")) {
                if (tarea.hasNonNull("id")) {
                    tareasUnicas.put(tarea.get("id").asText(), tarea);
                }
            }

            for (JsonNode tarea : candidatas.path("data")) {
                if (tarea.hasNonNull("id")
                        && tarea.path("assignee").isNull()) {
                    tareasUnicas.putIfAbsent(
                            tarea.get("id").asText(), tarea);
                }
            }

            ObjectNode resultado =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .createObjectNode();

            resultado.put("usuario", usuario);

            ArrayNode lista = resultado.putArray("data");
            tareasUnicas.values().forEach(lista::add);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",
                            "No se pudieron consultar las tareas de Flowable."));
        }
    }

    @GetMapping("/api/cursos/instancia/{processInstanceId}")
    public ResponseEntity<?> obtenerCursoPorInstancia(
            @PathVariable("processInstanceId")
            String processInstanceId) {

        Map<String, Object> curso =
                cursoService.buscarCursoPorProcessInstanceId(
                        processInstanceId);

        if (curso == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(curso);
    }

    // Completar una tarea normal.
    @PostMapping("/api/tareas/{tareaId}/completar")
    public ResponseEntity<?> completarTarea(
            @PathVariable("tareaId") String tareaId,
            HttpSession sesion) {

        String usuario =
                (String) sesion.getAttribute("usuarioFlowable");

        if (usuario == null || usuario.isBlank()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Debes iniciar sesion."));
        }

        try {
            JsonNode tarea =
                    flowableService.obtenerTareaPorId(tareaId);

            if (tarea == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "La tarea no existe."));
            }

            String responsable =
                    tarea.path("assignee").asText("");

            if (!usuario.equals(responsable)) {
                return ResponseEntity.status(403)
                        .body(Map.of(
                                "error",
                                "No tienes permiso para completar esta tarea."));
            }

            // La propuesta se completa por su endpoint específico.
            if ("Registrar y Elaborar propuesta"
                    .equals(tarea.path("name").asText(""))) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "La propuesta debe completarse desde su formulario."));
            }

            flowableService.completarTarea(tareaId);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Tarea completada correctamente."));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",
                            "No se pudo completar la tarea."));
        }
    }

    // Guardar la propuesta en PostgreSQL sin completar la tarea.
    @PostMapping("/api/cursos/instancia/{processInstanceId}/propuesta")
    public ResponseEntity<?> guardarPropuesta(
            @PathVariable("processInstanceId")
            String processInstanceId,
            @RequestBody Map<String, Object> datos,
            HttpSession sesion) {

        String usuario =
                (String) sesion.getAttribute("usuarioFlowable");

        if (usuario == null || usuario.isBlank()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Debes iniciar sesion."));
        }

        if (!"jefe_admin".equals(usuario)) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "error",
                            "No tienes permiso para elaborar propuestas."));
        }

        try {
            String programa =
                    String.valueOf(datos.getOrDefault("programa", "")).trim();

            if (programa.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "El programa es obligatorio."));
            }

            BigDecimal presupuesto = new BigDecimal(
                    String.valueOf(datos.get("presupuesto")));

            if (presupuesto.signum() < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "El presupuesto no puede ser negativo."));
            }

            LocalDate fechaInicio = LocalDate.parse(
                    String.valueOf(datos.get("fechaInicioPosible")));

            LocalDate fechaFin = LocalDate.parse(
                    String.valueOf(datos.get("fechaFinPropuesta")));

            if (fechaFin.isBefore(fechaInicio)) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "La fecha de fin no puede ser anterior al inicio."));
            }

            cursoService.guardarPropuesta(
                    processInstanceId,
                    programa,
                    presupuesto,
                    fechaInicio,
                    fechaFin);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Propuesta guardada correctamente."));

        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Datos invalidos: " + e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",
                            "No se pudo guardar la propuesta."));
        }
    }

    // Guardar la propuesta y completar la tarea en Flowable.
    @PostMapping("/api/tareas/{tareaId}/completar-propuesta")
    public ResponseEntity<?> completarPropuesta(
            @PathVariable("tareaId") String tareaId,
            @RequestBody Map<String, Object> datos,
            HttpSession sesion) {

        String usuario =
                (String) sesion.getAttribute("usuarioFlowable");

        if (usuario == null || usuario.isBlank()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Debes iniciar sesion."));
        }

        if (!"jefe_admin".equals(usuario)) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "error",
                            "No tienes permiso para completar propuestas."));
        }

        try {
            JsonNode tarea =
                    flowableService.obtenerTareaPorId(tareaId);

            if (tarea == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "La tarea no existe."));
            }

            if (!usuario.equals(
                    tarea.path("assignee").asText(""))) {
                return ResponseEntity.status(403)
                        .body(Map.of(
                                "error",
                                "La tarea no esta asignada a tu usuario."));
            }

            if (!"Registrar y Elaborar propuesta"
                    .equals(tarea.path("name").asText(""))) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "La tarea no corresponde a elaborar una propuesta."));
            }

            String processInstanceId =
                    tarea.path("processInstanceId").asText();

            if (processInstanceId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "La tarea no tiene una instancia de proceso valida."));
            }

            String programa =
                    String.valueOf(datos.getOrDefault("programa", "")).trim();

            if (programa.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "El programa es obligatorio."));
            }

            BigDecimal presupuesto = new BigDecimal(
                    String.valueOf(datos.get("presupuesto")));

            if (presupuesto.signum() < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "El presupuesto no puede ser negativo."));
            }

            // Flowable recibe el presupuesto como variable long.
            // Por eso, en esta versión debe ser un número entero.
            presupuesto.longValueExact();

            LocalDate fechaInicio = LocalDate.parse(
                    String.valueOf(datos.get("fechaInicioPosible")));

            LocalDate fechaFin = LocalDate.parse(
                    String.valueOf(datos.get("fechaFinPropuesta")));

            if (fechaFin.isBefore(fechaInicio)) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "La fecha de fin no puede ser anterior al inicio."));
            }

            // Guardar los datos en PostgreSQL.
            cursoService.guardarPropuesta(
                    processInstanceId,
                    programa,
                    presupuesto,
                    fechaInicio,
                    fechaFin);

            // Completar la tarea enviando las variables a Flowable.
            flowableService.completarTareaConPropuesta(
                    tareaId,
                    programa,
                    presupuesto,
                    fechaInicio,
                    fechaFin);

            return ResponseEntity.ok(Map.of(
                    "mensaje",
                    "Propuesta guardada y tarea completada correctamente."));

        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Datos invalidos: " + e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",
                            "No se pudo completar la propuesta. Revisar los logs."));
        }
    }
}
