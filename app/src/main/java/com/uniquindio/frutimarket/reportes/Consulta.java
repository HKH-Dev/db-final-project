package com.uniquindio.frutimarket.reportes;

import com.uniquindio.frutimarket.comun.Columna;
import java.util.List;

/** Consulta puntual: datos especificos que se quieren saber en un momento (p. ej. lo que compro un cliente). */
public record Consulta(String clave, String titulo, String descripcion, String icono, List<Parametro> parametros, List<Seccion> secciones) {

    /** ficha=true muestra la primera fila como tarjeta de datos en vez de tabla */
    public record Seccion(String titulo, String sql, List<Columna> columnas, boolean ficha) {
    }
}
