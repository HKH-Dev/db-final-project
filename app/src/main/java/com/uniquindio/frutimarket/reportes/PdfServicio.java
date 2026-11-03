package com.uniquindio.frutimarket.reportes;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.uniquindio.frutimarket.comun.Fmt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Llena una plantilla Thymeleaf de templates/pdf/ y la convierte en PDF. */
@Service
public class PdfServicio {

    private final TemplateEngine plantillas;
    private final Fmt fmt;
    private final File fuenteNormal;
    private final File fuenteNegrita;

    public PdfServicio(TemplateEngine plantillas, Fmt fmt) {
        this.plantillas = plantillas;
        this.fmt = fmt;
        this.fuenteNormal = copiarFuente("fonts/DejaVuSans.ttf");
        this.fuenteNegrita = copiarFuente("fonts/DejaVuSans-Bold.ttf");
    }

    public byte[] generar(String plantilla, Map<String, Object> datos) {
        Context ctx = new Context(java.util.Locale.forLanguageTag("es-CO"));
        ctx.setVariables(datos);
        ctx.setVariable("fmt", fmt);
        String html = plantillas.process(plantilla, ctx);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            if (fuenteNormal != null) {
                b.useFont(fuenteNormal, "DejaVu Sans", 400, PdfRendererBuilder.FontStyle.NORMAL, true);
            }
            if (fuenteNegrita != null) {
                b.useFont(fuenteNegrita, "DejaVu Sans", 700, PdfRendererBuilder.FontStyle.NORMAL, true);
            }
            b.withHtmlContent(html, null);
            b.toStream(os);
            b.run();
            return os.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    /** openhtmltopdf necesita un archivo; la fuente va dentro del jar */
    private static File copiarFuente(String ruta) {
        ClassPathResource r = new ClassPathResource(ruta);
        if (!r.exists()) {
            return null;
        }
        try (InputStream in = r.getInputStream()) {
            File f = File.createTempFile("frutimarket-", ".ttf");
            f.deleteOnExit();
            Files.copy(in, f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return f;
        } catch (IOException e) {
            return null;
        }
    }
}
