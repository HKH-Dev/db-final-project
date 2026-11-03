package com.uniquindio.frutimarket.cuenta;

import com.uniquindio.frutimarket.config.UsuarioSesion;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/** Portal del campesino: sus fincas, sus lotes y lo que se le ha liquidado. */
@Controller
public class MisLotesController {

    private final JdbcTemplate jdbc;

    public MisLotesController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/mis-lotes")
    public String misLotes(@AuthenticationPrincipal UsuarioSesion u, Model model) {
        if (u.idCampesino() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        int c = u.idCampesino();
        model.addAttribute("resumen", jdbc.queryForMap("""
                SELECT COUNT(l.id_lote) AS lotes, COALESCE(SUM(l.cantidad_kg), 0) AS kg,
                       COALESCE(SUM(l.cantidad_kg * l.precio_finca_kg), 0) AS liquidado,
                       COALESCE(SUM(l.cantidad_kg - l.cantidad_disponible_kg), 0) AS kg_vendidos
                  FROM lote l JOIN finca f ON f.id_finca = l.id_finca WHERE f.id_campesino = ? AND YEAR(l.fecha_cosecha) = YEAR(CURDATE())""", c));
        model.addAttribute("fincas", jdbc.queryForList("""
                SELECT f.nombre, f.vereda, m.nombre AS municipio, f.area_hectareas, f.altitud_msnm FROM finca f
                  JOIN municipio m ON m.id_municipio = f.id_municipio WHERE f.id_campesino = ?""", c));
        model.addAttribute("lotes", jdbc.queryForList("""
                SELECT l.codigo, CONCAT(pr.nombre, ' ', pr.variedad) AS producto, f.nombre AS finca, pa.nombre AS punto, l.fecha_cosecha,
                       l.cantidad_kg, l.cantidad_kg - l.cantidad_disponible_kg AS vendido, l.precio_finca_kg,
                       ROUND(l.cantidad_kg * l.precio_finca_kg) AS valor, l.calidad, l.estado
                  FROM lote l JOIN finca f ON f.id_finca = l.id_finca JOIN producto pr ON pr.id_producto = l.id_producto
                  JOIN punto_acopio pa ON pa.id_punto = l.id_punto WHERE f.id_campesino = ? ORDER BY l.fecha_cosecha DESC LIMIT 60""", c));
        return "cuenta/mis-lotes";
    }
}
