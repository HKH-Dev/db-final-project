package com.uniquindio.frutimarket.reportes;

import static com.uniquindio.frutimarket.comun.Columna.de;
import static com.uniquindio.frutimarket.comun.Columna.texto;

import com.uniquindio.frutimarket.comun.Formato;
import com.uniquindio.frutimarket.reportes.Grafico.Tipo;
import com.uniquindio.frutimarket.reportes.Reporte.Nivel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Los 10 reportes del sistema: 3 simples (una tabla), 4 intermedios (multitabla con GROUP BY / HAVING)
 * y 3 complejos (con subconsultas). Cinco de ellos llevan grafico estadistico.
 */
@Component
public class CatalogoReportes {

    private static final String NO_CANCELADO = "p.estado <> 'CANCELADO'";
    private final Map<String, Reporte> reportes = new LinkedHashMap<>();

    public CatalogoReportes() {
        // =============================================================== SIMPLES
        agregar(new Reporte("catalogo-precios", 1, Nivel.SIMPLE,
                "Catálogo de productos y precios",
                "Lista de precios vigente por kilo, con IVA, vida útil y si requiere cadena de frío.",
                "Sirve como lista de precios para los clientes y para revisar qué productos necesitan cuarto frío.",
                List.of("producto"), "WHERE, CASE, ORDER BY",
                """
                SELECT nombre, variedad, precio_venta_kg, iva_pct,
                       ROUND(precio_venta_kg * (1 + iva_pct / 100)) AS precio_con_iva,
                       vida_util_dias,
                       CASE WHEN requiere_refrigeracion = 1 THEN 'Refrigerado' ELSE 'Ambiente' END AS conservacion
                  FROM producto
                 WHERE activo = 1
                   AND (:conservacion = '' OR requiere_refrigeracion = (:conservacion = 'Refrigerado'))
                 ORDER BY nombre, variedad
                """,
                List.of(Parametro.opciones("conservacion", "Conservación", true, "Refrigerado", "Ambiente")),
                List.of(texto("nombre", "Producto"), texto("variedad", "Variedad"), de("precio_venta_kg", "Precio kg", Formato.MONEDA),
                        de("iva_pct", "IVA", Formato.PORCENTAJE), de("precio_con_iva", "Precio kg con IVA", Formato.MONEDA),
                        de("vida_util_dias", "Vida útil (días)", Formato.ENTERO), texto("conservacion", "Conservación")),
                null));

        agregar(new Reporte("nomina", 2, Nivel.SIMPLE,
                "Nómina de empleados con su salario",
                "Empleados activos con cargo, antigüedad y salario mensual; al final, el total de la nómina.",
                "Permite presupuestar el costo laboral mensual y revisar la equidad salarial por cargo.",
                List.of("empleado"), "WHERE, TIMESTAMPDIFF, ORDER BY",
                """
                SELECT CONCAT(nombres, ' ', apellidos) AS empleado, numero_documento, cargo, fecha_ingreso,
                       TIMESTAMPDIFF(YEAR, fecha_ingreso, CURDATE()) AS antiguedad, salario,
                       ROUND(salario * 12) AS costo_anual
                  FROM empleado
                 WHERE activo = 1
                   AND (:cargo = '' OR cargo = :cargo)
                 ORDER BY salario DESC, empleado
                """,
                List.of(Parametro.sql("cargo", "Cargo", "SELECT DISTINCT cargo, cargo FROM empleado ORDER BY cargo", true)),
                List.of(texto("empleado", "Empleado"), texto("numero_documento", "Cédula"), texto("cargo", "Cargo"),
                        de("fecha_ingreso", "Ingreso", Formato.FECHA), de("antiguedad", "Antigüedad (años)", Formato.ENTERO),
                        de("salario", "Salario mensual", Formato.MONEDA).conTotal(), de("costo_anual", "Salario anual", Formato.MONEDA).conTotal()),
                null));

        agregar(new Reporte("lotes-por-vencer", 3, Nivel.SIMPLE,
                "Lotes próximos a vencer",
                "Lotes con existencias que vencen en los próximos días, con el valor que se perdería si no se venden.",
                "Indica qué producto rotar o poner en promoción hoy para reducir la merma.",
                List.of("lote"), "WHERE, BETWEEN, DATEDIFF, ORDER BY",
                """
                SELECT codigo, calidad, fecha_cosecha, fecha_vencimiento,
                       DATEDIFF(fecha_vencimiento, CURDATE()) AS dias_restantes,
                       cantidad_kg, cantidad_disponible_kg, precio_finca_kg,
                       ROUND(cantidad_disponible_kg * precio_finca_kg) AS valor_en_riesgo
                  FROM lote
                 WHERE estado = 'DISPONIBLE'
                   AND cantidad_disponible_kg > 0
                   AND fecha_vencimiento BETWEEN CURDATE() AND CURDATE() + INTERVAL :dias DAY
                 ORDER BY fecha_vencimiento, valor_en_riesgo DESC
                """,
                List.of(Parametro.entero("dias", "Vencen en los próximos (días)", "4")),
                List.of(texto("codigo", "Lote"), de("calidad", "Calidad", Formato.ESTADO), de("fecha_cosecha", "Cosecha", Formato.FECHA),
                        de("fecha_vencimiento", "Vence", Formato.FECHA), de("dias_restantes", "Días", Formato.ENTERO),
                        de("cantidad_kg", "Recibido", Formato.KG), de("cantidad_disponible_kg", "Disponible", Formato.KG).conTotal(),
                        de("precio_finca_kg", "Costo kg", Formato.MONEDA), de("valor_en_riesgo", "Valor en riesgo", Formato.MONEDA).conTotal()),
                null));

        // =============================================================== INTERMEDIOS
        agregar(new Reporte("ventas-mensuales", 4, Nivel.INTERMEDIO,
                "Ventas realizadas mes a mes",
                "Pedidos, kilos vendidos, valor y ticket promedio por mes del año elegido (sin pedidos cancelados).",
                "Muestra la tendencia del negocio y la temporada alta (vacaciones de mitad de año y Semana Santa).",
                List.of("pedido", "detalle_pedido"), "JOIN, WHERE, GROUP BY, ELT",
                """
                SELECT MONTH(p.fecha_pedido) AS mes_num,
                       ELT(MONTH(p.fecha_pedido), 'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio',
                           'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre') AS mes,
                       COUNT(DISTINCT p.id_pedido) AS pedidos,
                       COUNT(DISTINCT p.id_cliente) AS clientes,
                       SUM(d.cantidad_kg) AS kg,
                       SUM(d.subtotal) AS ventas,
                       ROUND(SUM(d.subtotal) / COUNT(DISTINCT p.id_pedido)) AS ticket_promedio
                  FROM pedido p
                  JOIN detalle_pedido d ON d.id_pedido = p.id_pedido
                 WHERE %s
                   AND YEAR(p.fecha_pedido) = :anio
                 GROUP BY MONTH(p.fecha_pedido)
                 ORDER BY mes_num
                """.formatted(NO_CANCELADO),
                List.of(Parametro.entero("anio", "Año", "ANIO_ACTUAL")),
                List.of(texto("mes", "Mes"), de("pedidos", "Pedidos", Formato.ENTERO).conTotal(), de("clientes", "Clientes distintos", Formato.ENTERO),
                        de("kg", "Kilos vendidos", Formato.KG).conTotal(), de("ventas", "Ventas", Formato.MONEDA).conTotal(),
                        de("ticket_promedio", "Ticket promedio", Formato.MONEDA)),
                Grafico.de(Tipo.BARRAS, "Ventas por mes", "mes", Formato.MONEDA, "ventas", "Ventas")));

        agregar(new Reporte("ventas-categoria", 5, Nivel.INTERMEDIO,
                "Ventas por categoría de producto",
                "Participación de cada categoría en las ventas de un periodo, con kilos y precio promedio obtenido.",
                "Ayuda a decidir en qué líneas de producto invertir y con qué campesinos ampliar acuerdos.",
                List.of("categoria", "producto", "lote", "detalle_pedido", "pedido"), "JOIN ×4, WHERE, GROUP BY, HAVING",
                """
                SELECT c.nombre AS categoria,
                       COUNT(DISTINCT pr.id_producto) AS productos,
                       COUNT(DISTINCT d.id_pedido) AS pedidos,
                       SUM(d.cantidad_kg) AS kg,
                       SUM(d.subtotal) AS ventas,
                       ROUND(SUM(d.subtotal) / SUM(d.cantidad_kg)) AS precio_promedio_kg
                  FROM categoria c
                  JOIN producto pr       ON pr.id_categoria = c.id_categoria
                  JOIN lote l            ON l.id_producto = pr.id_producto
                  JOIN detalle_pedido d  ON d.id_lote = l.id_lote
                  JOIN pedido p          ON p.id_pedido = d.id_pedido
                 WHERE %s
                   AND p.fecha_pedido >= :desde AND p.fecha_pedido < :hasta + INTERVAL 1 DAY
                 GROUP BY c.id_categoria, c.nombre
                HAVING SUM(d.subtotal) >= :minimo
                 ORDER BY ventas DESC
                """.formatted(NO_CANCELADO),
                List.of(Parametro.fecha("desde", "Desde", "INICIO_ANIO"), Parametro.fecha("hasta", "Hasta", "HOY"),
                        Parametro.entero("minimo", "Ventas mínimas ($)", "0")),
                List.of(texto("categoria", "Categoría"), de("productos", "Productos", Formato.ENTERO), de("pedidos", "Pedidos", Formato.ENTERO),
                        de("kg", "Kilos", Formato.KG).conTotal(), de("ventas", "Ventas", Formato.MONEDA).conTotal(),
                        de("precio_promedio_kg", "Precio promedio kg", Formato.MONEDA)),
                Grafico.de(Tipo.DONA, "Participación en ventas", "categoria", Formato.MONEDA, "ventas", "Ventas")));

        agregar(new Reporte("clientes-frecuentes", 6, Nivel.INTERMEDIO,
                "Clientes frecuentes y su volumen de compra",
                "Clientes con un número mínimo de pedidos en el año, su total comprado, ticket promedio y última compra.",
                "Identifica a quién ofrecer un plan de suscripción o condiciones comerciales especiales.",
                List.of("cliente", "municipio", "pedido"), "JOIN, WHERE, GROUP BY, HAVING, AVG, MAX",
                """
                SELECT cl.nombre, cl.tipo_cliente, m.nombre AS municipio,
                       COUNT(p.id_pedido) AS pedidos,
                       SUM(p.total) AS total_comprado,
                       ROUND(AVG(p.total)) AS ticket_promedio,
                       MAX(p.fecha_pedido) AS ultima_compra
                  FROM cliente cl
                  JOIN municipio m ON m.id_municipio = cl.id_municipio
                  JOIN pedido p    ON p.id_cliente = cl.id_cliente
                 WHERE %s
                   AND YEAR(p.fecha_pedido) = :anio
                   AND (:tipo = '' OR cl.tipo_cliente = :tipo)
                 GROUP BY cl.id_cliente, cl.nombre, cl.tipo_cliente, m.nombre
                HAVING COUNT(p.id_pedido) >= :min_pedidos
                 ORDER BY total_comprado DESC
                """.formatted(NO_CANCELADO),
                List.of(Parametro.entero("anio", "Año", "ANIO_ACTUAL"), Parametro.entero("min_pedidos", "Mínimo de pedidos", "20"),
                        Parametro.opciones("tipo", "Tipo de cliente", true, "HOGAR", "RESTAURANTE", "HOTEL", "TIENDA", "SUPERMERCADO")),
                List.of(texto("nombre", "Cliente"), de("tipo_cliente", "Tipo", Formato.ESTADO), texto("municipio", "Municipio"),
                        de("pedidos", "Pedidos", Formato.ENTERO).conTotal(), de("total_comprado", "Total comprado", Formato.MONEDA).conTotal(),
                        de("ticket_promedio", "Ticket promedio", Formato.MONEDA), de("ultima_compra", "Última compra", Formato.FECHA)),
                null));

        agregar(new Reporte("recaudo-medios", 7, Nivel.INTERMEDIO,
                "Recaudo y comisiones por medio de pago",
                "Transacciones, tasa de aprobación, dinero recaudado (pedidos y planes) y comisión de la pasarela por medio de pago.",
                "Permite negociar comisiones y decidir qué medios promover en el pago en línea.",
                List.of("metodo_pago", "pago"), "JOIN, WHERE, GROUP BY, HAVING, CASE dentro de SUM",
                """
                SELECT mp.nombre AS metodo, mp.tipo,
                       COUNT(*) AS transacciones,
                       ROUND(100 * SUM(CASE WHEN pg.estado = 'APROBADO' THEN 1 ELSE 0 END) / COUNT(*), 1) AS tasa_aprobacion,
                       SUM(CASE WHEN pg.estado = 'APROBADO' AND pg.id_pedido IS NOT NULL THEN pg.valor ELSE 0 END) AS recaudo_pedidos,
                       SUM(CASE WHEN pg.estado = 'APROBADO' AND pg.id_suscripcion IS NOT NULL THEN pg.valor ELSE 0 END) AS recaudo_planes,
                       SUM(CASE WHEN pg.estado = 'APROBADO' THEN pg.valor ELSE 0 END) AS recaudo,
                       ROUND(SUM(CASE WHEN pg.estado = 'APROBADO' THEN pg.valor * mp.comision_pct / 100 ELSE 0 END)) AS comision
                  FROM metodo_pago mp
                  JOIN pago pg ON pg.id_metodo = mp.id_metodo
                 WHERE YEAR(pg.fecha_pago) = :anio
                   AND (:mes = 0 OR MONTH(pg.fecha_pago) = :mes)
                 GROUP BY mp.id_metodo, mp.nombre, mp.tipo
                HAVING COUNT(*) > 0
                 ORDER BY recaudo DESC
                """,
                List.of(Parametro.entero("anio", "Año", "ANIO_ACTUAL"), Parametro.opciones("mes", "Mes", false,
                        "0|Todo el año", "1|Enero", "2|Febrero", "3|Marzo", "4|Abril", "5|Mayo", "6|Junio", "7|Julio", "8|Agosto",
                        "9|Septiembre", "10|Octubre", "11|Noviembre", "12|Diciembre")),
                List.of(texto("metodo", "Medio de pago"), de("tipo", "Tipo", Formato.ESTADO), de("transacciones", "Transacciones", Formato.ENTERO).conTotal(),
                        de("tasa_aprobacion", "Aprobación", Formato.PORCENTAJE), de("recaudo_pedidos", "Pedidos", Formato.MONEDA).conTotal(),
                        de("recaudo_planes", "Planes", Formato.MONEDA).conTotal(), de("recaudo", "Recaudo total", Formato.MONEDA).conTotal(),
                        de("comision", "Comisión", Formato.MONEDA).conTotal()),
                Grafico.de(Tipo.BARRAS_H, "Recaudo por medio de pago", "metodo", Formato.MONEDA,
                        "recaudo_pedidos", "Pedidos", "recaudo_planes", "Planes")));

        // =============================================================== COMPLEJOS
        agregar(new Reporte("productos-estrella", 8, Nivel.COMPLEJO,
                "Productos estrella: ventas sobre el promedio",
                "Productos cuyas ventas del año superan el promedio de ventas por producto, con su margen frente al precio pagado en finca.",
                "Señala los productos que sostienen el negocio y cuánto margen deja cada uno para fijar precios.",
                List.of("producto", "categoria", "lote", "detalle_pedido", "pedido"), "Tabla derivada, subconsulta escalar en WHERE y SELECT, AVG de agregados",
                """
                SELECT pr.nombre, pr.variedad, cat.nombre AS categoria,
                       t.kg, t.ventas, t.costo_finca,
                       ROUND(100 * (t.ventas - t.costo_finca) / t.ventas, 1) AS margen_pct,
                       ROUND(100 * t.ventas / (SELECT SUM(d3.subtotal)
                                                 FROM detalle_pedido d3 JOIN pedido p3 ON p3.id_pedido = d3.id_pedido
                                                WHERE p3.estado <> 'CANCELADO' AND YEAR(p3.fecha_pedido) = :anio), 1) AS participacion
                  FROM (SELECT l.id_producto,
                               SUM(d.cantidad_kg) AS kg,
                               SUM(d.subtotal) AS ventas,
                               ROUND(SUM(d.cantidad_kg * l.precio_finca_kg)) AS costo_finca
                          FROM detalle_pedido d
                          JOIN lote l   ON l.id_lote = d.id_lote
                          JOIN pedido p ON p.id_pedido = d.id_pedido
                         WHERE p.estado <> 'CANCELADO' AND YEAR(p.fecha_pedido) = :anio
                         GROUP BY l.id_producto) t
                  JOIN producto pr   ON pr.id_producto = t.id_producto
                  JOIN categoria cat ON cat.id_categoria = pr.id_categoria
                 WHERE t.ventas > (SELECT AVG(v.ventas)
                                     FROM (SELECT SUM(d2.subtotal) AS ventas
                                             FROM detalle_pedido d2
                                             JOIN lote l2   ON l2.id_lote = d2.id_lote
                                             JOIN pedido p2 ON p2.id_pedido = d2.id_pedido
                                            WHERE p2.estado <> 'CANCELADO' AND YEAR(p2.fecha_pedido) = :anio
                                            GROUP BY l2.id_producto) v)
                 ORDER BY t.ventas DESC
                """,
                List.of(Parametro.entero("anio", "Año", "ANIO_ACTUAL")),
                List.of(texto("nombre", "Producto"), texto("variedad", "Variedad"), texto("categoria", "Categoría"),
                        de("kg", "Kilos", Formato.KG).conTotal(), de("ventas", "Ventas", Formato.MONEDA).conTotal(),
                        de("costo_finca", "Pagado a campesinos", Formato.MONEDA).conTotal(), de("margen_pct", "Margen", Formato.PORCENTAJE),
                        de("participacion", "Participación", Formato.PORCENTAJE).conTotal()),
                Grafico.de(Tipo.BARRAS, "Ventas frente a lo pagado en finca", "nombre", Formato.MONEDA,
                        "ventas", "Ventas", "costo_finca", "Pagado a campesinos")));

        agregar(new Reporte("suscriptores-inactivos", 9, Nivel.COMPLEJO,
                "Suscriptores en riesgo de abandono",
                "Clientes con un plan vigente que no han hecho pedidos en los últimos días, con lo que han pagado en planes.",
                "Lista de clientes para llamar y retener antes de que cancelen o no renueven su plan.",
                List.of("cliente", "suscripcion", "plan", "pedido", "pago"), "NOT EXISTS, subconsultas correlacionadas en SELECT",
                """
                SELECT cl.nombre, cl.tipo_cliente, cl.telefono, pl.nombre AS plan, s.estado, s.fecha_fin,
                       (SELECT MAX(p.fecha_pedido) FROM pedido p
                         WHERE p.id_cliente = cl.id_cliente AND p.estado <> 'CANCELADO') AS ultima_compra,
                       DATEDIFF(CURDATE(), (SELECT MAX(p.fecha_pedido) FROM pedido p
                                             WHERE p.id_cliente = cl.id_cliente AND p.estado <> 'CANCELADO')) AS dias_sin_comprar,
                       (SELECT COALESCE(SUM(pg.valor), 0)
                          FROM pago pg JOIN suscripcion s2 ON s2.id_suscripcion = pg.id_suscripcion
                         WHERE s2.id_cliente = cl.id_cliente AND pg.estado = 'APROBADO') AS pagado_en_planes
                  FROM cliente cl
                  JOIN suscripcion s ON s.id_cliente = cl.id_cliente AND s.estado IN ('ACTIVA', 'PAUSADA')
                  JOIN plan pl       ON pl.id_plan = s.id_plan
                 WHERE NOT EXISTS (SELECT 1 FROM pedido p
                                    WHERE p.id_cliente = cl.id_cliente
                                      AND p.estado <> 'CANCELADO'
                                      AND p.fecha_pedido >= CURDATE() - INTERVAL :dias DAY)
                 ORDER BY dias_sin_comprar DESC
                """,
                List.of(Parametro.entero("dias", "Sin pedidos en los últimos (días)", "5")),
                List.of(texto("nombre", "Cliente"), de("tipo_cliente", "Tipo", Formato.ESTADO), texto("telefono", "Teléfono"),
                        texto("plan", "Plan"), de("estado", "Estado", Formato.ESTADO), de("fecha_fin", "Vence", Formato.FECHA),
                        de("ultima_compra", "Última compra", Formato.FECHA), de("dias_sin_comprar", "Días sin comprar", Formato.ENTERO),
                        de("pagado_en_planes", "Pagado en planes", Formato.MONEDA).conTotal()),
                null));

        agregar(new Reporte("desempeno-acopios", 10, Nivel.COMPLEJO,
                "Desempeño de los puntos de acopio",
                "Por punto: kilos recibidos, vendidos y vencidos (merma), ventas, participación en el total y ventas por empleado.",
                "Compara la eficiencia de cada punto para reasignar personal, inventario o cuartos fríos.",
                List.of("punto_acopio", "municipio", "empleado", "lote", "pedido", "detalle_pedido"),
                "Tabla derivada con 6 subconsultas correlacionadas, subconsulta escalar para el total",
                """
                SELECT x.*,
                       ROUND(100 * x.kg_vencidos / NULLIF(x.kg_recibidos, 0), 1) AS merma_pct,
                       ROUND(100 * x.ventas / (SELECT SUM(p.total) FROM pedido p
                                                WHERE p.estado <> 'CANCELADO' AND YEAR(p.fecha_pedido) = :anio), 1) AS participacion,
                       ROUND(x.ventas / NULLIF(x.empleados, 0)) AS ventas_por_empleado
                  FROM (SELECT pa.nombre AS punto, m.nombre AS municipio,
                               (SELECT COUNT(*) FROM empleado e WHERE e.id_punto = pa.id_punto AND e.activo = 1) AS empleados,
                               (SELECT COALESCE(SUM(l.cantidad_kg), 0) FROM lote l
                                 WHERE l.id_punto = pa.id_punto AND YEAR(l.fecha_cosecha) = :anio) AS kg_recibidos,
                               (SELECT COALESCE(SUM(d.cantidad_kg), 0) FROM detalle_pedido d JOIN pedido p ON p.id_pedido = d.id_pedido
                                 WHERE p.id_punto = pa.id_punto AND p.estado <> 'CANCELADO' AND YEAR(p.fecha_pedido) = :anio) AS kg_vendidos,
                               (SELECT COALESCE(SUM(l.cantidad_disponible_kg), 0) FROM lote l
                                 WHERE l.id_punto = pa.id_punto AND l.estado = 'VENCIDO' AND YEAR(l.fecha_cosecha) = :anio) AS kg_vencidos,
                               (SELECT COALESCE(SUM(p.total), 0) FROM pedido p
                                 WHERE p.id_punto = pa.id_punto AND p.estado <> 'CANCELADO' AND YEAR(p.fecha_pedido) = :anio) AS ventas
                          FROM punto_acopio pa
                          JOIN municipio m ON m.id_municipio = pa.id_municipio) x
                 ORDER BY x.ventas DESC
                """,
                List.of(Parametro.entero("anio", "Año", "ANIO_ACTUAL")),
                List.of(texto("punto", "Punto de acopio"), texto("municipio", "Municipio"), de("empleados", "Empleados", Formato.ENTERO).conTotal(),
                        de("kg_recibidos", "Recibido", Formato.KG).conTotal(), de("kg_vendidos", "Vendido", Formato.KG).conTotal(),
                        de("kg_vencidos", "Vencido", Formato.KG).conTotal(), de("merma_pct", "Merma", Formato.PORCENTAJE),
                        de("ventas", "Ventas", Formato.MONEDA).conTotal(), de("participacion", "Participación", Formato.PORCENTAJE).conTotal(),
                        de("ventas_por_empleado", "Ventas por empleado", Formato.MONEDA)),
                Grafico.de(Tipo.BARRAS, "Kilos vendidos frente a vencidos", "punto", Formato.KG,
                        "kg_vendidos", "Vendido", "kg_vencidos", "Vencido")));
    }

    private void agregar(Reporte r) {
        reportes.put(r.clave(), r);
    }

    public List<Reporte> todos() {
        return List.copyOf(reportes.values());
    }

    public Optional<Reporte> buscar(String clave) {
        return Optional.ofNullable(reportes.get(clave));
    }
}
