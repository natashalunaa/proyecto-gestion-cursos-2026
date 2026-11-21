package curso.flowable;

import curso.flowable.dto.SolicitudCurso;
import curso.flowable.repository.EmpresaRepository;
import curso.flowable.service.CursoService;

import java.util.Map;
import java.util.LinkedHashMap;
import jakarta.servlet.http.HttpSession;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import curso.flowable.service.FlowableService;
import com.fasterxml.jackson.databind.JsonNode;

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

    // Comprobar que Spring Boot funciona
    @GetMapping("/api/cursos/saludo")
    public String saludo() {
        return "API de Gestion de Cursos funcionando correctamente";
    }

    // Comprobar la conexion con PostgreSQL
    @GetMapping("/api/cursos/probar-bdd")
    public ResponseEntity<?> probarBaseDeDatos() {

        try {
            Integer cantidadEmpresas = empresaRepository.contarEmpresas();

            return ResponseEntity.ok(
                    "Conexion con PostgreSQL correcta. Empresas registradas: "
                            + cantidadEmpresas);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("No se pudo consultar PostgreSQL. Revisar los logs del servidor.");
        }
    }

    // Recibir una solicitud de capacitacion
    @PostMapping("/api/cursos/iniciar")
    public ResponseEntity<?> iniciarCurso(@RequestBody SolicitudCurso solicitud) {

        // Comprobar si la solicitud incluye el CUIT
        if (solicitud.getCuit() == null || solicitud.getCuit().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Debe indicar el CUIT de la empresa.");
        }

        // Comprobar que se haya indicado la temática
        if (solicitud.tematica() == null || solicitud.tematica().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Debe indicar la tematica del curso.");
        }

        // Buscar la empresa utilizando su CUIT
        Integer empresaId = cursoService.buscarEmpresaPorCuit(
                solicitud.getCuit());

        // Si la empresa no existe, registrarla automáticamente
        if (empresaId == null) {

            // Comprobar que estén los datos necesarios
            if (solicitud.getEmpresa() == null || solicitud.getEmpresa().isBlank()
                    || solicitud.getContacto() == null || solicitud.getContacto().isBlank()
                    || solicitud.getEmail() == null || solicitud.getEmail().isBlank()) {

                return ResponseEntity.badRequest()
                        .body("Debe completar los datos de la empresa.");
            }

            // Registrar la nueva empresa en PostgreSQL
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

        // Asignar el ID encontrado a la solicitud
        solicitud.setEmpresaId(empresaId);

        try {
            // El Service inicia Flowable y guarda el curso
            String processInstanceId = cursoService.registrarSolicitud(solicitud);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Solicitud de curso registrada correctamente",
                    "processInstanceId", processInstanceId));

        } catch (Exception e) {

            // Mostrar el error completo en la terminal de Spring Boot
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body("Error al iniciar o registrar el curso. "
                            + "Revisar los logs del servidor.");
        }
    }

    // Consultar los datos de un curso por su ID
    @GetMapping("/api/cursos/{id}")
    public ResponseEntity<?> obtenerCurso(@PathVariable("id") Integer id) {

        Map<String, Object> curso = cursoService.buscarCursoPorId(id);

        if (curso == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(curso);
    }

    // Consultar solo las tareas del usuario que inicio sesion.
    @GetMapping("/api/tareas/mis-tareas")
    public ResponseEntity<?> obtenerMisTareas(HttpSession sesion) {
        String usuario = (String) sesion.getAttribute("usuarioFlowable");
        if (usuario == null || usuario.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesion."));
        }
        try {
            JsonNode asignadas = flowableService.obtenerTareasUsuario(usuario);
            JsonNode candidatas = flowableService.obtenerTareasCandidatas(usuario);
            Map<String, JsonNode> tareasUnicas = new LinkedHashMap<>();
            for (JsonNode tarea : asignadas.path("data")) {
                if (tarea.hasNonNull("id")) tareasUnicas.put(tarea.get("id").asText(), tarea);
            }
            for (JsonNode tarea : candidatas.path("data")) {
                if (tarea.hasNonNull("id") && tarea.path("assignee").isNull()) {
                    tareasUnicas.putIfAbsent(tarea.get("id").asText(), tarea);
                }
            }
            ObjectNode resultado = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
            resultado.put("usuario", usuario);
            ArrayNode lista = resultado.putArray("data");
            tareasUnicas.values().forEach(lista::add);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "No se pudieron consultar las tareas de Flowable."));
        }
    }

    @GetMapping("/api/cursos/instancia/{processInstanceId}")
    public ResponseEntity<?> obtenerCursoPorInstancia(
            @PathVariable("processInstanceId") String processInstanceId) {

        Map<String, Object> curso = cursoService.buscarCursoPorProcessInstanceId(processInstanceId);

        if (curso == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(curso);
    }

}
