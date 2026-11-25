package curso.flowable.service;

import org.springframework.stereotype.Service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class InformePdfService {

    // Generar un PDF a partir de un informe escrito.
    public String generarPdf(String informe) throws IOException {

        if (informe == null || informe.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe ingresar el informe de resultados.");
        }

        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            PDType1Font fuente = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA);

            PDType1Font fuenteTitulo = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD);

            String texto = informe.replace("\r", "")
                    .replace("\t", "    ");

            StringBuilder contenido = new StringBuilder();

            // Dividir el texto en lineas.
            for (String parrafo : texto.split("\n", -1)) {

                String linea = "";

                for (String palabra : parrafo.split(" ")) {

                    String candidata = linea.isEmpty()
                            ? palabra
                            : linea + " " + palabra;

                    if (candidata.length() > 75
                            && !linea.isEmpty()) {

                        contenido.append(linea).append("\n");
                        linea = palabra;

                    } else {
                        linea = candidata;
                    }
                }

                contenido.append(linea).append("\n");
            }

            String[] lineas = contenido.toString().split("\n", -1);

            int indice = 0;

            while (indice < lineas.length) {

                PDPage pagina = new PDPage();
                documento.addPage(pagina);

                try (PDPageContentStream flujo =
                             new PDPageContentStream(documento, pagina)) {

                    flujo.beginText();

                    flujo.setFont(fuenteTitulo, 16);
                    flujo.newLineAtOffset(50, 740);
                    flujo.showText("Informe de resultados del curso");

                    flujo.setFont(fuente, 11);
                    flujo.setLeading(15);
                    flujo.newLineAtOffset(0, -35);

                    int cantidadLineas = 0;

                    while (indice < lineas.length
                            && cantidadLineas < 42) {

                        flujo.showText(lineas[indice]);
                        flujo.newLine();

                        indice++;
                        cantidadLineas++;
                    }

                    flujo.endText();
                }
            }

            documento.save(salida);

            return Base64.getEncoder()
                    .encodeToString(salida.toByteArray());
        }
    }

    // Comprobar que el archivo adjunto sea un PDF valido.
    public String validarPdf(String pdfBase64) throws IOException {

        if (pdfBase64 == null || pdfBase64.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe adjuntar un archivo PDF.");
        }

        // Limitar el archivo a aproximadamente 5 MB.
        if (pdfBase64.length() > 7_000_000) {

            throw new IllegalArgumentException(
                    "El PDF no puede superar los 5 MB.");
        }

        byte[] archivo;

        try {
            archivo = Base64.getDecoder().decode(pdfBase64);

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "El archivo adjunto no es valido.");
        }

        if (archivo.length > 5 * 1024 * 1024) {

            throw new IllegalArgumentException(
                    "El PDF no puede superar los 5 MB.");
        }

        try (PDDocument documento = Loader.loadPDF(archivo)) {

            if (documento.getNumberOfPages() == 0) {

                throw new IllegalArgumentException(
                        "El PDF no contiene paginas.");
            }

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    "El archivo adjunto no es un PDF valido.");
        }

        return Base64.getEncoder().encodeToString(archivo);
    }
}