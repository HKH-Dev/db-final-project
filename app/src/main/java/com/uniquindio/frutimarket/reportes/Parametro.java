package com.uniquindio.frutimarket.reportes;

import java.util.List;

/**
 * Filtro de una consulta o reporte. defecto admite: ANIO_ACTUAL, HOY, INICIO_ANIO, MES_ACTUAL o un valor fijo.
 * Para tipo SQL, sqlOpciones devuelve (valor, etiqueta).
 */
public record Parametro(String nombre, String etiqueta, Tipo tipo, String defecto, List<String> opciones, String sqlOpciones, boolean todos) {

    public enum Tipo { ENTERO, FECHA, TEXTO, OPCIONES, SQL }

    public static Parametro entero(String n, String e, String def) {
        return new Parametro(n, e, Tipo.ENTERO, def, List.of(), null, false);
    }

    public static Parametro fecha(String n, String e, String def) {
        return new Parametro(n, e, Tipo.FECHA, def, List.of(), null, false);
    }

    public static Parametro texto(String n, String e, String def) {
        return new Parametro(n, e, Tipo.TEXTO, def, List.of(), null, false);
    }

    /** Lista fija; con todos=true se agrega la opcion «Todos» (valor vacio) */
    public static Parametro opciones(String n, String e, boolean todos, String... ops) {
        return new Parametro(n, e, Tipo.OPCIONES, todos ? "" : ops[0], List.of(ops), null, todos);
    }

    public static Parametro sql(String n, String e, String sql, boolean todos) {
        return new Parametro(n, e, Tipo.SQL, "", List.of(), sql, todos);
    }

    public String inputType() {
        return switch (tipo) {
            case ENTERO -> "number";
            case FECHA -> "date";
            default -> "text";
        };
    }
}
