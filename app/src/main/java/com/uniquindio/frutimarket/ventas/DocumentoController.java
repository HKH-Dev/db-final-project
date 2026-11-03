package com.uniquindio.frutimarket.ventas;

import com.uniquindio.frutimarket.config.UsuarioSesion;
import com.uniquindio.frutimarket.reportes.PdfServicio;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Documentos del sistema generados con plantillas (templates/pdf): factura de venta,
 * comprobante de pago, guia de despacho y certificado de suscripcion.
 */
@RestController
@RequestMapping("/documentos")
public class DocumentoController {

    private static final String EMPRESA = """
            SELECT 'FrutiMarket Quindío S.A.S.' AS razon, '901.482.733-5' AS nit, 'Calle 50 # 19-30, Bodega 4, Armenia, Quindío' AS direccion,
                   '606 745 2210' AS telefono, 'facturacion@frutimarket.com.co' AS email""";

    private final JdbcTemplate jdbc;
    private final PdfServicio pdf;

    public DocumentoController(JdbcTemplate jdbc, PdfServicio pdf) {
        this.jdbc = jdbc;
        this.pdf = pdf;
    }

    @GetMapping("/factura/{idPedido}")
    public ResponseEntity<byte[]> factura(@PathVariable int idPedido, @AuthenticationPrincipal UsuarioSesion u) {
        Map<String, Object> f = una("""
                SELECT f.*, p.fecha_pedido, p.fecha_entrega_programada, p.direccion_entrega, p.id_cliente, c.nombre AS cliente,
                       CONCAT(c.tipo_documento, ' ', c.numero_documento) AS documento, c.direccion, c.telefono, c.email,
                       m.nombre AS municipio, pa.nombre AS punto,
                       (SELECT mp.nombre FROM pago pg JOIN metodo_pago mp ON mp.id_metodo = pg.id_metodo
                         WHERE pg.id_pedido = p.id_pedido AND pg.estado IN ('APROBADO', 'REEMBOLSADO') ORDER BY pg.fecha_pago DESC LIMIT 1) AS medio,
                       (SELECT pg.referencia FROM pago pg WHERE pg.id_pedido = p.id_pedido AND pg.estado IN ('APROBADO', 'REEMBOLSADO')
                         ORDER BY pg.fecha_pago DESC LIMIT 1) AS referencia,
                       p.estado
                  FROM factura f JOIN pedido p ON p.id_pedido = f.id_pedido JOIN cliente c ON c.id_cliente = p.id_cliente
                  JOIN municipio m ON m.id_municipio = c.id_municipio JOIN punto_acopio pa ON pa.id_punto = p.id_punto
                 WHERE f.id_pedido = ?""", idPedido);
        permiso(u, f.get("id_cliente"));
        Map<String, Object> d = base();
        d.put("f", f);
        d.put("lineas", jdbc.queryForList("""
                SELECT d.linea, pr.nombre, pr.variedad, l.codigo AS lote, l.calidad, d.cantidad_kg, d.precio_kg, d.iva_pct, d.subtotal
                  FROM detalle_pedido d JOIN lote l ON l.id_lote = d.id_lote JOIN producto pr ON pr.id_producto = l.id_producto
                 WHERE d.id_pedido = ? ORDER BY d.linea""", idPedido));
        return responder("pdf/factura", d, "factura-" + f.get("numero"));
    }

    @GetMapping("/comprobante/{idPago}")
    public ResponseEntity<byte[]> comprobante(@PathVariable int idPago, @AuthenticationPrincipal UsuarioSesion u) {
        Map<String, Object> p = una("""
                SELECT pg.*, mp.nombre AS metodo, mp.tipo AS tipo_metodo,
                       COALESCE(c1.id_cliente, c2.id_cliente) AS id_cliente, COALESCE(c1.nombre, c2.nombre) AS cliente,
                       COALESCE(CONCAT(c1.tipo_documento, ' ', c1.numero_documento), CONCAT(c2.tipo_documento, ' ', c2.numero_documento)) AS documento,
                       COALESCE(c1.email, c2.email) AS email,
                       CASE WHEN pg.id_pedido IS NOT NULL THEN CONCAT('Pago del pedido #', pg.id_pedido)
                            ELSE CONCAT('Suscripción ', pl.nombre, ' (', DATE_FORMAT(s.fecha_inicio, '%d/%m/%Y'), ' a ', DATE_FORMAT(s.fecha_fin, '%d/%m/%Y'), ')') END AS concepto,
                       (SELECT f.numero FROM factura f WHERE f.id_pedido = pg.id_pedido) AS factura
                  FROM pago pg JOIN metodo_pago mp ON mp.id_metodo = pg.id_metodo
                  LEFT JOIN pedido pe ON pe.id_pedido = pg.id_pedido LEFT JOIN cliente c1 ON c1.id_cliente = pe.id_cliente
                  LEFT JOIN suscripcion s ON s.id_suscripcion = pg.id_suscripcion LEFT JOIN plan pl ON pl.id_plan = s.id_plan
                  LEFT JOIN cliente c2 ON c2.id_cliente = s.id_cliente
                 WHERE pg.id_pago = ?""", idPago);
        permiso(u, p.get("id_cliente"));
        Map<String, Object> d = base();
        d.put("p", p);
        return responder("pdf/comprobante", d, "comprobante-" + p.get("referencia"));
    }

