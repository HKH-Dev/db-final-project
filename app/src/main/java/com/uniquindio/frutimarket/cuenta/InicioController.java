package com.uniquindio.frutimarket.cuenta;

import com.uniquindio.frutimarket.config.UsuarioSesion;
import com.uniquindio.frutimarket.reportes.CatalogoReportes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Tablero de inicio segun el rol. */
@Controller
public class InicioController {

    private final JdbcTemplate jdbc;
    private final CatalogoReportes reportes;

    public InicioController(JdbcTemplate jdbc, CatalogoReportes reportes) {
        this.jdbc = jdbc;
        this.reportes = reportes;
    }

    @GetMapping("/")
    public String inicio(@AuthenticationPrincipal UsuarioSesion u, Model model) {
        if (u.esCliente()) {
            return inicioCliente(u, model);
        }
        if (u.esCampesino()) {
            return "redirect:/mis-lotes";
        }
        model.addAttribute("kpi", jdbc.queryForMap("""
                SELECT (SELECT COALESCE(SUM(total), 0) FROM pedido WHERE estado <> 'CANCELADO'
                          AND fecha_pedido >= DATE_FORMAT(CURDATE(), '%Y-%m-01')) AS ventas_mes,
                       (SELECT COALESCE(SUM(total), 0) FROM pedido WHERE estado <> 'CANCELADO'
                          AND fecha_pedido >= DATE_FORMAT(CURDATE() - INTERVAL 1 MONTH, '%Y-%m-01')
                          AND fecha_pedido < DATE_FORMAT(CURDATE(), '%Y-%m-01')) AS ventas_mes_anterior,
                       (SELECT COUNT(*) FROM pedido WHERE fecha_pedido >= CURDATE() - INTERVAL 30 DAY AND estado <> 'CANCELADO') AS pedidos_30,
                       (SELECT COUNT(*) FROM pedido WHERE estado IN ('PENDIENTE', 'PAGADO', 'DESPACHADO')) AS pedidos_abiertos,
                       (SELECT COALESCE(SUM(cantidad_disponible_kg), 0) FROM lote WHERE estado = 'DISPONIBLE' AND fecha_vencimiento >= CURDATE()) AS kg_inventario,
                       (SELECT COUNT(*) FROM lote WHERE estado = 'DISPONIBLE' AND cantidad_disponible_kg > 0
                          AND fecha_vencimiento BETWEEN CURDATE() AND CURDATE() + INTERVAL 3 DAY) AS lotes_por_vencer,
                       (SELECT COUNT(*) FROM suscripcion WHERE estado = 'ACTIVA' AND fecha_fin >= CURDATE()) AS planes_activos,
                       (SELECT COUNT(*) FROM cliente WHERE activo = 1) AS clientes"""));
        model.addAttribute("serie", jdbc.queryForList("""
                SELECT DATE_FORMAT(fecha_pedido, '%Y-%m-%d') AS dia, CAST(SUM(total) AS DOUBLE) AS ventas, COUNT(*) AS pedidos
                  FROM pedido WHERE estado <> 'CANCELADO' AND fecha_pedido >= CURDATE() - INTERVAL 29 DAY
                 GROUP BY DATE_FORMAT(fecha_pedido, '%Y-%m-%d') ORDER BY dia"""));
        model.addAttribute("ultimos", jdbc.queryForList("""
                SELECT p.id_pedido, p.fecha_pedido, c.nombre AS cliente, p.estado, p.total
                  FROM pedido p JOIN cliente c ON c.id_cliente = p.id_cliente ORDER BY p.fecha_pedido DESC LIMIT 8"""));
        model.addAttribute("topProductos", jdbc.queryForList("""
                SELECT CONCAT(pr.nombre, ' ', pr.variedad) AS producto, SUM(d.cantidad_kg) AS kg, SUM(d.subtotal) AS ventas
                  FROM detalle_pedido d JOIN pedido p ON p.id_pedido = d.id_pedido JOIN lote l ON l.id_lote = d.id_lote
                  JOIN producto pr ON pr.id_producto = l.id_producto
                 WHERE p.estado <> 'CANCELADO' AND p.fecha_pedido >= CURDATE() - INTERVAL 30 DAY
                 GROUP BY pr.id_producto, pr.nombre, pr.variedad ORDER BY ventas DESC LIMIT 5"""));
        model.addAttribute("reportes", reportes.todos());
        return "inicio";
    }

    private String inicioCliente(UsuarioSesion u, Model model) {
        if (u.idCliente() == null) {
            return "inicio-cliente";
        }
        model.addAttribute("plan", jdbc.queryForList("""
                SELECT s.id_suscripcion, s.fecha_inicio, s.fecha_fin, s.estado, pl.nombre, pl.descuento_pct, pl.kg_incluidos
                  FROM suscripcion s JOIN plan pl ON pl.id_plan = s.id_plan
                 WHERE s.id_cliente = ? AND s.estado IN ('ACTIVA', 'PAUSADA') AND s.fecha_fin >= CURDATE()
                 ORDER BY s.fecha_fin DESC LIMIT 1""", u.idCliente()).stream().findFirst().orElse(null));
        model.addAttribute("resumen", jdbc.queryForMap("""
                SELECT COUNT(*) AS pedidos, COALESCE(SUM(total), 0) AS total, COALESCE(SUM(descuento), 0) AS ahorro
                  FROM pedido WHERE id_cliente = ? AND estado <> 'CANCELADO' AND YEAR(fecha_pedido) = YEAR(CURDATE())""", u.idCliente()));
        model.addAttribute("pedidos", jdbc.queryForList("""
                SELECT id_pedido, fecha_pedido, estado, total FROM pedido WHERE id_cliente = ? ORDER BY fecha_pedido DESC LIMIT 5""", u.idCliente()));
        model.addAttribute("avisos", jdbc.queryForList("""
                SELECT titulo, mensaje, fecha_programada, tipo FROM notificacion
                 WHERE id_usuario = ? AND leida = 0 AND fecha_programada <= NOW() ORDER BY fecha_programada DESC LIMIT 4""", u.id()));
        return "inicio-cliente";
    }
}
