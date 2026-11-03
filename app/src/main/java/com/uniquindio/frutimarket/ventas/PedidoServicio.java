package com.uniquindio.frutimarket.ventas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas del proceso de venta. Cada metodo es una transaccion: si algo falla (por ejemplo el
 * trigger avisa que no hay inventario) se deshace todo y no quedan pedidos ni pagos a medias.
 */
@Service
public class PedidoServicio {

    public static class ReglaException extends RuntimeException {
        public ReglaException(String m) {
            super(m);
        }
    }

    public record Linea(int idProducto, BigDecimal kg) {
    }

    public record ProductoDisponible(int id, String nombre, BigDecimal precio, BigDecimal ivaPct, BigDecimal disponible,
                                     boolean refrigerado, String categoria) {
    }

    private static final SecureRandom AZAR = new SecureRandom();
    private final JdbcTemplate jdbc;

    public PedidoServicio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ catalogo para vender
    public List<ProductoDisponible> disponibles(int idPunto) {
        return jdbc.query("""
                SELECT pr.id_producto, CONCAT(pr.nombre, ' ', pr.variedad) AS nombre, pr.precio_venta_kg, pr.iva_pct,
                       SUM(l.cantidad_disponible_kg) AS disponible, pr.requiere_refrigeracion, c.nombre AS categoria
                  FROM producto pr
                  JOIN categoria c ON c.id_categoria = pr.id_categoria
                  JOIN lote l ON l.id_producto = pr.id_producto
                 WHERE l.id_punto = ? AND l.estado = 'DISPONIBLE' AND l.cantidad_disponible_kg > 0
                   AND l.fecha_vencimiento > CURDATE() AND pr.activo = 1
                 GROUP BY pr.id_producto, pr.nombre, pr.variedad, pr.precio_venta_kg, pr.iva_pct, pr.requiere_refrigeracion, c.nombre
                 ORDER BY c.nombre, nombre""", (rs, i) -> new ProductoDisponible(rs.getInt(1), rs.getString(2), rs.getBigDecimal(3),
                rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBoolean(6), rs.getString(7)), idPunto);
    }

