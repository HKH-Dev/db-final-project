package com.uniquindio.frutimarket.comun;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Formato colombiano para las plantillas: ${@fmt.moneda(x)}, ${@fmt.valor(x, col.formato)}. */
@Component("fmt")
public class Fmt {

    private static final Locale CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", CO);
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", CO);

    private static DecimalFormat patron(String p) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(CO);
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return new DecimalFormat(p, s);
    }

    public String valor(Object v, Formato f) {
        if (v == null) {
            return "—";
        }
        return switch (f) {
            case MONEDA -> moneda(v);
            case ENTERO -> patron("#,##0").format(numero(v));
            case NUMERO -> patron("#,##0.##").format(numero(v));
            case KG -> patron("#,##0.0").format(numero(v)) + " kg";
            case PORCENTAJE -> patron("#,##0.0").format(numero(v)) + " %";
            case FECHA -> fecha(v);
            case FECHA_HORA -> fechaHora(v);
            case BOOLEANO -> siNo(v);
            case ESTADO -> etiqueta(String.valueOf(v));
            case TEXTO -> String.valueOf(v);
        };
    }

    public String moneda(Object v) {
        if (v == null) {
            return "—";
        }
        return "$ " + patron("#,##0").format(numero(v));
    }

    public String porcentaje(Object v) {
        return valor(v, Formato.PORCENTAJE);
    }

    public String kg(Object v) {
        return valor(v, Formato.KG);
    }

    public String entero(Object v) {
        return valor(v, Formato.ENTERO);
    }

    public String fecha(Object v) {
        if (v == null) {
            return "—";
        }
        if (v instanceof LocalDate d) {
            return d.format(FECHA);
        }
        if (v instanceof Date d) {
            return d.toLocalDate().format(FECHA);
        }
        if (v instanceof LocalDateTime d) {
            return d.format(FECHA);
        }
        if (v instanceof Timestamp t) {
            return t.toLocalDateTime().format(FECHA);
        }
        return String.valueOf(v);
    }

    public String fechaHora(Object v) {
        if (v instanceof LocalDateTime d) {
            return d.format(FECHA_HORA);
        }
        if (v instanceof Timestamp t) {
            return t.toLocalDateTime().format(FECHA_HORA);
        }
        return fecha(v);
    }

    public String fechaLarga(Object v) {
        LocalDate d = v instanceof LocalDate x ? x
                : v instanceof Date x ? x.toLocalDate()
                : v instanceof LocalDateTime x ? x.toLocalDate()
                : v instanceof Timestamp x ? x.toLocalDateTime().toLocalDate() : LocalDate.now();
        return d.format(FECHA_LARGA);
    }

    public String hoy() {
        return LocalDate.now().format(FECHA_LARGA);
    }

    public String siNo(Object v) {
        return verdadero(v) ? "Sí" : "No";
    }

    public boolean si(Object v) {
        return verdadero(v);
    }

    public static boolean verdadero(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        return v != null && ("1".equals(v.toString()) || "true".equalsIgnoreCase(v.toString()));
    }

    /** EN_RUTA -> En ruta */
    public String etiqueta(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "—";
        }
        String t = codigo.replace('_', ' ').toLowerCase(CO);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Clase CSS para el estado: estado-pagado, estado-en-ruta... */
    public String claseEstado(Object codigo) {
        return codigo == null ? "" : "estado-" + codigo.toString().toLowerCase(CO).replace('_', '-');
    }

    public static BigDecimal numero(Object v) {
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
