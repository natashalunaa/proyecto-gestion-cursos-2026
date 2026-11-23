import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ProbarConexion {
    public static void main(String[] args) throws Exception {
        URL url = new URL(
            "http://host.docker.internal:8081/api/interno/empresas/habilitar-acceso"
        );

        HttpURLConnection conexion =
            (HttpURLConnection) url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setRequestProperty("Content-Type", "application/json");
        conexion.setDoOutput(true);
        conexion.setConnectTimeout(5000);
        conexion.setReadTimeout(5000);

        byte[] datos = "{}".getBytes(StandardCharsets.UTF_8);
        conexion.getOutputStream().write(datos);

        System.out.println("Codigo HTTP: " + conexion.getResponseCode());

        conexion.disconnect();
    }
}