    // ------------------------------------------------------------------ crear
    @Transactional
    public int crear(int idCliente, int idPunto, LocalDate entrega, String direccion, String observaciones, List<Linea> lineas) {
        List<Linea> validas = lineas.stream().filter(l -> l.kg() != null && l.kg().signum() > 0).toList();
        if (validas.isEmpty()) {
            throw new ReglaException("Agregue al menos un producto con cantidad mayor que cero.");
        }
        if (entrega == null || entrega.isBefore(LocalDate.now())) {
            throw new ReglaException("La fecha de entrega no puede ser anterior a hoy.");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new ReglaException("Escriba la dirección de entrega.");
        }
        // suscripcion vigente: da descuento en el pedido
        List<Map<String, Object>> sus = jdbc.queryForList("""
                SELECT s.id_suscripcion, pl.descuento_pct FROM suscripcion s JOIN plan pl ON pl.id_plan = s.id_plan
                 WHERE s.id_cliente = ? AND s.estado = 'ACTIVA' AND CURDATE() BETWEEN s.fecha_inicio AND s.fecha_fin
                 ORDER BY s.fecha_fin DESC LIMIT 1""", idCliente);
        Integer idSus = sus.isEmpty() ? null : ((Number) sus.getFirst().get("id_suscripcion")).intValue();
        BigDecimal dctoPct = sus.isEmpty() ? BigDecimal.ZERO : (BigDecimal) sus.getFirst().get("descuento_pct");

        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO pedido (id_cliente, id_punto, id_suscripcion, fecha_pedido, fecha_entrega_programada,
                                        direccion_entrega, estado, observaciones)
                    VALUES (?, ?, ?, NOW(), ?, ?, 'PENDIENTE', ?)""", new String[] {"id_pedido"});
            ps.setInt(1, idCliente);
            ps.setInt(2, idPunto);
            ps.setObject(3, idSus);
            ps.setObject(4, entrega);
            ps.setString(5, direccion.trim());
            ps.setString(6, observaciones == null || observaciones.isBlank() ? null : observaciones.trim());
            return ps;
        }, kh);
        int idPedido = kh.getKey().intValue();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal ivaBruto = BigDecimal.ZERO;
        int linea = 0;
        for (Linea l : validas) {
            Map<String, Object> pr = jdbc.queryForMap("SELECT CONCAT(nombre, ' ', variedad) AS nombre, precio_venta_kg, iva_pct FROM producto WHERE id_producto = ?", l.idProducto());
            BigDecimal falta = l.kg().setScale(1, RoundingMode.HALF_UP);
            // primero sale el lote que vence antes (FEFO); FOR UPDATE bloquea los lotes hasta el COMMIT
            List<Map<String, Object>> lotes = jdbc.queryForList("""
                    SELECT id_lote, cantidad_disponible_kg, calidad FROM lote
                     WHERE id_punto = ? AND id_producto = ? AND estado = 'DISPONIBLE' AND cantidad_disponible_kg > 0
                       AND fecha_vencimiento > CURDATE()
                     ORDER BY fecha_vencimiento, id_lote FOR UPDATE""", idPunto, l.idProducto());
            BigDecimal hay = lotes.stream().map(x -> (BigDecimal) x.get("cantidad_disponible_kg")).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (hay.compareTo(falta) < 0) {
                throw new ReglaException("Solo hay " + hay.stripTrailingZeros().toPlainString() + " kg de " + pr.get("nombre") + " en este punto de acopio.");
            }
            for (Map<String, Object> lote : lotes) {
                if (falta.signum() <= 0) {
                    break;
                }
                BigDecimal toma = falta.min((BigDecimal) lote.get("cantidad_disponible_kg"));
                BigDecimal precio = precioPorCalidad((BigDecimal) pr.get("precio_venta_kg"), (String) lote.get("calidad"));
                BigDecimal sub = toma.multiply(precio).setScale(0, RoundingMode.HALF_UP);
                BigDecimal iva = (BigDecimal) pr.get("iva_pct");
                jdbc.update("INSERT INTO detalle_pedido (id_pedido, linea, id_lote, cantidad_kg, precio_kg, iva_pct, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        idPedido, ++linea, lote.get("id_lote"), toma, precio, iva, sub);
                subtotal = subtotal.add(sub);
                ivaBruto = ivaBruto.add(sub.multiply(iva).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                falta = falta.subtract(toma);
            }
        }
        BigDecimal descuento = subtotal.multiply(dctoPct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal factor = subtotal.signum() == 0 ? BigDecimal.ONE
                : BigDecimal.ONE.subtract(descuento.divide(subtotal, 6, RoundingMode.HALF_UP));
        BigDecimal iva = ivaBruto.multiply(factor).setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(descuento).add(iva);
        jdbc.update("UPDATE pedido SET subtotal = ?, descuento = ?, iva = ?, total = ? WHERE id_pedido = ?", subtotal, descuento, iva, total, idPedido);

        notificarCliente(idCliente, "PEDIDO", "Pedido #" + idPedido + " recibido",
                "Recibimos tu pedido por " + pesos(total) + ". Entrega programada el " + entrega.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".",
                LocalDateTime.now());
        notificarCliente(idCliente, "RECORDATORIO", "Pedido #" + idPedido + " pendiente de pago",
                "Completa el pago en línea para que podamos alistar tu pedido.", LocalDateTime.now().plusHours(2));
        return idPedido;
    }

    static BigDecimal precioPorCalidad(BigDecimal base, String calidad) {
        BigDecimal f = switch (calidad) {
            case "EXTRA" -> new BigDecimal("1.10");
            case "SEGUNDA" -> new BigDecimal("0.85");
            default -> BigDecimal.ONE;
        };
        return base.multiply(f).setScale(-1, RoundingMode.HALF_UP).setScale(0);
    }

    // ------------------------------------------------------------------ pago en linea (pasarela simulada)
    public record ResultadoPago(int idPago, boolean aprobado, String referencia) {
    }

    @Transactional
    public ResultadoPago pagar(int idPedido, int idMetodo, boolean simularRechazo) {
        Map<String, Object> p = pedido(idPedido);
        if (!"PENDIENTE".equals(p.get("estado"))) {
            throw new ReglaException("El pedido #" + idPedido + " ya no está pendiente de pago.");
        }
        Map<String, Object> m = metodo(idMetodo);
        BigDecimal total = (BigDecimal) p.get("total");
        String ref = referencia((String) m.get("tipo"));
        String estado = simularRechazo ? "RECHAZADO" : "APROBADO";
        int idPago = insertarPago(idMetodo, idPedido, null, total, ref, estado);
        int idCliente = ((Number) p.get("id_cliente")).intValue();
        if (simularRechazo) {
            notificarCliente(idCliente, "PAGO", "Pago rechazado", "Tu banco rechazó el pago del pedido #" + idPedido + ". Intenta con otro medio de pago.", LocalDateTime.now());
            return new ResultadoPago(idPago, false, ref);
        }
        jdbc.update("UPDATE pedido SET estado = 'PAGADO' WHERE id_pedido = ?", idPedido);
        int siguiente = jdbc.queryForObject("SELECT COALESCE(MAX(id_factura), 0) + 1 FROM factura", Integer.class);
        jdbc.update("""
                INSERT INTO factura (id_pedido, numero, fecha_emision, subtotal, descuento, iva, total)
                SELECT id_pedido, ?, NOW(), subtotal, descuento, iva, total FROM pedido WHERE id_pedido = ?""",
                "FM-" + String.format("%06d", siguiente), idPedido);
        notificarCliente(idCliente, "PAGO", "Pago aprobado", "Recibimos " + pesos(total) + " del pedido #" + idPedido + " con " + m.get("nombre") + ".", LocalDateTime.now());
        return new ResultadoPago(idPago, true, ref);
    }

    // ------------------------------------------------------------------ logistica
    @Transactional
    public void despachar(int idPedido, int idTransportador, BigDecimal temperatura) {
        Map<String, Object> p = pedido(idPedido);
        if (!"PAGADO".equals(p.get("estado"))) {
            throw new ReglaException("Solo se despachan pedidos pagados.");
        }
        Map<String, Object> t = jdbc.queryForMap("SELECT nombre, placa, capacidad_kg FROM transportador WHERE id_transportador = ?", idTransportador);
        BigDecimal kg = jdbc.queryForObject("SELECT SUM(cantidad_kg) FROM detalle_pedido WHERE id_pedido = ?", BigDecimal.class, idPedido);
        if (kg.compareTo((BigDecimal) t.get("capacidad_kg")) > 0) {
            throw new ReglaException("El pedido pesa " + kg.stripTrailingZeros().toPlainString() + " kg y el vehículo carga máximo " + t.get("capacidad_kg") + " kg.");
        }
        int siguiente = jdbc.queryForObject("SELECT COALESCE(MAX(id_despacho), 0) + 1 FROM despacho", Integer.class);
        jdbc.update("""
                INSERT INTO despacho (id_pedido, id_transportador, numero_guia, fecha_salida, estado, temperatura_c)
                VALUES (?, ?, ?, NOW(), 'EN_RUTA', ?)""", idPedido, idTransportador, "GD-" + String.format("%06d", siguiente), temperatura);
        jdbc.update("UPDATE pedido SET estado = 'DESPACHADO' WHERE id_pedido = ?", idPedido);
        notificarCliente(((Number) p.get("id_cliente")).intValue(), "PEDIDO", "Pedido #" + idPedido + " en camino",
                "Tu pedido salió en el vehículo " + t.get("placa") + ".", LocalDateTime.now());
    }

    @Transactional
    public void entregar(int idPedido) {
        Map<String, Object> p = pedido(idPedido);
        if (!"DESPACHADO".equals(p.get("estado"))) {
            throw new ReglaException("Solo se entregan pedidos despachados.");
        }
        jdbc.update("UPDATE despacho SET estado = 'ENTREGADO', fecha_entrega = NOW() WHERE id_pedido = ?", idPedido);
        jdbc.update("UPDATE pedido SET estado = 'ENTREGADO' WHERE id_pedido = ?", idPedido);
        notificarCliente(((Number) p.get("id_cliente")).intValue(), "PEDIDO", "Pedido #" + idPedido + " entregado",
                "Gracias por comprarle al campo quindiano.", LocalDateTime.now());
    }

    /** El trigger trg_pedido_cancelar devuelve los kilos a los lotes */
    @Transactional
    public void cancelar(int idPedido) {
        Map<String, Object> p = pedido(idPedido);
        String estado = (String) p.get("estado");
        if (!"PENDIENTE".equals(estado) && !"PAGADO".equals(estado)) {
            throw new ReglaException("Un pedido " + estado.toLowerCase() + " ya no se puede cancelar.");
        }
        jdbc.update("UPDATE pedido SET estado = 'CANCELADO' WHERE id_pedido = ?", idPedido);
        int reembolsos = jdbc.update("UPDATE pago SET estado = 'REEMBOLSADO' WHERE id_pedido = ? AND estado = 'APROBADO'", idPedido);
        notificarCliente(((Number) p.get("id_cliente")).intValue(), "PEDIDO", "Pedido #" + idPedido + " cancelado",
                reembolsos > 0 ? "Reembolsamos " + pesos((BigDecimal) p.get("total")) + " a tu medio de pago." : "El pedido se canceló sin cobro.",
                LocalDateTime.now());
    }

    // ------------------------------------------------------------------ suscripciones (planes)
    @Transactional
    public int suscribir(int idCliente, int idPlan, int idMetodo, boolean simularRechazo) {
        Integer vigentes = jdbc.queryForObject("SELECT COUNT(*) FROM suscripcion WHERE id_cliente = ? AND estado = 'ACTIVA' AND fecha_fin >= CURDATE()",
                Integer.class, idCliente);
        if (vigentes > 0) {
            throw new ReglaException("Ya tienes un plan activo. Cancélalo o espera a que venza para cambiarlo.");
        }
        Map<String, Object> pl = jdbc.queryForMap("SELECT nombre, periodicidad, precio FROM plan WHERE id_plan = ? AND activo = 1", idPlan);
        Map<String, Object> m = metodo(idMetodo);
        if (!Boolean.TRUE.equals(m.get("en_linea")) && !Integer.valueOf(1).equals(m.get("en_linea"))) {
            throw new ReglaException("Los planes se pagan con un medio en línea.");
        }
        int dias = switch ((String) pl.get("periodicidad")) {
            case "SEMANAL" -> 7;
            case "QUINCENAL" -> 15;
            default -> 30;
        };
        BigDecimal precio = (BigDecimal) pl.get("precio");
        if (simularRechazo) {
            throw new ReglaException("La pasarela rechazó el pago de " + pesos(precio) + ". No se activó el plan.");
        }
        LocalDate fin = LocalDate.now().plusDays(dias);
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO suscripcion (id_cliente, id_plan, fecha_inicio, fecha_fin, estado, renovacion_automatica) VALUES (?, ?, CURDATE(), ?, 'ACTIVA', 1)",
                    new String[] {"id_suscripcion"});
            ps.setInt(1, idCliente);
            ps.setInt(2, idPlan);
            ps.setObject(3, fin);
            return ps;
        }, kh);
        int idSus = kh.getKey().intValue();
        insertarPago(idMetodo, null, idSus, precio, referencia((String) m.get("tipo")), "APROBADO");
        String finTxt = fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        notificarCliente(idCliente, "SUSCRIPCION", pl.get("nombre") + " activo", "Tu plan quedó activo hasta el " + finTxt + ". Pagaste " + pesos(precio) + ".", LocalDateTime.now());
        notificarCliente(idCliente, "RECORDATORIO", "Tu plan se renueva pronto", "El " + finTxt + " se renueva tu " + pl.get("nombre") + ". Revisa tu medio de pago.",
                fin.minusDays(2).atTime(8, 0));
        return idSus;
    }

    @Transactional
    public void cancelarSuscripcion(int idSuscripcion, int idCliente) {
        int n = jdbc.update("UPDATE suscripcion SET estado = 'CANCELADA', renovacion_automatica = 0 WHERE id_suscripcion = ? AND id_cliente = ? AND estado IN ('ACTIVA', 'PAUSADA')",
                idSuscripcion, idCliente);
        if (n == 0) {
            throw new ReglaException("No se encontró un plan activo para cancelar.");
        }
        notificarCliente(idCliente, "SUSCRIPCION", "Plan cancelado", "Cancelaste tu plan. Puedes volver a suscribirte cuando quieras.", LocalDateTime.now());
    }

    // ------------------------------------------------------------------ apoyo
    public Map<String, Object> pedido(int idPedido) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM pedido WHERE id_pedido = ?", idPedido);
        if (r.isEmpty()) {
            throw new ReglaException("No existe el pedido #" + idPedido + ".");
        }
        return r.getFirst();
    }

    private Map<String, Object> metodo(int idMetodo) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM metodo_pago WHERE id_metodo = ? AND activo = 1", idMetodo);
        if (r.isEmpty()) {
            throw new ReglaException("Elija un medio de pago válido.");
        }
        return r.getFirst();
    }

    private int insertarPago(int idMetodo, Integer idPedido, Integer idSus, BigDecimal valor, String ref, String estado) {
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO pago (id_metodo, id_pedido, id_suscripcion, fecha_pago, valor, referencia, estado) VALUES (?, ?, ?, NOW(), ?, ?, ?)",
                    new String[] {"id_pago"});
            ps.setInt(1, idMetodo);
            ps.setObject(2, idPedido);
            ps.setObject(3, idSus);
            ps.setBigDecimal(4, valor);
            ps.setString(5, ref);
            ps.setString(6, estado);
            return ps;
        }, kh);
        return kh.getKey().intValue();
    }

    private String referencia(String tipo) {
        String pref = switch (tipo) {
            case "TARJETA" -> "TC";
            case "PSE" -> "PSE";
            case "BILLETERA" -> "BIL";
            case "TRANSFERENCIA" -> "TRF";
            default -> "EFE";
        };
        String r;
        do {
            r = pref + (10_000_000 + AZAR.nextInt(90_000_000));
        } while (jdbc.queryForObject("SELECT COUNT(*) FROM pago WHERE referencia = ?", Integer.class, r) > 0);
        return r;
    }

    public void notificarCliente(int idCliente, String tipo, String titulo, String mensaje, LocalDateTime cuando) {
        jdbc.update("""
                INSERT INTO notificacion (id_usuario, tipo, titulo, mensaje, fecha_programada, leida, fecha_creacion)
                SELECT id_usuario, ?, ?, ?, ?, 0, NOW() FROM cliente WHERE id_cliente = ? AND id_usuario IS NOT NULL""",
                tipo, titulo, mensaje, cuando, idCliente);
    }

    static String pesos(BigDecimal v) {
        return "$ " + String.format(java.util.Locale.forLanguageTag("es-CO"), "%,.0f", v).replace(',', '.');
    }
}
