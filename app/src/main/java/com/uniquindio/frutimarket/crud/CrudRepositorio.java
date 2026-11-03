package com.uniquindio.frutimarket.crud;

import com.uniquindio.frutimarket.comun.Columna;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * SQL generico para las entidades del catalogo. Los nombres de tabla y columna salen siempre
 * de CatalogoEntidades (nunca del usuario); los valores van como parametros (:nombre).
 */
@Repository
public class CrudRepositorio {

    private final NamedParameterJdbcTemplate jdbc;

    public CrudRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Pagina(List<Map<String, Object>> filas, long total, int pagina, int tamano) {
        public int paginas() {
            return (int) Math.max(1, (total + tamano - 1) / tamano);
        }

        public int desde() {
            return Math.max(1, pagina - 2);
        }

        public int hasta() {
            return Math.min(paginas(), pagina + 2);
        }
    }

    public Pagina listar(Entidad e, String busqueda, String orden, boolean desc, int pagina, int tamano) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = "";
        if (busqueda != null && !busqueda.isBlank()) {
            String cols = e.getColumnas().stream().map(c -> "t." + c.clave()).collect(Collectors.joining(", "));
            where = " WHERE CONCAT_WS(' ', t.id, " + cols + ") LIKE :q";
            p.addValue("q", "%" + busqueda.trim() + "%");
        }
        String base = "SELECT * FROM (" + e.getSqlLista() + ") t" + where;
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + base + ") x", p, Long.class);
        String ord = e.getOrdenDefecto();
        if (orden != null && (orden.equals("id") || e.getColumnas().stream().map(Columna::clave).anyMatch(orden::equals))) {
            ord = orden + (desc ? " DESC" : " ASC");
        }
        p.addValue("lim", tamano).addValue("off", (long) Math.max(0, pagina - 1) * tamano);
        List<Map<String, Object>> filas = jdbc.queryForList(base + " ORDER BY " + ord + " LIMIT :lim OFFSET :off", p);
        return new Pagina(filas, total, pagina, tamano);
    }

    public Optional<Map<String, Object>> fila(Entidad e, long id) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM (" + e.getSqlLista() + ") t WHERE t.id = :id", Map.of("id", id));
        return r.stream().findFirst();
    }

    public Optional<Map<String, Object>> registro(Entidad e, long id) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM " + e.getTabla() + " WHERE " + e.getPk() + " = :id", Map.of("id", id));
        return r.stream().findFirst();
    }

    public long insertar(Entidad e, Map<String, Object> valores) {
        String cols = String.join(", ", valores.keySet());
        String params = valores.keySet().stream().map(k -> ":" + k).collect(Collectors.joining(", "));
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO " + e.getTabla() + " (" + cols + ") VALUES (" + params + ")",
                new MapSqlParameterSource(valores), kh, new String[] {e.getPk()});
        Number n = kh.getKey();
        return n == null ? 0 : n.longValue();
    }

    public void actualizar(Entidad e, long id, Map<String, Object> valores) {
        String set = valores.keySet().stream().map(k -> k + " = :" + k).collect(Collectors.joining(", "));
        MapSqlParameterSource p = new MapSqlParameterSource(valores).addValue("__id", id);
        jdbc.update("UPDATE " + e.getTabla() + " SET " + set + " WHERE " + e.getPk() + " = :__id", p);
    }

    public void eliminar(Entidad e, long id) {
        jdbc.update("DELETE FROM " + e.getTabla() + " WHERE " + e.getPk() + " = :id", Map.of("id", id));
    }

    public Map<String, List<Opcion>> opciones(Entidad e) {
        Map<String, List<Opcion>> m = new LinkedHashMap<>();
        for (Campo c : e.getCampos()) {
            if (c.tipo() == Campo.Tipo.FK) {
                m.put(c.columna(), jdbc.getJdbcTemplate().query(c.sqlOpciones(),
                        (rs, i) -> new Opcion(rs.getString(1), rs.getString(2))));
            }
        }
        return m;
    }
}
