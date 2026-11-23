package simulador;

import java.util.Scanner;

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
                    gestionarMensaje(scanner, flowable,
                            "ACEPTAR PROPUESTA", "Msg_acepta");
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


    private static void gestionarMensaje(
        Scanner scanner,
        FlowableClient flowable,
        String titulo,
        String mensaje) {

        System.out.println("\n=== " + titulo + " ===");

        int cantidad = flowable.listarEjecuciones(mensaje);

        if (cantidad == -1) {
            System.out.println("No se pudo consultar las ejecuciones.");
            return;
        }

        if (cantidad == 0) {
            System.out.println(
                    "No hay ejecuciones esperando el mensaje: " + mensaje
            );
            return;
        }

        System.out.println("Ejecuciones encontradas: " + cantidad);

        System.out.print("Ingresá el ID de ejecución: ");
        String executionId = scanner.nextLine().trim();

        if (executionId.isEmpty()) {
            System.out.println("No ingresaste un ID.");
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

