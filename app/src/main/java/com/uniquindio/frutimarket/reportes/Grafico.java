package com.uniquindio.frutimarket.reportes;

import com.uniquindio.frutimarket.comun.Formato;
import java.util.List;

/** Grafico estadistico de un reporte: la columna etiqueta va en el eje X y cada serie es una columna numerica. */
public record Grafico(Tipo tipo, String titulo, String etiqueta, List<Serie> series, Formato formato) {

    public enum Tipo { BARRAS, BARRAS_H, LINEA, TORTA, DONA }

    public record Serie(String columna, String nombre) {
    }

    public static Grafico de(Tipo tipo, String titulo, String etiqueta, Formato formato, String... columnaNombre) {
        List<Serie> s = new java.util.ArrayList<>();
        for (int i = 0; i < columnaNombre.length; i += 2) {
            s.add(new Serie(columnaNombre[i], columnaNombre[i + 1]));
        }
        return new Grafico(tipo, titulo, etiqueta, s, formato);
    }

    public String tipoChartJs() {
        return switch (tipo) {
            case BARRAS, BARRAS_H -> "bar";
            case LINEA -> "line";
            case TORTA -> "pie";
            case DONA -> "doughnut";
        };
    }

    public boolean circular() {
        return tipo == Tipo.TORTA || tipo == Tipo.DONA;
    }
}
