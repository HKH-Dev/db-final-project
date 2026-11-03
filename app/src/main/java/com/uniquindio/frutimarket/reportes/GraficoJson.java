package com.uniquindio.frutimarket.reportes;

import com.uniquindio.frutimarket.comun.Fmt;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Configuracion del grafico para Chart.js (static/js/app.js -> fmGrafico). */
final class GraficoJson {

    private GraficoJson() {
    }

    static String de(Grafico g, List<Map<String, Object>> filas) {
        String etiquetas = filas.stream().map(f -> texto(String.valueOf(f.get(g.etiqueta())))).collect(Collectors.joining(","));
        String series = g.series().stream().map(s -> "{\"nombre\":" + texto(s.nombre()) + ",\"datos\":["
                + filas.stream().map(f -> f.get(s.columna()) == null ? "0" : Fmt.numero(f.get(s.columna())).toPlainString()).collect(Collectors.joining(","))
                + "]}").collect(Collectors.joining(","));
        return "{\"tipo\":" + texto(g.tipoChartJs()) + ",\"horizontal\":" + (g.tipo() == Grafico.Tipo.BARRAS_H)
                + ",\"formato\":" + texto(g.formato().name()) + ",\"etiquetas\":[" + etiquetas + "],\"series\":[" + series + "]}";
    }

    private static String texto(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '<' -> b.append("\\u003c");
                case '\n' -> b.append("\\n");
                default -> b.append(c);
            }
        }
        return b.append('"').toString();
    }
}
