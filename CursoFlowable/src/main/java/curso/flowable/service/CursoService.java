package curso.flowable.service;

import curso.flowable.dto.SolicitudCurso;
import curso.flowable.repository.EmpresaRepository;
import curso.flowable.repository.CursoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.security.SecureRandom;

@Service
public class CursoService {

    private final EmpresaRepository empresaRepository;
    private final CursoRepository cursoRepository;
    private final FlowableService flowableService;

    // Constructor
    public CursoService(
            EmpresaRepository empresaRepository,
            CursoRepository cursoRepository,
            FlowableService flowableService) {

        this.empresaRepository = empresaRepository;
        this.cursoRepository = cursoRepository;
        this.flowableService = flowableService;
    }

    // Comprobar si existe una empresa por su ID
    public boolean existeEmpresa(Integer empresaId) {

        return empresaRepository.existeEmpresaPorId(empresaId);
    }

    // Buscar una empresa por su CUIT
    public Integer buscarEmpresaPorCuit(String cuit) {

        return empresaRepository.buscarIdPorCuit(cuit);
    }

    // Registrar una empresa nueva y devolver su ID
    public Integer registrarEmpresa(
            String cuit,
            String razonSocial,
            String contacto,
            String email,
            String telefono) {

        return empresaRepository.registrarEmpresa(
                cuit,
                razonSocial,
                contacto,
                email,
                telefono);
    }

    // Iniciar el proceso BPMN y guardar la solicitud del curso
    public String registrarSolicitud(SolicitudCurso solicitud)
            throws Exception {

        // Iniciar el proceso en Flowable
        String processInstanceId =
                flowableService.iniciarProceso(solicitud);

        try {

            // Guardar el curso en PostgreSQL
            cursoRepository.guardarCurso(
                    solicitud,
                    processInstanceId);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "No se pudo guardar el curso. Instancia creada: "
                            + processInstanceId,
                    e);
        }

        flowableService.confirmarRegistro(processInstanceId);

        return processInstanceId;
    }

    // Consultar los datos de un curso por su ID
    public Map<String, Object> buscarCursoPorId(Integer cursoId) {

        return cursoRepository.buscarCursoPorId(cursoId);
    }

    // Obtener los cursos asociados a una empresa
    public List<Map<String, Object>> buscarCursosPorEmpresaId(
            Integer empresaId) {

        return cursoRepository.buscarCursosPorEmpresaId(empresaId);
    }

    // Buscar el curso asociado a una instancia de Flowable
    public Map<String, Object> buscarCursoPorProcessInstanceId(
            String processInstanceId) {

        return cursoRepository.buscarCursoPorProcessInstanceId(
                processInstanceId);
    }

    // Guardar la propuesta elaborada por el jefe administrativo
    public void guardarPropuesta(
            String processInstanceId,
            String programa,
            BigDecimal presupuesto,
            LocalDate fechaInicioPosible,
            LocalDate fechaFinPropuesta) {

        // Comprobar que las fechas sean correctas
        if (fechaFinPropuesta.isBefore(fechaInicioPosible)) {

            throw new IllegalArgumentException(
                    "La fecha de finalización no puede ser anterior "
                            + "a la fecha de inicio.");
        }

        // Guardar los datos en PostgreSQL
        int filasActualizadas = cursoRepository.guardarPropuesta(
                processInstanceId,
                programa,
                presupuesto,
                fechaInicioPosible,
                fechaFinPropuesta);

        // Verificar que se haya encontrado el curso
        if (filasActualizadas == 0) {

            throw new IllegalArgumentException(
                    "No se encontró un curso asociado a esta instancia.");
        }
    }

    // Habilitar la cuenta de una empresa cuando acepta la propuesta
    @Transactional
    public void habilitarAccesoEmpresa(String processInstanceId) {

        // Buscar el curso asociado a la instancia de Flowable
        Map<String, Object> curso =
                cursoRepository.buscarCursoPorProcessInstanceId(
                        processInstanceId);

        if (curso == null) {

            throw new IllegalArgumentException(
                    "No se encontró el curso asociado al proceso.");
        }

        // Obtener la empresa asociada al curso
        Integer empresaId =
                ((Number) curso.get("empresa_id")).intValue();

        String razonSocial = (String) curso.get("empresa");

        // Si la empresa no tiene cuenta, generar sus credenciales
        if (!empresaRepository.tieneCuenta(empresaId)) {

            // Generar un usuario basado en el ID de la empresa
            String usuario = "empresa_" + empresaId;

            // Generar una contraseña aleatoria
            SecureRandom random = new SecureRandom();

            byte[] bytes = new byte[24];

            random.nextBytes(bytes);

            String password = java.util.Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(bytes);

            // Proteger la contraseña con BCrypt
            BCryptPasswordEncoder encoder =
                    new BCryptPasswordEncoder();

            String passwordHash = encoder.encode(password);

            // Guardar las credenciales sin sobrescribir
            // una cuenta existente
            int filas = empresaRepository.guardarCredenciales(
                    empresaId,
                    usuario,
                    passwordHash);

            if (filas == 0
                    && !empresaRepository.tieneCuenta(empresaId)) {

                throw new IllegalStateException(
                        "No se pudo habilitar la cuenta de la empresa.");
            }

            // Simular la entrega de credenciales
            if (filas > 0
                    && Boolean.getBoolean(
                            "simular.entrega.credenciales")) {

                System.out.println();
                System.out.println(
                        "==========================================");
                System.out.println(
                        "     ENVÍO SIMULADO DE CREDENCIALES");
                System.out.println(
                        "==========================================");
                System.out.println("Empresa: " + razonSocial);
                System.out.println("Usuario: " + usuario);
                System.out.println("Contraseña: " + password);
                System.out.println("Portal: http://localhost:8081/");
                System.out.println(
                        "==========================================");
                System.out.println();
            }

        } else {

            // Si la empresa ya tiene cuenta,
            // conservar sus credenciales actuales
            System.out.println(
                    "La empresa " + razonSocial
                            + " ya tiene una cuenta habilitada.");
        }

        // Actualizar el estado del curso independientemente
        // de si la empresa tiene una cuenta nueva o existente
        int filasActualizadas = cursoRepository.actualizarEstadoCurso(
                processInstanceId,
                "en_evaluacion",
                "en_preparacion");

        if (filasActualizadas > 0) {

            System.out.println(
                    "Estado del curso actualizado a en_preparacion.");

            System.out.println(
                    "Instancia de Flowable: " + processInstanceId);

        } else {

            System.out.println(
                    "No se modificó el estado del curso. "
                            + "El curso no se encuentra en evaluación.");
        }
    }
}