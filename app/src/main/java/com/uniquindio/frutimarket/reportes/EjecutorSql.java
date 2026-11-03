package com.uniquindio.frutimarket.reportes;

import com.uniquindio.frutimarket.comun.Columna;
import com.uniquindio.frutimarket.comun.Fmt;
import com.uniquindio.frutimarket.crud.Opcion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/** Resuelve los filtros y ejecuta el SQL de reportes y consultas. */
@Service
public class EjecutorSql {

    private final NamedParameterJdbcTemplate jdbc;

    public EjecutorSql(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Filtro listo para pintarse en el formulario */
    public record Filtro(Parametro p, String valor, List<Opcion> opciones) {
    }

    public List<Filtro> filtros(List<Parametro> params, Map<String, String> entrada) {
        List<Filtro> r = new ArrayList<>();
        for (Parametro p : params) {
            List<Opcion> ops = opciones(p);
            String v = entrada.get(p.nombre());
            if (v == null || (v.isBlank() && !p.todos())) {
                v = valorDefecto(p, ops);
            }
            r.add(new Filtro(p, v.trim(), ops));
        }
        return r;
    }

    public MapSqlParameterSource parametros(List<Filtro> filtros) {
        MapSqlParameterSource s = new MapSqlParameterSource();
        for (Filtro f : filtros) {
            Object v = switch (f.p().tipo()) {
                case ENTERO -> {
                    try {
                        yield Long.parseLong(f.valor());
                    } catch (NumberFormatException e) {
                        yield 0L;
                    }
                }
                case FECHA -> {
                    try {
                        yield LocalDate.parse(f.valor());
                    } catch (DateTimeParseException e) {
                        yield LocalDate.now();
                    }
                }
                default -> f.valor();
            };
            s.addValue(f.p().nombre(), v);
        }
        return s;
    }

    public List<Map<String, Object>> ejecutar(String sql, MapSqlParameterSource p) {
        return jdbc.queryForList(sql, p);
    }

    public Map<String, BigDecimal> totales(List<Columna> columnas, List<Map<String, Object>> filas) {
        Map<String, BigDecimal> t = new LinkedHashMap<>();
        for (Columna c : columnas) {
            if (c.total()) {
                BigDecimal s = BigDecimal.ZERO;
                for (Map<String, Object> f : filas) {
                    Object v = f.get(c.clave());
                    if (v != null) {
                        s = s.add(Fmt.numero(v));
                    }
                }
                t.put(c.clave(), s);
            }
        }
        return t;
    }

    /** Texto legible de los filtros aplicados (para el encabezado del PDF) */
    public String resumenFiltros(List<Filtro> filtros) {
        List<String> partes = new ArrayList<>();
        for (Filtro f : filtros) {
            String etq = f.valor();
            for (Opcion o : f.opciones()) {
                if (o.id().equals(f.valor())) {
                    etq = o.etiqueta();
                }
            }
            if (f.valor().isBlank()) {
                etq = "Todos";
            }
            if (f.p().tipo() == Parametro.Tipo.FECHA) {
                try {
                    etq = new Fmt().fecha(LocalDate.parse(f.valor()));
                } catch (DateTimeParseException ignorada) {
                    // se deja el texto tal cual
                }
            }
            partes.add(f.p().etiqueta() + ": " + etq);
        }
        return String.join("  ·  ", partes);
    }

    private List<Opcion> opciones(Parametro p) {
        return switch (p.tipo()) {
            case OPCIONES -> p.opciones().stream().map(o -> {
                String[] x = o.split("\\|", 2);
                return new Opcion(x[0], x.length > 1 ? x[1] : new Fmt().etiqueta(x[0]));
            }).toList();
            case SQL -> jdbc.getJdbcTemplate().query(p.sqlOpciones(), (rs, i) -> new Opcion(rs.getString(1), rs.getString(2)));
            default -> List.of();
        };
    }

    private String valorDefecto(Parametro p, List<Opcion> ops) {
        String d = p.defecto() == null ? "" : p.defecto();
        LocalDate hoy = LocalDate.now();
        return switch (d) {
            case "ANIO_ACTUAL" -> String.valueOf(hoy.getYear());
            case "HOY" -> hoy.toString();
            case "INICIO_ANIO" -> hoy.withDayOfYear(1).toString();
            case "MAX_PEDIDO" -> String.valueOf(jdbc.getJdbcTemplate().queryForObject("SELECT COALESCE(MAX(id_pedido), 0) FROM pedido", Long.class));
            case "PRIMER_LOTE" -> jdbc.getJdbcTemplate().query(
                    "SELECT codigo FROM lote WHERE estado = 'AGOTADO' ORDER BY id_lote DESC LIMIT 1", (rs, i) -> rs.getString(1))
                    .stream().findFirst().orElse("");
            default -> {
                if (d.isEmpty() && !p.todos() && !ops.isEmpty()) {
                    yield ops.getFirst().id();
                }
                yield d.contains("|") ? d.split("\\|")[0] : d;
            }
        };
    }
}
