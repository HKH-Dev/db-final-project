package com.uniquindio.frutimarket.reportes;

import com.uniquindio.frutimarket.comun.Columna;
import java.util.List;

public record Reporte(String clave, int numero, Nivel nivel, String titulo, String descripcion, String decision,
                      List<String> tablas, String tecnicas, String sql, List<Parametro> parametros,
                      List<Columna> columnas, Grafico grafico) {

    public enum Nivel {
        SIMPLE("Simple", "Consulta a una sola tabla"),
        INTERMEDIO("Intermedio", "Multitabla con JOIN, WHERE, GROUP BY y HAVING"),
        COMPLEJO("Complejo", "Con subconsultas");

        private final String nombre;
        private final String regla;

        Nivel(String nombre, String regla) {
            this.nombre = nombre;
            this.regla = regla;
        }

        public String getNombre() { return nombre; }
        public String getRegla() { return regla; }
    }

    public boolean tieneTotales() {
        return columnas.stream().anyMatch(Columna::total);
    }
}
