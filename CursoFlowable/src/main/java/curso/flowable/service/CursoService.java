
package curso.flowable.service;

import curso.flowable.dto.SolicitudCurso;
import curso.flowable.repository.EmpresaRepository;
import curso.flowable.repository.CursoRepository;
import org.springframework.stereotype.Service;
import java.util.Map;

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
}
