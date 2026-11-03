package com.uniquindio.frutimarket.reportes;

import static com.uniquindio.frutimarket.comun.Columna.de;
import static com.uniquindio.frutimarket.comun.Columna.texto;

import com.uniquindio.frutimarket.comun.Formato;
import com.uniquindio.frutimarket.reportes.Consulta.Seccion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Consultas de datos especificos (distintas de los reportes, que son listados para decidir). */
@Component
public class CatalogoConsultas {

    private static final String LISTA_CLIENTES = "SELECT id_cliente, CONCAT(nombre, ' - ', tipo_cliente) FROM cliente ORDER BY nombre";
    private final Map<String, Consulta> consultas = new LinkedHashMap<>();

    public CatalogoConsultas() {
        agregar(new Consulta("compras-cliente", "Productos que compró un cliente",
                "Qué productos, cuántos kilos y cuánto ha gastado un cliente en cada uno.", "basket",
                List.of(Parametro.sql("cliente", "Cliente", LISTA_CLIENTES, false)),
                List.of(new Seccion("Cliente", """
                                SELECT c.nombre, c.tipo_cliente, CONCAT(c.tipo_documento, ' ', c.numero_documento) AS documento, c.telefono, c.email,
                                       c.direccion, m.nombre AS municipio, c.fecha_registro
                                  FROM cliente c JOIN municipio m ON m.id_municipio = c.id_municipio WHERE c.id_cliente = :cliente""",
                                List.of(texto("nombre", "Nombre"), de("tipo_cliente", "Tipo", Formato.ESTADO), texto("documento", "Documento"),
                                        texto("telefono", "Teléfono"), texto("email", "Correo"), texto("direccion", "Dirección"),
                                        texto("municipio", "Municipio"), de("fecha_registro", "Cliente desde", Formato.FECHA)), true),
                        new Seccion("Productos comprados", """
                                SELECT CONCAT(pr.nombre, ' ', pr.variedad) AS producto, COUNT(DISTINCT p.id_pedido) AS veces,
                                       SUM(d.cantidad_kg) AS kg, SUM(d.subtotal) AS total, MAX(p.fecha_pedido) AS ultima
                                  FROM pedido p JOIN detalle_pedido d ON d.id_pedido = p.id_pedido
                                  JOIN lote l ON l.id_lote = d.id_lote JOIN producto pr ON pr.id_producto = l.id_producto
                                 WHERE p.id_cliente = :cliente AND p.estado <> 'CANCELADO'
                                 GROUP BY pr.id_producto, pr.nombre, pr.variedad ORDER BY total DESC""",
                                List.of(texto("producto", "Producto"), de("veces", "Pedidos", Formato.ENTERO), de("kg", "Kilos", Formato.KG).conTotal(),
                                        de("total", "Total", Formato.MONEDA).conTotal(), de("ultima", "Última vez", Formato.FECHA)), false))));

        agregar(new Consulta("pedidos-cliente", "Pedidos de un cliente",
                "Historial de pedidos de un cliente con su estado, pago y entrega.", "bag-check",
                List.of(Parametro.sql("cliente", "Cliente", LISTA_CLIENTES, false),
                        Parametro.opciones("estado", "Estado", true, "PENDIENTE", "PAGADO", "DESPACHADO", "ENTREGADO", "CANCELADO")),
                List.of(new Seccion("Pedidos", """
                                SELECT p.id_pedido, p.fecha_pedido, pa.nombre AS punto, p.estado, p.total,
                                       (SELECT f.numero FROM factura f WHERE f.id_pedido = p.id_pedido) AS factura,
                                       (SELECT d.numero_guia FROM despacho d WHERE d.id_pedido = p.id_pedido) AS guia
                                  FROM pedido p JOIN punto_acopio pa ON pa.id_punto = p.id_punto
                                 WHERE p.id_cliente = :cliente AND (:estado = '' OR p.estado = :estado)
                                 ORDER BY p.fecha_pedido DESC""",
                        List.of(de("id_pedido", "N.º", Formato.ENTERO), de("fecha_pedido", "Fecha", Formato.FECHA_HORA), texto("punto", "Punto"),
                                de("estado", "Estado", Formato.ESTADO), de("total", "Total", Formato.MONEDA).conTotal(),
                                texto("factura", "Factura"), texto("guia", "Guía")), false))));

        agregar(new Consulta("detalle-pedido", "Detalle de un pedido",
                "Encabezado, productos con el lote y la finca de donde salieron, pagos y despacho de un pedido.", "receipt",
                List.of(Parametro.entero("pedido", "Número de pedido", "MAX_PEDIDO")),
                List.of(new Seccion("Pedido", """
                                SELECT p.id_pedido, p.fecha_pedido, c.nombre AS cliente, pa.nombre AS punto, p.estado,
                                       p.fecha_entrega_programada, p.direccion_entrega, p.subtotal, p.descuento, p.iva, p.total
                                  FROM pedido p JOIN cliente c ON c.id_cliente = p.id_cliente JOIN punto_acopio pa ON pa.id_punto = p.id_punto
                                 WHERE p.id_pedido = :pedido""",
                                List.of(de("id_pedido", "Pedido", Formato.ENTERO), de("fecha_pedido", "Fecha", Formato.FECHA_HORA), texto("cliente", "Cliente"),
                                        texto("punto", "Punto de acopio"), de("estado", "Estado", Formato.ESTADO),
                                        de("fecha_entrega_programada", "Entrega", Formato.FECHA), texto("direccion_entrega", "Dirección"),
                                        de("subtotal", "Subtotal", Formato.MONEDA), de("descuento", "Descuento", Formato.MONEDA),
                                        de("iva", "IVA", Formato.MONEDA), de("total", "Total", Formato.MONEDA)), true),
                        new Seccion("Productos", """
                                SELECT d.linea, CONCAT(pr.nombre, ' ', pr.variedad) AS producto, l.codigo AS lote, f.nombre AS finca,
                                       d.cantidad_kg, d.precio_kg, d.subtotal
                                  FROM detalle_pedido d JOIN lote l ON l.id_lote = d.id_lote JOIN producto pr ON pr.id_producto = l.id_producto
                                  JOIN finca f ON f.id_finca = l.id_finca WHERE d.id_pedido = :pedido ORDER BY d.linea""",
                                List.of(de("linea", "#", Formato.ENTERO), texto("producto", "Producto"), texto("lote", "Lote"), texto("finca", "Finca"),
                                        de("cantidad_kg", "Kilos", Formato.KG).conTotal(), de("precio_kg", "Precio kg", Formato.MONEDA),
                                        de("subtotal", "Subtotal", Formato.MONEDA).conTotal()), false),
                        new Seccion("Pagos", """
                                SELECT pg.fecha_pago, mp.nombre AS metodo, pg.referencia, pg.valor, pg.estado
                                  FROM pago pg JOIN metodo_pago mp ON mp.id_metodo = pg.id_metodo WHERE pg.id_pedido = :pedido ORDER BY pg.fecha_pago""",
                                List.of(de("fecha_pago", "Fecha", Formato.FECHA_HORA), texto("metodo", "Medio"), texto("referencia", "Referencia"),
                                        de("valor", "Valor", Formato.MONEDA), de("estado", "Estado", Formato.ESTADO)), false))));

        agregar(new Consulta("trazabilidad-lote", "Trazabilidad de un lote",
                "De qué finca y campesino viene un lote y a qué clientes se vendió.", "diagram-3",
                List.of(Parametro.texto("codigo", "Código del lote", "PRIMER_LOTE")),
                List.of(new Seccion("Origen del lote", """
                                SELECT l.codigo, CONCAT(pr.nombre, ' ', pr.variedad) AS producto, l.calidad, l.estado, l.fecha_cosecha, l.fecha_vencimiento,
                                       l.cantidad_kg, l.cantidad_disponible_kg, l.precio_finca_kg, f.nombre AS finca,
                                       CONCAT(f.vereda, ', ', m.nombre) AS ubicacion, f.altitud_msnm,
                                       CONCAT(ca.nombres, ' ', ca.apellidos) AS campesino, ca.telefono, pa.nombre AS punto
                                  FROM lote l JOIN producto pr ON pr.id_producto = l.id_producto JOIN finca f ON f.id_finca = l.id_finca
                                  JOIN municipio m ON m.id_municipio = f.id_municipio JOIN campesino ca ON ca.id_campesino = f.id_campesino
                                  JOIN punto_acopio pa ON pa.id_punto = l.id_punto WHERE l.codigo = :codigo""",
                                List.of(texto("codigo", "Lote"), texto("producto", "Producto"), de("calidad", "Calidad", Formato.ESTADO),
                                        de("estado", "Estado", Formato.ESTADO), de("fecha_cosecha", "Cosecha", Formato.FECHA),
                                        de("fecha_vencimiento", "Vence", Formato.FECHA), de("cantidad_kg", "Recibido", Formato.KG),
                                        de("cantidad_disponible_kg", "Disponible", Formato.KG), de("precio_finca_kg", "Precio en finca", Formato.MONEDA),
                                        texto("finca", "Finca"), texto("ubicacion", "Vereda y municipio"), de("altitud_msnm", "Altitud (m)", Formato.ENTERO),
                                        texto("campesino", "Campesino"), texto("telefono", "Teléfono"), texto("punto", "Recibido en")), true),
                        new Seccion("Vendido a", """
                                SELECT p.id_pedido, p.fecha_pedido, c.nombre AS cliente, m.nombre AS municipio, d.cantidad_kg, p.estado
                                  FROM detalle_pedido d JOIN lote l ON l.id_lote = d.id_lote JOIN pedido p ON p.id_pedido = d.id_pedido
                                  JOIN cliente c ON c.id_cliente = p.id_cliente JOIN municipio m ON m.id_municipio = c.id_municipio
                                 WHERE l.codigo = :codigo ORDER BY p.fecha_pedido""",
                                List.of(de("id_pedido", "Pedido", Formato.ENTERO), de("fecha_pedido", "Fecha", Formato.FECHA_HORA), texto("cliente", "Cliente"),
                                        texto("municipio", "Municipio"), de("cantidad_kg", "Kilos", Formato.KG).conTotal(),
                                        de("estado", "Estado", Formato.ESTADO)), false))));

        agregar(new Consulta("ficha-campesino", "Datos de un campesino",
                "Datos de contacto, fincas y lo que ha entregado un productor.", "person-badge",
                List.of(Parametro.sql("campesino", "Campesino", "SELECT id_campesino, CONCAT(nombres, ' ', apellidos) FROM campesino ORDER BY nombres", false)),
                List.of(new Seccion("Campesino", """
                                SELECT CONCAT(ca.nombres, ' ', ca.apellidos) AS nombre, CONCAT(ca.tipo_documento, ' ', ca.numero_documento) AS documento,
                                       ca.telefono, ca.email, m.nombre AS municipio, ca.fecha_vinculacion, ca.certificado_bpa,
                                       (SELECT COALESCE(SUM(l.cantidad_kg), 0) FROM lote l JOIN finca f ON f.id_finca = l.id_finca
                                         WHERE f.id_campesino = ca.id_campesino) AS kg_entregados,
                                       (SELECT COALESCE(SUM(l.cantidad_kg * l.precio_finca_kg), 0) FROM lote l JOIN finca f ON f.id_finca = l.id_finca
                                         WHERE f.id_campesino = ca.id_campesino) AS valor_liquidado
                                  FROM campesino ca JOIN municipio m ON m.id_municipio = ca.id_municipio WHERE ca.id_campesino = :campesino""",
                                List.of(texto("nombre", "Nombre"), texto("documento", "Documento"), texto("telefono", "Teléfono"), texto("email", "Correo"),
                                        texto("municipio", "Municipio"), de("fecha_vinculacion", "Vinculado desde", Formato.FECHA),
                                        de("certificado_bpa", "Certificado BPA", Formato.BOOLEANO), de("kg_entregados", "Kilos entregados", Formato.KG),
                                        de("valor_liquidado", "Valor liquidado", Formato.MONEDA)), true),
                        new Seccion("Fincas", """
                                SELECT f.nombre, f.vereda, m.nombre AS municipio, f.area_hectareas, f.altitud_msnm,
                                       COUNT(l.id_lote) AS lotes, COALESCE(SUM(l.cantidad_kg), 0) AS kg
                                  FROM finca f JOIN municipio m ON m.id_municipio = f.id_municipio LEFT JOIN lote l ON l.id_finca = f.id_finca
                                 WHERE f.id_campesino = :campesino GROUP BY f.id_finca, f.nombre, f.vereda, m.nombre, f.area_hectareas, f.altitud_msnm""",
                                List.of(texto("nombre", "Finca"), texto("vereda", "Vereda"), texto("municipio", "Municipio"),
                                        de("area_hectareas", "Área (ha)", Formato.NUMERO), de("altitud_msnm", "Altitud", Formato.ENTERO),
                                        de("lotes", "Lotes", Formato.ENTERO).conTotal(), de("kg", "Kilos", Formato.KG).conTotal()), false))));

        agregar(new Consulta("existencias-producto", "Existencias de un producto",
                "Lotes vigentes de un producto en cada punto de acopio, del que vence primero al último.", "boxes",
                List.of(Parametro.sql("producto", "Producto", "SELECT id_producto, CONCAT(nombre, ' ', variedad) FROM producto ORDER BY nombre", false)),
                List.of(new Seccion("Lotes disponibles", """
                                SELECT pa.nombre AS punto, l.codigo, l.calidad, l.fecha_cosecha, l.fecha_vencimiento, l.cantidad_disponible_kg, f.nombre AS finca
                                  FROM lote l JOIN punto_acopio pa ON pa.id_punto = l.id_punto JOIN finca f ON f.id_finca = l.id_finca
                                 WHERE l.id_producto = :producto AND l.estado = 'DISPONIBLE' AND l.cantidad_disponible_kg > 0
                                   AND l.fecha_vencimiento >= CURDATE()
                                 ORDER BY l.fecha_vencimiento""",
                        List.of(texto("punto", "Punto"), texto("codigo", "Lote"), de("calidad", "Calidad", Formato.ESTADO),
                                de("fecha_cosecha", "Cosecha", Formato.FECHA), de("fecha_vencimiento", "Vence", Formato.FECHA),
                                de("cantidad_disponible_kg", "Disponible", Formato.KG).conTotal(), texto("finca", "Finca")), false))));

        agregar(new Consulta("suscripciones-cliente", "Plan y pagos de un cliente",
                "Suscripciones que ha tenido un cliente y los pagos de cada una.", "arrow-repeat",
                List.of(Parametro.sql("cliente", "Cliente", "SELECT DISTINCT c.id_cliente, c.nombre FROM cliente c JOIN suscripcion s ON s.id_cliente = c.id_cliente ORDER BY c.nombre", false)),
                List.of(new Seccion("Suscripciones y pagos", """
                                SELECT s.id_suscripcion, pl.nombre AS plan, s.fecha_inicio, s.fecha_fin, s.estado, pg.fecha_pago, mp.nombre AS metodo, pg.valor
                                  FROM suscripcion s JOIN plan pl ON pl.id_plan = s.id_plan
                                  LEFT JOIN pago pg ON pg.id_suscripcion = s.id_suscripcion
                                  LEFT JOIN metodo_pago mp ON mp.id_metodo = pg.id_metodo
                                 WHERE s.id_cliente = :cliente ORDER BY s.fecha_inicio DESC""",
                        List.of(de("id_suscripcion", "N.º", Formato.ENTERO), texto("plan", "Plan"), de("fecha_inicio", "Inicio", Formato.FECHA),
                                de("fecha_fin", "Fin", Formato.FECHA), de("estado", "Estado", Formato.ESTADO), de("fecha_pago", "Pagado el", Formato.FECHA),
                                texto("metodo", "Medio"), de("valor", "Valor", Formato.MONEDA).conTotal()), false))));

        agregar(new Consulta("personal-punto", "Personal de un punto de acopio",
                "Empleados de un punto con su cargo y datos de contacto.", "people",
                List.of(Parametro.sql("punto", "Punto de acopio", "SELECT id_punto, nombre FROM punto_acopio ORDER BY nombre", false)),
                List.of(new Seccion("Empleados", """
                                SELECT CONCAT(e.nombres, ' ', e.apellidos) AS nombre, e.cargo, e.telefono, e.email, e.fecha_ingreso, e.activo
                                  FROM empleado e WHERE e.id_punto = :punto ORDER BY e.cargo, nombre""",
                        List.of(texto("nombre", "Nombre"), texto("cargo", "Cargo"), texto("telefono", "Teléfono"), texto("email", "Correo"),
                                de("fecha_ingreso", "Ingreso", Formato.FECHA), de("activo", "Activo", Formato.BOOLEANO)), false))));
    }

    private void agregar(Consulta c) {
        consultas.put(c.clave(), c);
    }

    public List<Consulta> todas() {
        return List.copyOf(consultas.values());
    }

    public Optional<Consulta> buscar(String clave) {
        return Optional.ofNullable(consultas.get(clave));
    }
}
