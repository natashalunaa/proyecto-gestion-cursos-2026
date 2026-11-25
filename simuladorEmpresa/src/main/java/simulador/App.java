package simulador;

import java.util.Scanner;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.JsonNode;

public class App {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int opcion;

        FlowableClient flowable = new FlowableClient();

        do {
            System.out.println("\n===== SIMULADOR DE EMPRESA =====");
            System.out.println("1. Aceptar propuesta");
            System.out.println("2. Rechazar propuesta");
            System.out.println("3. Solicitar modificaciones");
            System.out.println("4. Enviar nómina de participantes");
            System.out.println("5. Cancelar curso");
            System.out.println("0. Salir");
            System.out.print("Elegí una opción: ");

            try {
                opcion = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                opcion = -1;
            }

            switch (opcion) {

                case 1:
                    aceptarPropuesta(scanner, flowable);
                    break;

                case 2:
                    gestionarMensaje(scanner, flowable,
                            "RECHAZAR PROPUESTA", "Msg_rechaza");
                    break;

                case 3:
                    gestionarMensaje(scanner, flowable,
                            "SOLICITAR MODIFICACIONES", "Msg_mod");
                    break;

                case 4:
                    gestionarMensaje(scanner, flowable,
                            "ENVIAR NÓMINA", "Msg_nom");
                    break;

                case 5:
                    gestionarMensaje(scanner, flowable,
                            "CANCELAR CURSO", "Msg_cancel");
                    break;

                case 0:
                    System.out.println("Saliendo del simulador...");
                    break;

                default:
                    System.out.println("Opción inválida.");
            }

        } while (opcion != 0);

        scanner.close();
    }

    // Permite aceptar la propuesta y la fecha en una sola operación.
    private static void aceptarPropuesta(
            Scanner scanner,
            FlowableClient flowable) {

        System.out.println("\n=== ACEPTAR PROPUESTA ===");

        String executionId = seleccionarEjecucion(
                scanner, flowable, "Msg_acepta");

        if (executionId == null) {
            return;
        }

        String processInstanceId =
                flowable.obtenerInstanciaDeEjecucion(executionId);

        if (processInstanceId == null) {
            System.out.println(
                    "No se pudo identificar la instancia del curso.");
            return;
        }

        JsonNode variables =
                flowable.obtenerVariablesProceso(processInstanceId);

        if (variables == null) {
            System.out.println(
                    "No se pudieron consultar los datos de la propuesta.");
            return;
        }

        String tematica = flowable.obtenerValorVariable(
                variables, "tematica");

        String fechaInicio = flowable.obtenerValorVariable(
                variables, "fechaInicioPosible");

        String fechaFin = flowable.obtenerValorVariable(
                variables, "fechaFinPropuesta");

        if (fechaInicio == null || fechaFin == null) {
            System.out.println(
                    "La propuesta no tiene las fechas necesarias.");
            return;
        }

        LocalDate inicio;
        LocalDate fin;

        try {
            inicio = LocalDate.parse(fechaInicio);
            fin = LocalDate.parse(fechaFin);

        } catch (Exception e) {
            System.out.println(
                    "Las fechas de la propuesta tienen un formato inválido.");
            return;
        }

        if (fin.isBefore(inicio)) {
            System.out.println(
                    "La fecha de finalización es anterior al inicio.");
            return;
        }

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");

        System.out.println("\n===== DATOS DE LA PROPUESTA =====");

        System.out.println("Curso: " +
                (tematica == null ? "Sin temática" : tematica));

        System.out.println(
                "Fecha de inicio: " + inicio.format(formato));

        System.out.println(
                "Fecha de finalización: " + fin.format(formato));

        System.out.println("\nS. Aceptar propuesta y fechas");
        System.out.println("N. Rechazar propuesta");
        System.out.println("0. Volver al menú");

        System.out.print(
                "\n¿Aceptás el curso para estas fechas? (s/n/0): ");

        String respuesta = scanner.nextLine().trim();

        if (respuesta.equalsIgnoreCase("s")) {

            // Convertimos la fecha al formato ISO 8601
            // utilizado por el temporizador de Flowable.
            String fechaCurso = inicio
                    .atStartOfDay(
                            ZoneId.of("America/Argentina/Cordoba"))
                    .toOffsetDateTime()
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            boolean enviado = flowable.enviarMensajeConFecha(
                    executionId,
                    "Msg_acepta",
                    fechaCurso);

            if (enviado) {
                System.out.println(
                        "Propuesta aceptada correctamente.");

                System.out.println(
                        "Fecha acordada: " + inicio.format(formato));
            }

        } else if (respuesta.equalsIgnoreCase("n")) {

            // Buscamos la ejecución que espera el mensaje
            // de rechazo dentro de la misma instancia.
            String ejecucionRechazo =
                    flowable.buscarEjecucionPorInstancia(
                            "Msg_rechaza",
                            processInstanceId);

            if (ejecucionRechazo == null) {
                System.out.println(
                        "No se encontró una ejecución para rechazar la propuesta.");
                return;
            }

            flowable.enviarMensaje(
                    ejecucionRechazo, "Msg_rechaza");

        } else if (respuesta.equals("0")) {

            System.out.println("Volviendo al menú principal...");

        } else {

            System.out.println(
                    "Opción inválida. No se realizaron cambios.");
        }
    }

    // Permite seleccionar una ejecución del proceso.
    private static String seleccionarEjecucion(
            Scanner scanner,
            FlowableClient flowable,
            String mensaje) {

        List<String> ejecuciones =
                flowable.listarEjecuciones(mensaje);

        if (ejecuciones == null) {
            System.out.println(
                    "No se pudieron consultar las ejecuciones.");
            return null;
        }

        if (ejecuciones.isEmpty()) {
            System.out.println(
                    "No hay ejecuciones esperando el mensaje: "
                            + mensaje);
            return null;
        }

        System.out.println("\n0. Cancelar operación");
        System.out.print("Seleccioná una ejecución: ");

        String entrada = scanner.nextLine().trim();

        int opcion;

        try {
            opcion = Integer.parseInt(entrada);

        } catch (NumberFormatException e) {
            System.out.println(
                    "Debés ingresar un número válido.");
            return null;
        }

        if (opcion == 0) {
            System.out.println("Operación cancelada.");
            return null;
        }

        if (opcion < 1 || opcion > ejecuciones.size()) {
            System.out.println(
                    "La opción seleccionada no existe.");
            return null;
        }

        return ejecuciones.get(opcion - 1);
    }

    // Gestiona los demás mensajes del simulador.
    private static void gestionarMensaje(
            Scanner scanner,
            FlowableClient flowable,
            String titulo,
            String mensaje) {

        System.out.println("\n=== " + titulo + " ===");

        String executionId =
                seleccionarEjecucion(scanner, flowable, mensaje);

        if (executionId == null) {
            return;
        }

        System.out.print("¿Confirmás el envío? (s/n): ");

        String confirmacion = scanner.nextLine().trim();

        if (confirmacion.equalsIgnoreCase("s")) {

            flowable.enviarMensaje(executionId, mensaje);

        } else {

            System.out.println("Operación cancelada.");
        }
    }
}