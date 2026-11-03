package com.uniquindio.frutimarket.comun;

/** Columna visible de una tabla de resultados (listados CRUD, consultas y reportes). */
public record Columna(String clave, String etiqueta, Formato formato, boolean total) {

    public static Columna de(String clave, String etiqueta, Formato formato) {
        return new Columna(clave, etiqueta, formato, false);
    }

    public static Columna texto(String clave, String etiqueta) {
        return new Columna(clave, etiqueta, Formato.TEXTO, false);
    }

    /** Columna que se suma en la fila de totales. */
    public Columna conTotal() {
        return new Columna(clave, etiqueta, formato, true);
    }

    public boolean numerica() {
        return switch (formato) {
            case ENTERO, NUMERO, KG, MONEDA, PORCENTAJE -> true;
            default -> false;
        };
    }
}