    @GetMapping("/guia/{idDespacho}")
    public ResponseEntity<byte[]> guia(@PathVariable int idDespacho, @AuthenticationPrincipal UsuarioSesion u) {
        Map<String, Object> g = una("""
                SELECT d.*, t.nombre AS transportador, t.numero_documento AS doc_transportador, t.telefono AS tel_transportador, t.placa,
                       t.tipo_vehiculo, t.refrigerado, p.id_cliente, p.direccion_entrega, p.fecha_entrega_programada, p.observaciones,
                       c.nombre AS cliente, c.telefono, m.nombre AS municipio, pa.nombre AS punto, pa.direccion AS dir_punto,
                       (SELECT SUM(dp.cantidad_kg) FROM detalle_pedido dp WHERE dp.id_pedido = p.id_pedido) AS kg_total
                  FROM despacho d JOIN transportador t ON t.id_transportador = d.id_transportador JOIN pedido p ON p.id_pedido = d.id_pedido
                  JOIN cliente c ON c.id_cliente = p.id_cliente JOIN municipio m ON m.id_municipio = c.id_municipio
                  JOIN punto_acopio pa ON pa.id_punto = p.id_punto
                 WHERE d.id_despacho = ?""", idDespacho);
        permiso(u, g.get("id_cliente"));
        Map<String, Object> d = base();
        d.put("g", g);
        d.put("lineas", jdbc.queryForList("""
                SELECT d.linea, CONCAT(pr.nombre, ' ', pr.variedad) AS producto, l.codigo AS lote, pr.requiere_refrigeracion, d.cantidad_kg,
                       f.nombre AS finca, mu.nombre AS municipio
                  FROM detalle_pedido d JOIN lote l ON l.id_lote = d.id_lote JOIN producto pr ON pr.id_producto = l.id_producto
                  JOIN finca f ON f.id_finca = l.id_finca JOIN municipio mu ON mu.id_municipio = f.id_municipio
                 WHERE d.id_pedido = ? ORDER BY d.linea""", g.get("id_pedido")));
        return responder("pdf/guia", d, "guia-" + g.get("numero_guia"));
    }

    @GetMapping("/certificado/{idSuscripcion}")
    public ResponseEntity<byte[]> certificado(@PathVariable int idSuscripcion, @AuthenticationPrincipal UsuarioSesion u) {
        Map<String, Object> s = una("""
                SELECT s.*, pl.nombre AS plan, pl.descripcion, pl.periodicidad, pl.precio, pl.kg_incluidos, pl.descuento_pct,
                       c.nombre AS cliente, CONCAT(c.tipo_documento, ' ', c.numero_documento) AS documento, c.direccion, m.nombre AS municipio
                  FROM suscripcion s JOIN plan pl ON pl.id_plan = s.id_plan JOIN cliente c ON c.id_cliente = s.id_cliente
                  JOIN municipio m ON m.id_municipio = c.id_municipio WHERE s.id_suscripcion = ?""", idSuscripcion);
        permiso(u, s.get("id_cliente"));
        Map<String, Object> d = base();
        d.put("s", s);
        d.put("pagos", jdbc.queryForList("""
                SELECT pg.fecha_pago, mp.nombre AS metodo, pg.referencia, pg.valor, pg.estado FROM pago pg
                  JOIN metodo_pago mp ON mp.id_metodo = pg.id_metodo WHERE pg.id_suscripcion = ? ORDER BY pg.fecha_pago""", idSuscripcion));
        return responder("pdf/certificado", d, "certificado-suscripcion-" + idSuscripcion);
    }

    private Map<String, Object> base() {
        Map<String, Object> d = new HashMap<>();
        d.put("empresa", jdbc.queryForMap(EMPRESA));
        d.put("generado", java.time.LocalDateTime.now());
        return d;
    }

    private Map<String, Object> una(String sql, Object id) {
        List<Map<String, Object>> r = jdbc.queryForList(sql, id);
        if (r.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El documento no existe");
        }
        return r.getFirst();
    }

    private static void permiso(UsuarioSesion u, Object idCliente) {
        if (u.esGestion()) {
            return;
        }
        if (u.idCliente() == null || idCliente == null || u.idCliente() != ((Number) idCliente).intValue()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private ResponseEntity<byte[]> responder(String plantilla, Map<String, Object> datos, String nombre) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(nombre + ".pdf").build().toString())
                .body(pdf.generar(plantilla, datos));
    }
}
