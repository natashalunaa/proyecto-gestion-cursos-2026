package curso.flowable;

import curso.flowable.service.CursoService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
public class IntegracionFlowableController {

    private final CursoService cursoService;
    private final String claveIntegracion;

    public IntegracionFlowableController(
            CursoService cursoService,
            @Value("${integracion.flowable.secret}") String claveIntegracion) {

        this.cursoService = cursoService;
        this.claveIntegracion = claveIntegracion;
    }

    @PostMapping("/api/interno/empresas/habilitar-acceso")
    public ResponseEntity<?> habilitarAcceso(
            @RequestHeader(value = "X-Flowable-Secret", required = false)
            String claveRecibida,
            @RequestBody Map<String, String> datos) {

        if (claveRecibida == null ||
                !MessageDigest.isEqual(
                        claveIntegracion.getBytes(StandardCharsets.UTF_8),
                        claveRecibida.getBytes(StandardCharsets.UTF_8))) {

            return ResponseEntity.status(401)
                    .body(Map.of("error", "Acceso no autorizado."));
        }

        String processInstanceId = datos.get("processInstanceId");

        if (processInstanceId == null || processInstanceId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Falta processInstanceId."));
        }

        try {
            cursoService.habilitarAccesoEmpresa(processInstanceId);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Acceso de empresa habilitado correctamente."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "No se pudo habilitar el acceso."));
        }
    }
}
