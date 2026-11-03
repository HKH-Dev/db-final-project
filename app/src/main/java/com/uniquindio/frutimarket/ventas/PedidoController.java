package com.uniquindio.frutimarket.ventas;

import com.uniquindio.frutimarket.config.UsuarioSesion;
import com.uniquindio.frutimarket.ventas.PedidoServicio.Linea;
import com.uniquindio.frutimarket.ventas.PedidoServicio.ReglaException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PedidoController {

    private final PedidoServicio servicio;
    private final JdbcTemplate jdbc;

    public PedidoController(PedidoServicio servicio, JdbcTemplate jdbc) {
        this.servicio = servicio;
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ mis pedidos (cliente)
    @GetMapping("/mis-pedidos")
    public String misPedidos(@AuthenticationPrincipal UsuarioSesion u, Model model) {
        if (u.idCliente() == null) {
            return "redirect:/crud/pedidos";
        }
        model.addAttribute("pedidos", jdbc.queryForList("""
                SELECT p.id_pedido, p.fecha_pedido, pa.nombre AS punto, p.estado, p.total, p.fecha_entrega_programada,
                       (SELECT COUNT(*) FROM detalle_pedido d WHERE d.id_pedido = p.id_pedido) AS lineas
                  FROM pedido p JOIN punto_acopio pa ON pa.id_punto = p.id_punto
                 WHERE p.id_cliente = ? ORDER BY p.fecha_pedido DESC""", u.idCliente()));
        return "pedidos/mis-pedidos";
    }

    // ------------------------------------------------------------------ nuevo pedido
    @GetMapping("/pedidos/nuevo")
    public String nuevo(@RequestParam(required = false) Integer punto, @RequestParam(required = false) Integer cliente,
                        @AuthenticationPrincipal UsuarioSesion u, Model model) {
        if (!u.esGestion() && !u.esCliente()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (u.esCliente()) {
            if (u.idCliente() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            cliente = u.idCliente();
        }
        List<Map<String, Object>> puntos = jdbc.queryForList("SELECT pa.id_punto, pa.nombre, m.nombre AS municipio FROM punto_acopio pa JOIN municipio m ON m.id_municipio = pa.id_municipio WHERE pa.activo = 1 ORDER BY pa.id_punto");
        if (punto == null) {
            punto = u.idPunto() != null ? u.idPunto() : ((Number) puntos.getFirst().get("id_punto")).intValue();
        }
        model.addAttribute("puntos", puntos);
        model.addAttribute("punto", punto);
        model.addAttribute("productos", servicio.disponibles(punto));
        if (u.esGestion()) {
            model.addAttribute("clientes", jdbc.queryForList("SELECT id_cliente, nombre, tipo_cliente, direccion FROM cliente WHERE activo = 1 ORDER BY nombre"));
        }
        model.addAttribute("cliente", cliente);
        if (cliente != null) {
            Map<String, Object> c = jdbc.queryForMap("SELECT nombre, direccion FROM cliente WHERE id_cliente = ?", cliente);
            model.addAttribute("datosCliente", c);
            model.addAttribute("plan", jdbc.queryForList("""
                    SELECT pl.nombre, pl.descuento_pct FROM suscripcion s JOIN plan pl ON pl.id_plan = s.id_plan
                     WHERE s.id_cliente = ? AND s.estado = 'ACTIVA' AND CURDATE() BETWEEN s.fecha_inicio AND s.fecha_fin LIMIT 1""", cliente)
                    .stream().findFirst().orElse(null));
        }
        model.addAttribute("manana", LocalDate.now().plusDays(1));
        return "pedidos/nuevo";
    }

    @PostMapping("/pedidos")
    public String crear(@RequestParam MultiValueMap<String, String> form, @AuthenticationPrincipal UsuarioSesion u, RedirectAttributes ra) {
        int punto = Integer.parseInt(form.getFirst("punto"));
        Integer cliente = u.esCliente() ? u.idCliente() : parse(form.getFirst("cliente"));
        if (cliente == null || (!u.esGestion() && !u.esCliente())) {
            ra.addFlashAttribute("error", "Elija el cliente del pedido.");
            return "redirect:/pedidos/nuevo?punto=" + punto;
        }
        List<Linea> lineas = new ArrayList<>();
        for (String k : form.keySet()) {
            if (k.startsWith("kg_")) {
                String v = form.getFirst(k);
                if (v != null && !v.isBlank()) {
                    try {
                        lineas.add(new Linea(Integer.parseInt(k.substring(3)), new BigDecimal(v.replace(',', '.'))));
                    } catch (NumberFormatException e) {
                        ra.addFlashAttribute("error", "Las cantidades deben ser números.");
                        return "redirect:/pedidos/nuevo?punto=" + punto + "&cliente=" + cliente;
                    }
                }
            }
        }
        try {
            LocalDate entrega = LocalDate.parse(form.getFirst("entrega"));
            int id = servicio.crear(cliente, punto, entrega, form.getFirst("direccion"), form.getFirst("observaciones"), lineas);
            ra.addFlashAttribute("ok", "Pedido #" + id + " creado. El inventario ya se descontó de los lotes.");
            return u.esCliente() ? "redirect:/pagos/pedido/" + id : "redirect:/pedidos/" + id;
        } catch (ReglaException | java.time.format.DateTimeParseException e) {
            ra.addFlashAttribute("error", e instanceof ReglaException ? e.getMessage() : "Fecha de entrega no válida.");
            return "redirect:/pedidos/nuevo?punto=" + punto + "&cliente=" + cliente;
        }
    }

    // ------------------------------------------------------------------ detalle y acciones
    @GetMapping("/pedidos/{id}")
    public String detalle(@PathVariable int id, @AuthenticationPrincipal UsuarioSesion u, Model model) {
        Map<String, Object> p = cargar(id, u);
        model.addAttribute("p", p);
        model.addAttribute("lineas", jdbc.queryForList("""
                SELECT d.linea, CONCAT(pr.nombre, ' ', pr.variedad) AS producto, l.codigo AS lote, l.calidad, f.nombre AS finca,
                       CONCAT(ca.nombres, ' ', ca.apellidos) AS campesino, d.cantidad_kg, d.precio_kg, d.iva_pct, d.subtotal
                  FROM detalle_pedido d JOIN lote l ON l.id_lote = d.id_lote JOIN producto pr ON pr.id_producto = l.id_producto
                  JOIN finca f ON f.id_finca = l.id_finca JOIN campesino ca ON ca.id_campesino = f.id_campesino
                 WHERE d.id_pedido = ? ORDER BY d.linea""", id));
        model.addAttribute("pagos", jdbc.queryForList("""
                SELECT pg.id_pago, pg.fecha_pago, mp.nombre AS metodo, pg.referencia, pg.valor, pg.estado
                  FROM pago pg JOIN metodo_pago mp ON mp.id_metodo = pg.id_metodo WHERE pg.id_pedido = ? ORDER BY pg.fecha_pago""", id));
        model.addAttribute("factura", jdbc.queryForList("SELECT numero, fecha_emision FROM factura WHERE id_pedido = ?", id).stream().findFirst().orElse(null));
        model.addAttribute("despacho", jdbc.queryForList("""
                SELECT d.id_despacho, d.numero_guia, d.fecha_salida, d.fecha_entrega, d.estado, d.temperatura_c, t.nombre AS transportador, t.placa, t.tipo_vehiculo
                  FROM despacho d JOIN transportador t ON t.id_transportador = d.id_transportador WHERE d.id_pedido = ?""", id).stream().findFirst().orElse(null));
        if (u.esGestion()) {
            model.addAttribute("transportadores", jdbc.queryForList("SELECT id_transportador, nombre, placa, tipo_vehiculo, capacidad_kg, refrigerado FROM transportador WHERE activo = 1 ORDER BY capacidad_kg"));
        }
        return "pedidos/detalle";
    }

    @PostMapping("/pedidos/{id}/despachar")
    public String despachar(@PathVariable int id, @RequestParam int transportador, @RequestParam(required = false) BigDecimal temperatura,
                            @AuthenticationPrincipal UsuarioSesion u, RedirectAttributes ra) {
        soloGestion(u);
        return accion(id, ra, () -> servicio.despachar(id, transportador, temperatura), "Pedido despachado. Se generó la guía de despacho.");
    }

    @PostMapping("/pedidos/{id}/entregar")
    public String entregar(@PathVariable int id, @AuthenticationPrincipal UsuarioSesion u, RedirectAttributes ra) {
        soloGestion(u);
        return accion(id, ra, () -> servicio.entregar(id), "Pedido marcado como entregado.");
    }

    @PostMapping("/pedidos/{id}/cancelar")
    public String cancelar(@PathVariable int id, @AuthenticationPrincipal UsuarioSesion u, RedirectAttributes ra) {
        cargar(id, u);
        return accion(id, ra, () -> servicio.cancelar(id), "Pedido cancelado. Los kilos volvieron al inventario.");
    }

    // ------------------------------------------------------------------ pasarela de pago (simulada)
    @GetMapping("/pagos/pedido/{id}")
    public String pasarela(@PathVariable int id, @AuthenticationPrincipal UsuarioSesion u, Model model) {
        Map<String, Object> p = cargar(id, u);
        if (!"PENDIENTE".equals(p.get("estado"))) {
            return "redirect:/pedidos/" + id;
        }
        model.addAttribute("concepto", "Pedido #" + id);
        model.addAttribute("detalle", p.get("cliente") + " · " + p.get("punto"));
        model.addAttribute("valor", p.get("total"));
        model.addAttribute("accion", "/pagos/pedido/" + id);
        model.addAttribute("volver", "/pedidos/" + id);
        model.addAttribute("metodos", jdbc.queryForList("SELECT id_metodo, nombre, tipo, en_linea FROM metodo_pago WHERE activo = 1 ORDER BY en_linea DESC, id_metodo"));
        return "pedidos/pasarela";
    }

    @PostMapping("/pagos/pedido/{id}")
    public String pagar(@PathVariable int id, @RequestParam int metodo, @RequestParam(defaultValue = "false") boolean rechazar,
                        @AuthenticationPrincipal UsuarioSesion u, RedirectAttributes ra) {
        cargar(id, u);
        try {
            PedidoServicio.ResultadoPago r = servicio.pagar(id, metodo, rechazar);
            if (r.aprobado()) {
                ra.addFlashAttribute("ok", "Pago aprobado (referencia " + r.referencia() + "). Se generó la factura electrónica.");
                return "redirect:/pedidos/" + id;
            }
            ra.addFlashAttribute("error", "La pasarela rechazó el pago (referencia " + r.referencia() + "). Intente con otro medio.");
            return "redirect:/pagos/pedido/" + id;
        } catch (ReglaException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos/" + id;
        }
    }

    // ------------------------------------------------------------------ apoyo
    private Map<String, Object> cargar(int id, UsuarioSesion u) {
        List<Map<String, Object>> r = jdbc.queryForList("""
                SELECT p.*, c.nombre AS cliente, c.tipo_cliente, c.telefono, c.email, CONCAT(c.tipo_documento, ' ', c.numero_documento) AS documento,
                       pa.nombre AS punto, pl.nombre AS plan
                  FROM pedido p JOIN cliente c ON c.id_cliente = p.id_cliente JOIN punto_acopio pa ON pa.id_punto = p.id_punto
                  LEFT JOIN suscripcion s ON s.id_suscripcion = p.id_suscripcion LEFT JOIN plan pl ON pl.id_plan = s.id_plan
                 WHERE p.id_pedido = ?""", id);
        if (r.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Map<String, Object> p = r.getFirst();
        boolean propio = u.idCliente() != null && u.idCliente().equals(((Number) p.get("id_cliente")).intValue());
        if (!u.esGestion() && !propio) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return p;
    }

    private static void soloGestion(UsuarioSesion u) {
        if (!u.esGestion()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String accion(int id, RedirectAttributes ra, Runnable r, String ok) {
        try {
            r.run();
            ra.addFlashAttribute("ok", ok);
        } catch (ReglaException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    private static Integer parse(String s) {
        try {
            return s == null || s.isBlank() ? null : Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
