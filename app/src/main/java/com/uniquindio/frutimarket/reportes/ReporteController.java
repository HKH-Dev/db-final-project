package com.uniquindio.frutimarket.reportes;

import com.uniquindio.frutimarket.config.UsuarioSesion;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final CatalogoReportes catalogo;
    private final EjecutorSql ejecutor;
    private final GraficoServicio graficos;
    private final PdfServicio pdf;

    public ReporteController(CatalogoReportes catalogo, EjecutorSql ejecutor, GraficoServicio graficos, PdfServicio pdf) {
        this.catalogo = catalogo;
        this.ejecutor = ejecutor;
        this.graficos = graficos;
        this.pdf = pdf;
    }

    @GetMapping
    public String indice(Model model) {
        model.addAttribute("reportes", catalogo.todos());
        model.addAttribute("niveles", Reporte.Nivel.values());
        return "reportes/indice";
    }

    @GetMapping("/{clave}")
    public String ver(@PathVariable String clave, @RequestParam Map<String, String> params, Model model) {
        model.addAllAttributes(datos(clave, params));
        return "reportes/ver";
    }

    @GetMapping("/{clave}/pdf")
    public ResponseEntity<byte[]> exportar(@PathVariable String clave, @RequestParam Map<String, String> params,
                                           @AuthenticationPrincipal UsuarioSesion u) {
        Map<String, Object> d = datos(clave, params);
        Reporte r = (Reporte) d.get("r");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filas = (List<Map<String, Object>>) d.get("filas");
        if (r.grafico() != null && !filas.isEmpty()) {
            // en hoja horizontal (mas de 7 columnas) el grafico es mas bajo para que quepa en la primera pagina
            boolean horizontal = r.columnas().size() > 7;
            int ancho = horizontal ? 1100 : (r.grafico().circular() ? 620 : 720);
            int alto = horizontal ? 290 : (r.grafico().circular() ? 300 : 330);
            d.put("imagenGrafico", graficos.png(r.grafico(), filas, ancho, alto));
        }
        d.put("generadoPor", u.nombre());
        d.put("generado", LocalDateTime.now());
        byte[] archivo = pdf.generar("pdf/reporte", d);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("reporte-" + r.numero() + "-" + r.clave() + ".pdf").build().toString())
                .body(archivo);
    }

    private Map<String, Object> datos(String clave, Map<String, String> params) {
        Reporte r = catalogo.buscar(clave).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<EjecutorSql.Filtro> filtros = ejecutor.filtros(r.parametros(), params);
        List<Map<String, Object>> filas = ejecutor.ejecutar(r.sql(), ejecutor.parametros(filtros));
        Map<String, Object> m = new HashMap<>();
        m.put("r", r);
        m.put("filtros", filtros);
        m.put("resumenFiltros", ejecutor.resumenFiltros(filtros));
        m.put("filas", filas);
        m.put("totales", ejecutor.totales(r.columnas(), filas));
        if (r.grafico() != null) {
            m.put("graficoJson", GraficoJson.de(r.grafico(), filas));
        }
        m.put("consulta", filtros.stream().map(f -> f.p().nombre() + "=" + java.net.URLEncoder.encode(f.valor(), java.nio.charset.StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b).orElse(""));
        return m;
    }
}
