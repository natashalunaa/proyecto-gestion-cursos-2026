
package curso.flowable.service;

import curso.flowable.dto.SolicitudCurso;
import curso.flowable.repository.EmpresaRepository;
import curso.flowable.repository.CursoRepository;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    public String registrarSolicitud(SolicitudCurso solicitud) throws Exception {

        // Iniciar el proceso en Flowable
        String processInstanceId = flowableService.iniciarProceso(solicitud);

        try {
            // Guardar el curso en PostgreSQL
            cursoRepository.guardarCurso(solicitud, processInstanceId);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo guardar el curso. Instancia creada: "
                            + processInstanceId,
                    e);
        }

        return processInstanceId;
    }

    // Consultar los datos de un curso por su ID
    public Map<String, Object> buscarCursoPorId(Integer cursoId) {
        return cursoRepository.buscarCursoPorId(cursoId);
    }

    // Buscar el curso asociado a una instancia de Flowable
    public Map<String, Object> buscarCursoPorProcessInstanceId(
            String processInstanceId) {

        return cursoRepository.buscarCursoPorProcessInstanceId(processInstanceId);
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
                    "La fecha de finalización no puede ser anterior a la fecha de inicio.");
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
    public void habilitarAccesoEmpresa(String processInstanceId) {

        // Buscar el curso asociado a la instancia de Flowable
        Map<String, Object> curso = cursoRepository.buscarCursoPorProcessInstanceId(processInstanceId);

        if (curso == null) {
            throw new IllegalArgumentException(
                    "No se encontró el curso asociado al proceso.");
        }

        // Obtener la empresa asociada al curso
        Integer empresaId = ((Number) curso.get("empresa_id")).intValue();

        // Si ya tiene cuenta, conservamos sus credenciales
        if (empresaRepository.tieneCuenta(empresaId)) {
            return;
        }

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
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String passwordHash = encoder.encode(password);

        // Guardar las credenciales sin sobrescribir una cuenta existente
        int filas = empresaRepository.guardarCredenciales(
                empresaId,
                usuario,
                passwordHash);

        if (filas == 0 && !empresaRepository.tieneCuenta(empresaId)) {
            throw new IllegalStateException(
                    "No se pudo habilitar la cuenta de la empresa.");
        }

        // IMPORTANTE:
        // La contraseña original todavía no se entrega a la empresa.
        // Debemos definir cómo comunicarla o simular su entrega.
    }
}
