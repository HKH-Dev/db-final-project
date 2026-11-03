package com.uniquindio.frutimarket.crud;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;

/** Traduce los errores de MariaDB (llaves, unicos, CHECK, triggers) a mensajes para el usuario. */
public final class ErroresBd {

    private static final Map<String, String> CHECKS = Map.ofEntries(
            Map.entry("ck_empleado_salario", "El salario no puede ser menor al salario mínimo 2026 ($ 1.750.905)."),
            Map.entry("ck_lote_cantidad", "La cantidad disponible debe estar entre 0 y la cantidad recibida."),
            Map.entry("ck_lote_fechas", "La fecha de vencimiento debe ser posterior a la de cosecha."),
            Map.entry("ck_suscripcion_fechas", "La fecha de fin debe ser posterior a la de inicio."),
            Map.entry("ck_plan_descuento", "El descuento del plan debe estar entre 0 % y 30 %."),
            Map.entry("ck_finca_altitud", "La altitud debe estar entre 0 y 4.000 m s. n. m."),
            Map.entry("ck_finca_area", "El área debe ser mayor que cero."),
            Map.entry("ck_producto_iva", "El IVA solo puede ser 0 %, 5 % o 19 %."),
            Map.entry("ck_producto_precio", "El precio debe ser mayor que cero."),
            Map.entry("ck_pago_origen", "Un pago debe ser de un pedido o de una suscripción."));

    private static final Pattern UNICO = Pattern.compile("Duplicate entry '(.*?)' for key '(.*?)'");
    private static final Pattern CHECK = Pattern.compile("CONSTRAINT `(.*?)` failed");
    private static final Pattern FK_HIJO = Pattern.compile("a foreign key constraint fails \\(`[^`]*`\\.`([^`]*)`");

    private ErroresBd() {
    }

    public static String mensaje(DataAccessException ex, boolean eliminando) {
        String m = ex.getMostSpecificCause().getMessage();
        if (m == null) {
            return "No se pudo guardar el registro.";
        }
        Matcher u = UNICO.matcher(m);
        if (u.find()) {
            return "Ya existe un registro con el valor «" + u.group(1) + "». Ese dato debe ser único.";
        }
        Matcher c = CHECK.matcher(m);
        if (c.find()) {
            return CHECKS.getOrDefault(c.group(1), "Un valor no cumple la regla de la base de datos " + c.group(1) + ".");
        }
        if (m.contains("foreign key constraint fails")) {
            if (eliminando) {
                Matcher f = FK_HIJO.matcher(m);
                String hijo = f.find() ? f.group(1).replace('_', ' ') : "otras tablas";
                return "No se puede eliminar: tiene registros asociados en «" + hijo + "». Puede marcarlo como inactivo.";
            }
            return "El registro relacionado que eligió no existe.";
        }
        if (m.contains("45000") || m.contains("Inventario insuficiente")) {
            return "Inventario insuficiente en el lote.";
        }
        return "Error de base de datos: " + m;
    }
}
