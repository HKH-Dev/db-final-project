package com.uniquindio.frutimarket.cuenta;

import com.uniquindio.frutimarket.config.UsuarioSesion;
import com.uniquindio.frutimarket.ventas.PedidoServicio;
import com.uniquindio.frutimarket.ventas.PedidoServicio.ReglaException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Planes de suscripcion para el cliente: ver, contratar con pago en linea y cancelar. */
@Controller
public class PlanController {

    private final JdbcTemplate jdbc;
    private final PedidoServicio servicio;

    public PlanController(JdbcTemplate jdbc, PedidoServicio servicio) {
        this.jdbc = jdbc;
        this.servicio = servicio;
    }

    @GetMapping("/planes")
    public String planes(@AuthenticationPrincipal UsuarioSesion u, Model model) {
        int cliente = cliente(u);
        model.addAttribute("planes", jdbc.queryForList("SELECT * FROM plan WHERE activo = 1 ORDER BY precio"));
        model.addAttribute("mias", jdbc.queryForList("""
                SELECT s.*, pl.nombre, pl.precio, pl.periodicidad FROM suscripcion s JOIN plan pl ON pl.id_plan = s.id_plan
                 WHERE s.id_cliente = ? ORDER BY s.fecha_inicio DESC LIMIT 12""", cliente));
        model.addAttribute("activa", jdbc.queryForObject(
                "SELECT COUNT(*) FROM suscripcion WHERE id_cliente = ? AND estado = 'ACTIVA' AND fecha_fin >= CURDATE()", Integer.class, cliente) > 0);
        return "cuenta/planes";
    }

    @GetMapping("/planes/{id}/pagar")
    public String pasarela(@PathVariable int id, @AuthenticationPrincipal UsuarioSesion u, Model model) {
        cliente(u);
        Map<String, Object> pl = jdbc.queryForMap("SELECT * FROM plan WHERE id_plan = ? AND activo = 1", id);
        model.addAttribute("concepto", "Suscripción " + pl.get("nombre"));
        model.addAttribute("detalle", "Periodicidad " + pl.get("periodicidad").toString().toLowerCase() + " · " + pl.get("kg_incluidos") + " kg incluidos");
        model.addAttribute("valor", pl.get("precio"));
        model.addAttribute("accion", "/planes/" + id + "/pagar");
        model.addAttribute("volver", "/planes");
        model.addAttribute("metodos", jdbc.queryForList("SELECT id_metodo, nombre, tipo, en_linea FROM metodo_pago WHERE activo = 1 AND en_linea = 1 ORDER BY id_metodo"));
        return "pedidos/pasarela";
    }

    @PostMapping("/planes/{id}/pagar")
    public String suscribir(@PathVariable int id, @RequestParam int metodo, @RequestParam(defaultValue = "false") boolean rechazar,
                            @AuthenticationPrincipal UsuarioSesion u, RedirectAttributes ra) {
        try {
            servicio.suscribir(cliente(u), id, metodo, rechazar);
            ra.addFlashAttribute("ok", "¡Listo! Tu plan quedó activo y te enviamos el comprobante a tus notificaciones.");
            return "redirect:/planes";
        } catch (ReglaException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return rechazar ? "redirect:/planes/" + id + "/pagar" : "redirect:/planes";
        }
    }

    @PostMapping("/planes/suscripcion/{id}/cancelar")
    public String cancelar(@PathVariable int id, @AuthenticationPrincipal UsuarioSesion u, RedirectAttributes ra) {
        try {
            servicio.cancelarSuscripcion(id, cliente(u));
            ra.addFlashAttribute("ok", "Plan cancelado.");
        } catch (ReglaException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/planes";
    }

    private static int cliente(UsuarioSesion u) {
        if (u.idCliente() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return u.idCliente();
    }
}
