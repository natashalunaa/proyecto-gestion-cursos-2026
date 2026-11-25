package curso.flowable;

import curso.flowable.dto.SolicitudCurso;
import curso.flowable.repository.EmpresaRepository;
import curso.flowable.service.CursoService;
import curso.flowable.service.FlowableService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        // ==========================================
        // COMPROBAR FUNCIONAMIENTO DE LA API
        // ==========================================

        @GetMapping("/api/cursos/saludo")
        public String saludo() {

                return "API de Gestion de Cursos funcionando correctamente";
        }

        // ==========================================
        // COMPROBAR CONEXION CON POSTGRESQL
        // ==========================================

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

        // ==========================================
        // REGISTRAR SOLICITUD E INICIAR PROCESO BPMN
        // ==========================================

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

                // Buscar si la empresa ya existe
                Integer empresaId = cursoService.buscarEmpresaPorCuit(
                                solicitud.getCuit());

                // Si no existe, registrar una empresa nueva
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

                // Asociar la solicitud a la empresa
                solicitud.setEmpresaId(empresaId);

                try {

                        String processInstanceId = cursoService.registrarSolicitud(solicitud);

                        return ResponseEntity.ok(Map.of(
                                        "mensaje",
                                        "Solicitud de curso registrada correctamente",
                                        "processInstanceId",
                                        processInstanceId));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body("Error al iniciar o registrar el curso.");
                }
        }

        // Permite consultar cursos por ID únicamente a administrativos.
        @GetMapping("/api/cursos/{id}")
        public ResponseEntity<?> obtenerCurso(
                        @PathVariable("id") Integer id,
                        HttpSession sesion) {

                String usuario = (String) sesion.getAttribute("usuarioFlowable");
                String rol = (String) sesion.getAttribute("tipoUsuario");

                if (rol == null) {
                        return ResponseEntity.status(401)
                                        .body(Map.of("error", "Debes iniciar sesión."));
                }

                if (!"administrativo".equals(rol) || usuario == null) {
                        return ResponseEntity.status(403)
                                        .body(Map.of("error", "Acceso no autorizado."));
                }

                Map<String, Object> curso = cursoService.buscarCursoPorId(id);

                if (curso == null) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok(curso);
        }

        // ==========================================
        // CONSULTAR TAREAS DEL USUARIO
        // ==========================================

        @GetMapping("/api/tareas/mis-tareas")
        public ResponseEntity<?> obtenerMisTareas(
                        HttpSession sesion) {

                String usuario = (String) sesion.getAttribute("usuarioFlowable");

                // Obtener el rol del usuario conectado.
                String rol = (String) sesion.getAttribute("tipoUsuario");

                // Verificar que exista una sesion activa
                if (usuario == null || usuario.isBlank()) {

                        return ResponseEntity.status(401)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesion."));
                }

                // Permitir únicamente usuarios internos autorizados.
                if (!"administrativo".equals(rol) && !"docente".equals(rol)) {
                        return ResponseEntity.status(403)
                                        .body(Map.of("error", "No tienes permiso para consultar tareas."));
                }

                try {

                        // Consultar tareas asignadas al usuario
                        JsonNode asignadas = flowableService.obtenerTareasUsuario(usuario);

                        // Consultar tareas reclamables únicamente para administrativos.
                        JsonNode candidatas = "administrativo".equals(rol)
                                        ? flowableService.obtenerTareasCandidatas(usuario)
                                        : new ObjectMapper().createObjectNode();

                        // Evitar tareas duplicadas
                        Map<String, JsonNode> tareasUnicas = new LinkedHashMap<>();

                        // Incorporar tareas asignadas
                        for (JsonNode tarea : asignadas.path("data")) {

                                if (tarea.hasNonNull("id")) {

                                        tareasUnicas.put(
                                                        tarea.get("id").asText(),
                                                        tarea);
                                }
                        }

                        // Incorporar tareas candidatas sin asignar
                        for (JsonNode tarea : candidatas.path("data")) {

                                if (tarea.hasNonNull("id")
                                                && tarea.path("assignee").isNull()) {

                                        tareasUnicas.putIfAbsent(
                                                        tarea.get("id").asText(),
                                                        tarea);
                                }
                        }

                        // Crear la respuesta JSON
                        ObjectMapper mapper = new ObjectMapper();

                        ObjectNode resultado = mapper.createObjectNode();

                        resultado.put("usuario", usuario);

                        ArrayNode lista = resultado.putArray("data");

                        // ==========================================
                        // INCORPORAR INFORMACION DEL CURSO
                        // ==========================================

                        for (JsonNode tarea : tareasUnicas.values()) {

                                // Copiar los datos originales de Flowable
                                ObjectNode tareaCompleta = tarea.deepCopy();

                                // Obtener el identificador de la instancia
                                String processInstanceId = tarea.path("processInstanceId").asText("");

                                // Buscar el curso asociado en PostgreSQL
                                Map<String, Object> curso = null;

                                if (!processInstanceId.isBlank()) {

                                        curso = cursoService
                                                        .buscarCursoPorProcessInstanceId(
                                                                        processInstanceId);
                                }

                                // Crear un objeto con los datos del curso
                                ObjectNode datosCurso = tareaCompleta.putObject("curso");

                                if (curso != null) {

                                        datosCurso.put("encontrado", true);

                                        datosCurso.set(
                                                        "id",
                                                        mapper.valueToTree(curso.get("id")));

                                        datosCurso.set(
                                                        "tematica",
                                                        mapper.valueToTree(curso.get("tematica")));

                                        datosCurso.set(
                                                        "empresa",
                                                        mapper.valueToTree(curso.get("empresa")));

                                        datosCurso.set(
                                                        "cantidad_participantes",
                                                        mapper.valueToTree(
                                                                        curso.get("cantidad_participantes")));

                                        datosCurso.set(
                                                        "estado",
                                                        mapper.valueToTree(curso.get("estado")));

                                } else {

                                        // Instancia sin curso asociado en PostgreSQL
                                        datosCurso.put("encontrado", false);
                                }

                                // Agregar la tarea con los datos del curso
                                lista.add(tareaCompleta);
                        }

                        return ResponseEntity.ok(resultado);

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No se pudieron consultar las tareas de Flowable."));
                }
        }

        // ==========================================
        // CONSULTAR CURSO POR INSTANCIA DE FLOWABLE
        // ==========================================

        // Permite consultar cursos por instancia únicamente a administrativos.
        @GetMapping("/api/cursos/instancia/{processInstanceId}")
        public ResponseEntity<?> obtenerCursoPorInstancia(
                        @PathVariable("processInstanceId") String processInstanceId,
                        HttpSession sesion) {

                String usuario = (String) sesion.getAttribute("usuarioFlowable");
                String rol = (String) sesion.getAttribute("tipoUsuario");

                if (rol == null) {
                        return ResponseEntity.status(401)
                                        .body(Map.of("error", "Debes iniciar sesión."));
                }

                if (!"administrativo".equals(rol) || usuario == null) {
                        return ResponseEntity.status(403)
                                        .body(Map.of("error", "Acceso no autorizado."));
                }

                Map<String, Object> curso = cursoService.buscarCursoPorProcessInstanceId(processInstanceId);

                if (curso == null) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok(curso);
        }

        // ==========================================
        // RECLAMAR UNA TAREA DISPONIBLE
        // ==========================================

        @PostMapping("/api/tareas/{tareaId}/reclamar")
        public ResponseEntity<?> reclamarTarea(
                        @PathVariable("tareaId") String tareaId,
                        HttpSession sesion) {

                String usuario = (String) sesion.getAttribute("usuarioFlowable");

                // Verificar que exista una sesión activa
                if (usuario == null || usuario.isBlank()) {
                        return ResponseEntity.status(401)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesion."));
                }
                // Permitir reclamar tareas únicamente a los administrativos.
                String rol = (String) sesion.getAttribute("tipoUsuario");

                if (!"administrativo".equals(rol)) {
                        return ResponseEntity.status(403)
                                        .body(Map.of(
                                                        "error",
                                                        "No tienes permiso para reclamar tareas."));
                }

                try {

                        // Consultar las tareas que el usuario puede reclamar
                        JsonNode candidatas = flowableService.obtenerTareasCandidatas(usuario);

                        boolean disponible = false;

                        // Comprobar que la tarea pertenezca a sus candidatas
                        for (JsonNode tarea : candidatas.path("data")) {

                                if (tarea.path("id").asText().equals(tareaId)
                                                && tarea.path("assignee").isNull()) {

                                        disponible = true;
                                        break;
                                }
                        }

                        if (!disponible) {
                                return ResponseEntity.status(403)
                                                .body(Map.of(
                                                                "error",
                                                                "La tarea no esta disponible para reclamar."));
                        }

                        // Reclamar la tarea en Flowable
                        flowableService.reclamarTarea(tareaId, usuario);

                        return ResponseEntity.ok(Map.of(
                                        "mensaje",
                                        "Tarea reclamada correctamente.",
                                        "tareaId",
                                        tareaId,
                                        "usuario",
                                        usuario));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No se pudo reclamar la tarea."));
                }
        }

        // ==========================================
        // COMPLETAR UNA TAREA NORMAL
        // ==========================================

        @PostMapping("/api/tareas/{tareaId}/completar")
        public ResponseEntity<?> completarTarea(
                        @PathVariable("tareaId") String tareaId,
                        HttpSession sesion) {

                String usuario = (String) sesion.getAttribute("usuarioFlowable");

                if (usuario == null || usuario.isBlank()) {

                        return ResponseEntity.status(401)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesion."));
                }
                // Verificar que el usuario tenga un rol autorizado.
                String rol = (String) sesion.getAttribute("tipoUsuario");

                if (!"administrativo".equals(rol) && !"docente".equals(rol)) {
                        return ResponseEntity.status(403)
                                        .body(Map.of(
                                                        "error",
                                                        "No tienes permiso para completar tareas."));
                }
                try {

                        JsonNode tarea = flowableService.obtenerTareaPorId(tareaId);

                        if (tarea == null) {

                                return ResponseEntity.status(404)
                                                .body(Map.of(
                                                                "error",
                                                                "La tarea no existe."));
                        }

                        String responsable = tarea.path("assignee").asText("");
                        // Identificar si la tarea corresponde al desarrollo de un curso.
                        boolean esDesarrollo = "Desarrollar curso".equalsIgnoreCase(
                                        tarea.path("name").asText("").trim());

                        // Los docentes solo pueden completar el desarrollo de cursos.
                        if ("docente".equals(rol) && !esDesarrollo) {
                                return ResponseEntity.status(403)
                                                .body(Map.of(
                                                                "error",
                                                                "Esta tarea debe completarse desde Flowable."));
                        }

                        // El desarrollo del curso corresponde únicamente a docentes.
                        if ("administrativo".equals(rol) && esDesarrollo) {
                                return ResponseEntity.status(403)
                                                .body(Map.of(
                                                                "error",
                                                                "Esta tarea corresponde al personal docente."));
                        }

                        // Verificar que la tarea pertenezca al usuario
                        if (!usuario.equals(responsable)) {

                                return ResponseEntity.status(403)
                                                .body(Map.of(
                                                                "error",
                                                                "No tienes permiso para completar esta tarea."));
                        }

                        // La propuesta se completa por su endpoint especifico
                        if ("Registrar y Elaborar propuesta"
                                        .equals(tarea.path("name").asText(""))) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "La propuesta debe completarse desde su formulario."));
                        }

                        // Los recursos deben completarse desde su formulario
                        if ("Evaluar Recursos".equals(tarea.path("name").asText(""))) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "Los recursos deben evaluarse desde su formulario."));
                        }

                        // Completar la tarea en Flowable
                        flowableService.completarTarea(tareaId);

                        return ResponseEntity.ok(Map.of(
                                        "mensaje",
                                        "Tarea completada correctamente."));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No se pudo completar la tarea."));
                }
        }

        // ==========================================
        // COMPLETAR TAREA EVALUAR RECURSOS
        // ==========================================

        @PostMapping("/api/tareas/{tareaId}/completar-recursos")
        public ResponseEntity<?> completarRecursos(
                        @PathVariable("tareaId") String tareaId,
                        @RequestBody Map<String, Object> datos,
                        HttpSession sesion) {

                String usuario = (String) sesion.getAttribute("usuarioFlowable");

                // Verificar que exista una sesion activa
                if (usuario == null || usuario.isBlank()) {
                        return ResponseEntity.status(401)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesion."));
                }
                // Permitir la evaluación de recursos únicamente a administrativos.
                String rol = (String) sesion.getAttribute("tipoUsuario");

                if (!"administrativo".equals(rol)) {
                        return ResponseEntity.status(403)
                                        .body(Map.of(
                                                        "error",
                                                        "No tienes permiso para evaluar recursos."));
                }

                try {

                        // Buscar la tarea en Flowable
                        JsonNode tarea = flowableService.obtenerTareaPorId(tareaId);

                        if (tarea == null) {
                                return ResponseEntity.status(404)
                                                .body(Map.of(
                                                                "error",
                                                                "La tarea no existe."));
                        }

                        // Verificar que la tarea este asignada al usuario
                        if (!usuario.equals(tarea.path("assignee").asText(""))) {
                                return ResponseEntity.status(403)
                                                .body(Map.of(
                                                                "error",
                                                                "No tienes permiso para completar esta tarea."));
                        }

                        // Verificar el tipo de tarea
                        if (!"Evaluar Recursos".equals(tarea.path("name").asText(""))) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "La tarea no corresponde a Evaluar Recursos."));
                        }

                        // Verificar que las variables sean booleanas
                        if (!(datos.get("requiereAula") instanceof Boolean)
                                        || !(datos.get("requierePlataforma") instanceof Boolean)
                                        || !(datos.get("requiereEquipamiento") instanceof Boolean)) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "Los recursos deben indicarse con valores booleanos."));
                        }

                        // Obtener los recursos seleccionados
                        boolean requiereAula = Boolean.TRUE.equals(datos.get("requiereAula"));

                        boolean requierePlataforma = Boolean.TRUE.equals(datos.get("requierePlataforma"));

                        boolean requiereEquipamiento = Boolean.TRUE.equals(datos.get("requiereEquipamiento"));

                        // Validar que se seleccione al menos un recurso
                        if (!requiereAula
                                        && !requierePlataforma
                                        && !requiereEquipamiento) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "Debe seleccionar al menos un recurso."));
                        }

                        // Completar la tarea enviando las variables a Flowable
                        flowableService.completarTareaConRecursos(
                                        tareaId,
                                        requiereAula,
                                        requierePlataforma,
                                        requiereEquipamiento);

                        return ResponseEntity.ok(Map.of(
                                        "mensaje",
                                        "Recursos evaluados correctamente."));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No se pudo completar la evaluacion de recursos."));
                }
        }
        // ==========================================
        // GUARDAR PROPUESTA SIN COMPLETAR LA TAREA
        // ==========================================

        @PostMapping("/api/cursos/instancia/{processInstanceId}/propuesta")
        public ResponseEntity<?> guardarPropuesta(
                        @PathVariable("processInstanceId") String processInstanceId,
                        @RequestBody Map<String, Object> datos,
                        HttpSession sesion) {

                String usuario = (String) sesion.getAttribute("usuarioFlowable");

                if (usuario == null || usuario.isBlank()) {

                        return ResponseEntity.status(401)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesion."));
                }

                // Solo el jefe administrativo puede elaborar propuestas
                if (!"jefe_admin".equals(usuario)) {

                        return ResponseEntity.status(403)
                                        .body(Map.of(
                                                        "error",
                                                        "No tienes permiso para elaborar propuestas."));
                }

                try {

                        String programa = String.valueOf(
                                        datos.getOrDefault("programa", "")).trim();

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
                                        String.valueOf(
                                                        datos.get("fechaInicioPosible")));

                        LocalDate fechaFin = LocalDate.parse(
                                        String.valueOf(
                                                        datos.get("fechaFinPropuesta")));

                        if (fechaFin.isBefore(fechaInicio)) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "La fecha de fin no puede ser anterior al inicio."));
                        }

                        // Guardar la propuesta en PostgreSQL
                        cursoService.guardarPropuesta(
                                        processInstanceId,
                                        programa,
                                        presupuesto,
                                        fechaInicio,
                                        fechaFin);

                        return ResponseEntity.ok(Map.of(
                                        "mensaje",
                                        "Propuesta guardada correctamente."));

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

        // ==========================================
        // GUARDAR PROPUESTA Y COMPLETAR LA TAREA
        // ==========================================

        @PostMapping("/api/tareas/{tareaId}/completar-propuesta")
        public ResponseEntity<?> completarPropuesta(
                        @PathVariable("tareaId") String tareaId,
                        @RequestBody Map<String, Object> datos,
                        HttpSession sesion) {

                String usuario = (String) sesion.getAttribute("usuarioFlowable");

                if (usuario == null || usuario.isBlank()) {

                        return ResponseEntity.status(401)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesion."));
                }

                // Verificar permisos del jefe administrativo
                if (!"jefe_admin".equals(usuario)) {

                        return ResponseEntity.status(403)
                                        .body(Map.of(
                                                        "error",
                                                        "No tienes permiso para completar propuestas."));
                }

                try {

                        // Buscar la tarea en Flowable
                        JsonNode tarea = flowableService.obtenerTareaPorId(tareaId);

                        if (tarea == null) {

                                return ResponseEntity.status(404)
                                                .body(Map.of(
                                                                "error",
                                                                "La tarea no existe."));
                        }

                        // Verificar que la tarea este asignada al usuario
                        if (!usuario.equals(
                                        tarea.path("assignee").asText(""))) {

                                return ResponseEntity.status(403)
                                                .body(Map.of(
                                                                "error",
                                                                "La tarea no esta asignada a tu usuario."));
                        }

                        // Verificar que sea una tarea de propuesta
                        if (!"Registrar y Elaborar propuesta"
                                        .equals(tarea.path("name").asText(""))) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "La tarea no corresponde a elaborar una propuesta."));
                        }

                        // Obtener la instancia asociada a la tarea
                        String processInstanceId = tarea.path("processInstanceId").asText();

                        if (processInstanceId.isBlank()) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "La tarea no tiene una instancia de proceso valida."));
                        }

                        // Validar el programa
                        String programa = String.valueOf(
                                        datos.getOrDefault("programa", "")).trim();

                        if (programa.isBlank()) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "El programa es obligatorio."));
                        }

                        // Validar el presupuesto
                        BigDecimal presupuesto = new BigDecimal(
                                        String.valueOf(datos.get("presupuesto")));

                        if (presupuesto.signum() < 0) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "El presupuesto no puede ser negativo."));
                        }

                        // Flowable recibe el presupuesto como variable long
                        presupuesto.longValueExact();

                        // Validar las fechas
                        LocalDate fechaInicio = LocalDate.parse(
                                        String.valueOf(
                                                        datos.get("fechaInicioPosible")));

                        LocalDate fechaFin = LocalDate.parse(
                                        String.valueOf(
                                                        datos.get("fechaFinPropuesta")));

                        if (fechaFin.isBefore(fechaInicio)) {

                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "error",
                                                                "La fecha de fin no puede ser anterior al inicio."));
                        }

                        // Guardar los datos en PostgreSQL
                        cursoService.guardarPropuesta(
                                        processInstanceId,
                                        programa,
                                        presupuesto,
                                        fechaInicio,
                                        fechaFin);

                        // Completar la tarea enviando las variables a Flowable
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

        // ==========================================
        // CONSULTAR CURSOS DE LA EMPRESA AUTENTICADA
        // ==========================================

        @GetMapping("/api/empresas/mis-cursos")
        public ResponseEntity<?> obtenerMisCursosEmpresa(
                        HttpSession sesion) {

                Integer empresaId = (Integer) sesion.getAttribute("empresaId");

                // Comprobar que haya una empresa autenticada
                if (empresaId == null) {

                        return ResponseEntity.status(401)
                                        .body(Map.of(
                                                        "error",
                                                        "Debes iniciar sesión como empresa."));
                }

                try {

                        return ResponseEntity.ok(
                                        cursoService.buscarCursosPorEmpresaId(empresaId));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No se pudieron consultar los cursos de la empresa."));
                }
        }

        @GetMapping("/api/empresas/mis-cursos/{id}")
        public ResponseEntity<?> obtenerDetalleCursoEmpresa(
                        @PathVariable("id") Integer id,
                        HttpSession sesion) {

                Integer empresaId = (Integer) sesion.getAttribute("empresaId");

                if (empresaId == null) {
                        return ResponseEntity.status(401).body(
                                        Map.of("error", "Debe iniciar sesión como empresa."));
                }

                Map<String, Object> curso = cursoService.buscarCursoPorId(id);

                if (curso == null || curso.isEmpty()) {
                        return ResponseEntity.status(404).body(
                                        Map.of("error", "Curso no encontrado."));
                }

                Object propietario = curso.get("empresa_id");

                if (!(propietario instanceof Number)
                                || ((Number) propietario).intValue() != empresaId.intValue()) {

                        return ResponseEntity.status(403).body(
                                        Map.of("error", "No tiene acceso a este curso."));
                }

                Map<String, Object> detalle = cursoService.buscarCursoPorProcessInstanceId(
                                String.valueOf(curso.get("process_instance_id")));

                return ResponseEntity.ok(
                                detalle != null ? detalle : curso);
        }

        @GetMapping("/api/empresas/mis-cursos/{id}/actividades")
        public ResponseEntity<?> obtenerActividadesCursoEmpresa(
                        @PathVariable("id") Integer id,
                        HttpSession sesion) {

                Integer empresaId = (Integer) sesion.getAttribute("empresaId");

                if (empresaId == null) {
                        return ResponseEntity.status(401).body(
                                        Map.of("error", "Debe iniciar sesión como empresa."));
                }

                Map<String, Object> curso = cursoService.buscarCursoPorId(id);

                if (curso == null || curso.isEmpty()) {
                        return ResponseEntity.status(404).body(
                                        Map.of("error", "Curso no encontrado."));
                }

                Object propietario = curso.get("empresa_id");

                if (!(propietario instanceof Number)
                                || ((Number) propietario).intValue() != empresaId.intValue()) {

                        return ResponseEntity.status(403).body(
                                        Map.of("error", "No tiene acceso a este curso."));
                }

                try {

                        String processInstanceId = String.valueOf(curso.get("process_instance_id"));

                        return ResponseEntity.ok(
                                        flowableService.obtenerAvanceCurso(processInstanceId));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.status(502).body(
                                        Map.of(
                                                        "error",
                                                        "No se pudo consultar el avance en Flowable."));
                }
        }
}
