package curso.flowable;

import curso.flowable.dto.SolicitudCurso;
import curso.flowable.repository.EmpresaRepository;
import curso.flowable.service.CursoService;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
public class CursoController {

    private final EmpresaRepository empresaRepository;
    private final CursoService cursoService;

    public CursoController(EmpresaRepository empresaRepository,
            CursoService cursoService) {

        this.empresaRepository = empresaRepository;
        this.cursoService = cursoService;
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
}
