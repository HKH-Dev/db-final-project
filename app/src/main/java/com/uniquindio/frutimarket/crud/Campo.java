package com.uniquindio.frutimarket.crud;

import java.util.List;

/** Campo editable de un formulario de registro / modificacion. */
public record Campo(String columna, String etiqueta, Tipo tipo, boolean requerido, int max, List<String> opciones,
                    String sqlOpciones, String ayuda, Double minimo, boolean ancho) {

    public enum Tipo { TEXTO, TEXTO_LARGO, EMAIL, TELEFONO, ENTERO, DECIMAL, MONEDA, FECHA, FECHA_HORA, BOOLEANO, OPCIONES, FK, CLAVE }

    public static Campo texto(String col, String etq, int max) {
        return new Campo(col, etq, Tipo.TEXTO, true, max, List.of(), null, null, null, false);
    }

    public static Campo largo(String col, String etq, int max) {
        return new Campo(col, etq, Tipo.TEXTO_LARGO, true, max, List.of(), null, null, null, true);
    }

    public static Campo email(String col, String etq) {
        return new Campo(col, etq, Tipo.EMAIL, true, 120, List.of(), null, null, null, false);
    }

    public static Campo telefono(String col, String etq) {
        return new Campo(col, etq, Tipo.TELEFONO, true, 20, List.of(), null, "10 dígitos: celular o fijo 606", null, false);
    }

    public static Campo entero(String col, String etq) {
        return new Campo(col, etq, Tipo.ENTERO, true, 0, List.of(), null, null, 0d, false);
    }

    public static Campo decimal(String col, String etq) {
        return new Campo(col, etq, Tipo.DECIMAL, true, 0, List.of(), null, null, 0d, false);
    }

    public static Campo moneda(String col, String etq) {
        return new Campo(col, etq, Tipo.MONEDA, true, 0, List.of(), null, "Pesos colombianos, sin puntos", 0d, false);
    }

    public static Campo fecha(String col, String etq) {
        return new Campo(col, etq, Tipo.FECHA, true, 0, List.of(), null, null, null, false);
    }

    public static Campo fechaHora(String col, String etq) {
        return new Campo(col, etq, Tipo.FECHA_HORA, true, 0, List.of(), null, null, null, false);
    }

    public static Campo booleano(String col, String etq) {
        return new Campo(col, etq, Tipo.BOOLEANO, false, 0, List.of(), null, null, null, false);
    }

    public static Campo opciones(String col, String etq, String... ops) {
        return new Campo(col, etq, Tipo.OPCIONES, true, 0, List.of(ops), null, null, null, false);
    }

    /** Llave foranea: el SQL debe devolver (id, etiqueta). */
    public static Campo fk(String col, String etq, String sql) {
        return new Campo(col, etq, Tipo.FK, true, 0, List.of(), sql, null, null, false);
    }

    public static Campo clave(String col, String etq) {
        return new Campo(col, etq, Tipo.CLAVE, true, 72, List.of(), null, "Mínimo 8 caracteres. Al editar, déjela vacía para conservar la actual", null, false);
    }

    public Campo opcional() {
        return new Campo(columna, etiqueta, tipo, false, max, opciones, sqlOpciones, ayuda, minimo, ancho);
    }

    public Campo ayuda(String texto) {
        return new Campo(columna, etiqueta, tipo, requerido, max, opciones, sqlOpciones, texto, minimo, ancho);
    }

    public Campo minimo(double m) {
        return new Campo(columna, etiqueta, tipo, requerido, max, opciones, sqlOpciones, ayuda, m, ancho);
    }

    public String inputType() {
        return switch (tipo) {
            case EMAIL -> "email";
            case TELEFONO -> "tel";
            case ENTERO, DECIMAL, MONEDA -> "number";
            case FECHA -> "date";
            case FECHA_HORA -> "datetime-local";
            case CLAVE -> "password";
            default -> "text";
        };
    }

    public String paso() {
        return switch (tipo) {
            case DECIMAL -> "0.01";
            default -> "1";
        };
    }
}
