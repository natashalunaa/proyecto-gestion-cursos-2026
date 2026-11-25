package curso.flowable;

import curso.flowable.service.CursoService;
import curso.flowable.service.InformePdfService;

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
        private final InformePdfService informePdfService;
        private final String claveIntegracion;

        public IntegracionFlowableController(
                        CursoService cursoService,
                        InformePdfService informePdfService,
                        @Value("${integracion.flowable.secret}") String claveIntegracion) {

                this.cursoService = cursoService;
                this.informePdfService = informePdfService;
                this.claveIntegracion = claveIntegracion;
        }

        // Verificar la autenticacion de Flowable.
        private boolean claveValida(String claveRecibida) {

                return claveRecibida != null
                                && MessageDigest.isEqual(
                                                claveIntegracion.getBytes(
                                                                StandardCharsets.UTF_8),
                                                claveRecibida.getBytes(
                                                                StandardCharsets.UTF_8));
        }

        // ==========================================
        // HABILITAR ACCESO DE LA EMPRESA
        // ==========================================

        @PostMapping("/api/interno/empresas/habilitar-acceso")
        public ResponseEntity<?> habilitarAcceso(
                        @RequestHeader(value = "X-Flowable-Secret", required = false) String claveRecibida,
                        @RequestBody Map<String, String> datos) {

                if (!claveValida(claveRecibida)) {

                        return ResponseEntity.status(401)
                                        .body(Map.of(
                                                        "error", "Acceso no autorizado."));
                }

                String processInstanceId = datos.get("processInstanceId");

                if (processInstanceId == null
                                || processInstanceId.isBlank()) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error", "Falta processInstanceId."));
                }

                try {
                        cursoService.habilitarAccesoEmpresa(
                                        processInstanceId,
                                        datos.get("fechaCurso"));

                        return ResponseEntity.ok(Map.of(
                                        "mensaje",
                                        "Acceso de empresa habilitado correctamente."));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", e.getMessage()));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No se pudo habilitar el acceso."));
                }
        }

        // ==========================================
        // GUARDAR INFORME DE RESULTADOS
        // ==========================================

        @PostMapping("/api/interno/cursos/guardar-informe")
        public ResponseEntity<?> guardarInforme(
                        @RequestHeader(value = "X-Flowable-Secret", required = false) String claveRecibida,
                        @RequestBody Map<String, String> datos) {

                if (!claveValida(claveRecibida)) {

                        return ResponseEntity.status(401)
                                        .body(Map.of(
                                                        "error", "Acceso no autorizado."));
                }

                String processInstanceId = datos.get("processInstanceId");

                if (processInstanceId == null
                                || processInstanceId.isBlank()) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of(
                                                        "error", "Falta processInstanceId."));
                }

                String informeResultados = datos.get("informeResultados");

                String informePdfBase64 = datos.get("informePdfBase64");

                try {

                        String pdf;

                        if (informePdfBase64 != null
                                        && !informePdfBase64.isBlank()) {

                                pdf = informePdfService.validarPdf(
                                                informePdfBase64);

                        } else if ("true".equalsIgnoreCase(
                                        datos.get("informeAdjunto"))) {

                                Map<String, Object> curso = cursoService.buscarCursoPorProcessInstanceId(
                                                processInstanceId);

                                if (curso == null) {
                                        throw new IllegalArgumentException(
                                                        "No se encontro el curso.");
                                }

                                Object informeGuardado = curso.get("informe_resultados_pdf");

                                if (informeGuardado == null
                                                || informeGuardado.toString().isBlank()) {

                                        throw new IllegalArgumentException(
                                                        "No se encontro el PDF adjunto.");
                                }

                                pdf = informePdfService.validarPdf(
                                                informeGuardado.toString());

                        } else {

                                pdf = informePdfService.generarPdf(
                                                informeResultados);
                        }

                        // Guardar el PDF en PostgreSQL.
                        cursoService.guardarInforme(
                                        processInstanceId,
                                        pdf);

                        return ResponseEntity.ok(Map.of(
                                        "mensaje",
                                        "Informe guardado correctamente."));

                } catch (IllegalArgumentException e) {

                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", e.getMessage()));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body(Map.of(
                                                        "error",
                                                        "No se pudo guardar el informe."));
                }
        }
}