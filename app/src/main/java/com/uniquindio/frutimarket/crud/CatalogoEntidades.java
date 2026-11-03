package com.uniquindio.frutimarket.crud;

import static com.uniquindio.frutimarket.comun.Columna.de;
import static com.uniquindio.frutimarket.comun.Columna.texto;
import static com.uniquindio.frutimarket.crud.Campo.booleano;
import static com.uniquindio.frutimarket.crud.Campo.decimal;
import static com.uniquindio.frutimarket.crud.Campo.email;
import static com.uniquindio.frutimarket.crud.Campo.entero;
import static com.uniquindio.frutimarket.crud.Campo.fecha;
import static com.uniquindio.frutimarket.crud.Campo.fechaHora;
import static com.uniquindio.frutimarket.crud.Campo.fk;
import static com.uniquindio.frutimarket.crud.Campo.largo;
import static com.uniquindio.frutimarket.crud.Campo.moneda;
import static com.uniquindio.frutimarket.crud.Campo.opciones;
import static com.uniquindio.frutimarket.crud.Campo.telefono;
import static com.uniquindio.frutimarket.crud.Campo.texto;

import com.uniquindio.frutimarket.comun.Formato;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Las 20 entidades fuertes del modelo con su registro, modificacion, eliminacion y consulta.
 * (detalle_pedido, la entidad debil, se gestiona desde la pantalla del pedido.)
 */
@Component
public class CatalogoEntidades {

    private static final String FK_MUNICIPIO = "SELECT id_municipio, CONCAT(nombre, ' (', departamento, ')') FROM municipio ORDER BY departamento DESC, nombre";
    private static final String FK_PUNTO = "SELECT id_punto, nombre FROM punto_acopio ORDER BY nombre";
    private static final String FK_CLIENTE = "SELECT id_cliente, nombre FROM cliente ORDER BY nombre";
    private static final String FK_USUARIO_LIBRE = "SELECT id_usuario, CONCAT(username, ' - ', nombre_completo) FROM usuario ORDER BY username";

    private final Map<String, Entidad> entidades = new LinkedHashMap<>();

    public CatalogoEntidades() {
        // ------------------------------------------------------------- Comercial
        agregar(new Entidad("clientes", "cliente", "id_cliente", "Clientes", "cliente", "people", "Comercial")
                .descripcion("Hogares, restaurantes, hoteles, tiendas y supermercados que compran en FrutiMarket.")
                .lista("""
                        SELECT c.id_cliente AS id, c.nombre, c.tipo_cliente, CONCAT(c.tipo_documento, ' ', c.numero_documento) AS documento,
                               c.telefono, c.email, c.direccion, m.nombre AS municipio, c.fecha_registro, c.activo,
                               u.username AS usuario,
                               (SELECT COUNT(*) FROM pedido p WHERE p.id_cliente = c.id_cliente) AS pedidos
                          FROM cliente c JOIN municipio m ON m.id_municipio = c.id_municipio
                          LEFT JOIN usuario u ON u.id_usuario = c.id_usuario""")
                .orden("nombre")
                .col(texto("nombre", "Nombre / razón social")).col(de("tipo_cliente", "Tipo", Formato.ESTADO))
                .col(texto("documento", "Documento")).col(texto("telefono", "Teléfono")).col(texto("municipio", "Municipio"))
                .col(de("pedidos", "Pedidos", Formato.ENTERO)).det(texto("email", "Correo")).det(texto("direccion", "Dirección"))
                .det(de("fecha_registro", "Fecha de registro", Formato.FECHA)).det(texto("usuario", "Usuario del sistema"))
                .det(de("activo", "Activo", Formato.BOOLEANO))
                .campo(texto("nombre", "Nombre o razón social", 120))
                .campo(opciones("tipo_cliente", "Tipo de cliente", "HOGAR", "RESTAURANTE", "HOTEL", "TIENDA", "SUPERMERCADO"))
                .campo(opciones("tipo_documento", "Tipo de documento", "CC", "CE", "NIT"))
                .campo(texto("numero_documento", "Número de documento", 20).ayuda("NIT con dígito de verificación, p. ej. 900456123-4"))
                .campo(telefono("telefono", "Teléfono")).campo(email("email", "Correo electrónico"))
                .campo(texto("direccion", "Dirección", 150)).campo(fk("id_municipio", "Municipio", FK_MUNICIPIO))
                .campo(fecha("fecha_registro", "Fecha de registro"))
                .campo(fk("id_usuario", "Usuario vinculado", FK_USUARIO_LIBRE).opcional())
                .campo(booleano("activo", "Activo")));

        agregar(new Entidad("pedidos", "pedido", "id_pedido", "Pedidos", "pedido", "bag-check", "Comercial")
                .descripcion("Pedidos de los clientes. Se crean desde «Nuevo pedido», que descuenta el inventario por lote (primero el que vence antes).")
                .lista("""
                        SELECT p.id_pedido AS id, p.fecha_pedido, c.nombre AS cliente, pa.nombre AS punto, p.estado,
                               p.fecha_entrega_programada, p.subtotal, p.descuento, p.iva, p.total, p.direccion_entrega,
                               p.observaciones, pl.nombre AS plan
                          FROM pedido p JOIN cliente c ON c.id_cliente = p.id_cliente
                          JOIN punto_acopio pa ON pa.id_punto = p.id_punto
                          LEFT JOIN suscripcion s ON s.id_suscripcion = p.id_suscripcion
                          LEFT JOIN plan pl ON pl.id_plan = s.id_plan""")
                .col(de("id", "N.º", Formato.ENTERO)).col(de("fecha_pedido", "Fecha", Formato.FECHA_HORA)).col(texto("cliente", "Cliente"))
                .col(texto("punto", "Punto de acopio")).col(de("estado", "Estado", Formato.ESTADO)).col(de("total", "Total", Formato.MONEDA))
                .det(de("fecha_entrega_programada", "Entrega programada", Formato.FECHA)).det(texto("direccion_entrega", "Dirección de entrega"))
                .det(de("subtotal", "Subtotal", Formato.MONEDA)).det(de("descuento", "Descuento", Formato.MONEDA)).det(de("iva", "IVA", Formato.MONEDA))
                .det(texto("plan", "Plan aplicado")).det(texto("observaciones", "Observaciones"))
                .campo(fecha("fecha_entrega_programada", "Fecha de entrega programada"))
                .campo(texto("direccion_entrega", "Dirección de entrega", 150))
                .campo(largo("observaciones", "Observaciones", 300).opcional())
                .crearEn("/pedidos/nuevo").detalleEn("/pedidos/{id}"));

        agregar(new Entidad("planes", "plan", "id_plan", "Planes", "plan", "box2-heart", "Comercial")
                .descripcion("Planes de suscripción (canastas periódicas) que los clientes pagan en línea.")
                .lista("""
                        SELECT pl.id_plan AS id, pl.nombre, pl.periodicidad, pl.precio, pl.kg_incluidos, pl.descuento_pct, pl.activo, pl.descripcion,
                               (SELECT COUNT(*) FROM suscripcion s WHERE s.id_plan = pl.id_plan AND s.estado = 'ACTIVA') AS activas
                          FROM plan pl""").orden("precio")
                .col(texto("nombre", "Plan")).col(de("periodicidad", "Periodicidad", Formato.ESTADO)).col(de("precio", "Precio", Formato.MONEDA))
                .col(de("kg_incluidos", "Kg incluidos", Formato.KG)).col(de("descuento_pct", "Descuento", Formato.PORCENTAJE))
                .col(de("activas", "Suscripciones activas", Formato.ENTERO)).det(texto("descripcion", "Descripción")).det(de("activo", "Activo", Formato.BOOLEANO))
                .campo(texto("nombre", "Nombre", 60)).campo(largo("descripcion", "Descripción", 300))
                .campo(opciones("periodicidad", "Periodicidad", "SEMANAL", "QUINCENAL", "MENSUAL"))
                .campo(moneda("precio", "Precio")).campo(decimal("kg_incluidos", "Kg incluidos"))
                .campo(decimal("descuento_pct", "Descuento en compras adicionales (%)").ayuda("Entre 0 y 30"))
                .campo(booleano("activo", "Activo")));

        agregar(new Entidad("suscripciones", "suscripcion", "id_suscripcion", "Suscripciones", "suscripción", "arrow-repeat", "Comercial")
                .descripcion("Planes contratados por cada cliente, con su vigencia y renovación.")
                .lista("""
                        SELECT s.id_suscripcion AS id, c.nombre AS cliente, pl.nombre AS plan, s.fecha_inicio, s.fecha_fin, s.estado,
                               s.renovacion_automatica, pl.precio
                          FROM suscripcion s JOIN cliente c ON c.id_cliente = s.id_cliente JOIN plan pl ON pl.id_plan = s.id_plan""")
                .col(texto("cliente", "Cliente")).col(texto("plan", "Plan")).col(de("fecha_inicio", "Inicio", Formato.FECHA))
                .col(de("fecha_fin", "Fin", Formato.FECHA)).col(de("estado", "Estado", Formato.ESTADO)).col(de("precio", "Valor", Formato.MONEDA))
                .det(de("renovacion_automatica", "Renovación automática", Formato.BOOLEANO))
                .campo(fk("id_cliente", "Cliente", FK_CLIENTE)).campo(fk("id_plan", "Plan", "SELECT id_plan, CONCAT(nombre, ' - ', periodicidad) FROM plan ORDER BY precio"))
                .campo(fecha("fecha_inicio", "Fecha de inicio")).campo(fecha("fecha_fin", "Fecha de fin"))
                .campo(opciones("estado", "Estado", "ACTIVA", "PAUSADA", "CANCELADA", "VENCIDA"))
                .campo(booleano("renovacion_automatica", "Renovación automática")));

        agregar(new Entidad("pagos", "pago", "id_pago", "Pagos", "pago", "credit-card", "Comercial")
                .descripcion("Pagos en línea de pedidos y suscripciones. Los nuevos se registran desde la pasarela del pedido o del plan.")
                .lista("""
                        SELECT pg.id_pago AS id, pg.fecha_pago, pg.referencia, mp.nombre AS metodo, pg.valor, pg.estado,
                               CASE WHEN pg.id_pedido IS NOT NULL THEN CONCAT('Pedido #', pg.id_pedido) ELSE CONCAT('Plan: ', pl.nombre) END AS concepto,
                               COALESCE(c1.nombre, c2.nombre) AS cliente
                          FROM pago pg JOIN metodo_pago mp ON mp.id_metodo = pg.id_metodo
                          LEFT JOIN pedido p ON p.id_pedido = pg.id_pedido LEFT JOIN cliente c1 ON c1.id_cliente = p.id_cliente
                          LEFT JOIN suscripcion s ON s.id_suscripcion = pg.id_suscripcion LEFT JOIN plan pl ON pl.id_plan = s.id_plan
                          LEFT JOIN cliente c2 ON c2.id_cliente = s.id_cliente""")
                .col(de("fecha_pago", "Fecha", Formato.FECHA_HORA)).col(texto("referencia", "Referencia")).col(texto("cliente", "Cliente"))
                .col(texto("concepto", "Concepto")).col(texto("metodo", "Medio")).col(de("valor", "Valor", Formato.MONEDA)).col(de("estado", "Estado", Formato.ESTADO))
                .campo(fk("id_metodo", "Medio de pago", "SELECT id_metodo, nombre FROM metodo_pago ORDER BY nombre"))
                .campo(opciones("estado", "Estado", "APROBADO", "RECHAZADO", "PENDIENTE", "REEMBOLSADO"))
                .sinCrear());

        agregar(new Entidad("facturas", "factura", "id_factura", "Facturas", "factura", "receipt", "Comercial")
                .descripcion("Facturas de venta generadas automáticamente al aprobarse el pago de un pedido.")
                .lista("""
                        SELECT f.id_factura AS id, f.numero, f.fecha_emision, c.nombre AS cliente, f.id_pedido AS pedido,
                               f.subtotal, f.descuento, f.iva, f.total
                          FROM factura f JOIN pedido p ON p.id_pedido = f.id_pedido JOIN cliente c ON c.id_cliente = p.id_cliente""")
                .col(texto("numero", "Número")).col(de("fecha_emision", "Emisión", Formato.FECHA_HORA)).col(texto("cliente", "Cliente"))
                .col(de("pedido", "Pedido", Formato.ENTERO)).col(de("total", "Total", Formato.MONEDA))
                .det(de("subtotal", "Subtotal", Formato.MONEDA)).det(de("descuento", "Descuento", Formato.MONEDA)).det(de("iva", "IVA", Formato.MONEDA))
                .sinCrear().sinEditar());

        agregar(new Entidad("notificaciones", "notificacion", "id_notificacion", "Notificaciones", "notificación", "bell", "Comercial")
                .descripcion("Recordatorios y avisos personalizados que recibe cada usuario.")
                .lista("""
                        SELECT n.id_notificacion AS id, n.fecha_programada, u.username AS usuario, n.tipo, n.titulo, n.mensaje, n.leida, n.fecha_creacion
                          FROM notificacion n JOIN usuario u ON u.id_usuario = n.id_usuario""")
                .orden("fecha_programada DESC")
                .col(de("fecha_programada", "Programada", Formato.FECHA_HORA)).col(texto("usuario", "Usuario")).col(de("tipo", "Tipo", Formato.ESTADO))
                .col(texto("titulo", "Título")).col(de("leida", "Leída", Formato.BOOLEANO)).det(texto("mensaje", "Mensaje"))
                .det(de("fecha_creacion", "Creada", Formato.FECHA_HORA))
                .campo(fk("id_usuario", "Usuario", FK_USUARIO_LIBRE))
                .campo(opciones("tipo", "Tipo", "RECORDATORIO", "PEDIDO", "PAGO", "SUSCRIPCION", "INVENTARIO", "SISTEMA"))
                .campo(texto("titulo", "Título", 100)).campo(largo("mensaje", "Mensaje", 400))
                .campo(fechaHora("fecha_programada", "Mostrar a partir de")).campo(booleano("leida", "Leída")));

        // ------------------------------------------------------------- Produccion e inventario
        agregar(new Entidad("lotes", "lote", "id_lote", "Lotes", "lote", "boxes", "Producción")
                .descripcion("Cosechas recibidas en los puntos de acopio. El inventario disponible lo actualizan los triggers al vender.")
                .lista("""
                        SELECT l.id_lote AS id, l.codigo, CONCAT(pr.nombre, ' ', pr.variedad) AS producto, f.nombre AS finca,
                               CONCAT(ca.nombres, ' ', ca.apellidos) AS campesino, pa.nombre AS punto, l.fecha_cosecha, l.fecha_vencimiento,
                               l.cantidad_kg, l.cantidad_disponible_kg, l.precio_finca_kg, l.calidad, l.estado
                          FROM lote l JOIN producto pr ON pr.id_producto = l.id_producto JOIN finca f ON f.id_finca = l.id_finca
                          JOIN campesino ca ON ca.id_campesino = f.id_campesino JOIN punto_acopio pa ON pa.id_punto = l.id_punto""")
                .col(texto("codigo", "Código")).col(texto("producto", "Producto")).col(texto("punto", "Punto"))
                .col(de("fecha_vencimiento", "Vence", Formato.FECHA)).col(de("cantidad_disponible_kg", "Disponible", Formato.KG))
                .col(de("calidad", "Calidad", Formato.ESTADO)).col(de("estado", "Estado", Formato.ESTADO))
                .det(texto("finca", "Finca de origen")).det(texto("campesino", "Campesino")).det(de("fecha_cosecha", "Fecha de cosecha", Formato.FECHA))
                .det(de("cantidad_kg", "Cantidad recibida", Formato.KG)).det(de("precio_finca_kg", "Precio pagado al campesino (kg)", Formato.MONEDA))
                .campo(texto("codigo", "Código", 20).ayuda("Formato LT-AAMM-0000"))
                .campo(fk("id_finca", "Finca", "SELECT f.id_finca, CONCAT(f.nombre, ' - ', ca.nombres, ' ', ca.apellidos) FROM finca f JOIN campesino ca ON ca.id_campesino = f.id_campesino ORDER BY f.nombre"))
                .campo(fk("id_producto", "Producto", "SELECT id_producto, CONCAT(nombre, ' ', variedad) FROM producto ORDER BY nombre"))
                .campo(fk("id_punto", "Punto de acopio", FK_PUNTO))
                .campo(fecha("fecha_cosecha", "Fecha de cosecha")).campo(fecha("fecha_vencimiento", "Fecha de vencimiento"))
                .campo(decimal("cantidad_kg", "Cantidad recibida (kg)").minimo(0.1))
                .campo(decimal("cantidad_disponible_kg", "Cantidad disponible (kg)").ayuda("Al registrar, igual a la recibida"))
                .campo(moneda("precio_finca_kg", "Precio pagado al campesino por kg"))
                .campo(opciones("calidad", "Calidad", "EXTRA", "PRIMERA", "SEGUNDA"))
                .campo(opciones("estado", "Estado", "DISPONIBLE", "AGOTADO", "VENCIDO")));

        agregar(new Entidad("productos", "producto", "id_producto", "Productos", "producto", "basket", "Producción")
                .descripcion("Catálogo de frutas y productos campesinos con su precio de venta por kilo.")
                .lista("""
                        SELECT pr.id_producto AS id, pr.nombre, pr.variedad, c.nombre AS categoria, pr.precio_venta_kg, pr.iva_pct,
                               pr.vida_util_dias, pr.requiere_refrigeracion, pr.activo, pr.unidad_medida
                          FROM producto pr JOIN categoria c ON c.id_categoria = pr.id_categoria""").orden("nombre")
                .col(texto("nombre", "Producto")).col(texto("variedad", "Variedad")).col(texto("categoria", "Categoría"))
                .col(de("precio_venta_kg", "Precio kg", Formato.MONEDA)).col(de("vida_util_dias", "Vida útil (días)", Formato.ENTERO))
                .col(de("requiere_refrigeracion", "Refrigerado", Formato.BOOLEANO)).det(de("iva_pct", "IVA", Formato.PORCENTAJE))
                .det(texto("unidad_medida", "Unidad")).det(de("activo", "Activo", Formato.BOOLEANO))
                .campo(texto("nombre", "Nombre", 80)).campo(texto("variedad", "Variedad", 60))
                .campo(fk("id_categoria", "Categoría", "SELECT id_categoria, nombre FROM categoria ORDER BY nombre"))
                .campo(opciones("unidad_medida", "Unidad de medida", "KG"))
                .campo(moneda("precio_venta_kg", "Precio de venta por kg").minimo(1))
                .campo(opciones("iva_pct", "IVA (%)", "0", "5", "19"))
                .campo(entero("vida_util_dias", "Vida útil (días)").minimo(1))
                .campo(booleano("requiere_refrigeracion", "Requiere refrigeración")).campo(booleano("activo", "Activo")));

        agregar(new Entidad("categorias", "categoria", "id_categoria", "Categorías", "categoría", "tags", "Producción")
                .lista("""
                        SELECT c.id_categoria AS id, c.nombre, c.descripcion,
                               (SELECT COUNT(*) FROM producto p WHERE p.id_categoria = c.id_categoria) AS productos
                          FROM categoria c""").orden("nombre")
                .col(texto("nombre", "Categoría")).col(texto("descripcion", "Descripción")).col(de("productos", "Productos", Formato.ENTERO))
                .campo(texto("nombre", "Nombre", 60)).campo(largo("descripcion", "Descripción", 250)));

        agregar(new Entidad("campesinos", "campesino", "id_campesino", "Campesinos", "campesino", "person-badge", "Producción")
                .descripcion("Productores vinculados que entregan sus cosechas en los puntos de acopio.")
                .lista("""
                        SELECT ca.id_campesino AS id, CONCAT(ca.nombres, ' ', ca.apellidos) AS nombre, CONCAT(ca.tipo_documento, ' ', ca.numero_documento) AS documento,
                               ca.telefono, ca.email, m.nombre AS municipio, ca.fecha_vinculacion, ca.certificado_bpa, ca.activo, u.username AS usuario,
                               (SELECT COUNT(*) FROM finca f WHERE f.id_campesino = ca.id_campesino) AS fincas
                          FROM campesino ca JOIN municipio m ON m.id_municipio = ca.id_municipio
                          LEFT JOIN usuario u ON u.id_usuario = ca.id_usuario""").orden("nombre")
                .col(texto("nombre", "Nombre")).col(texto("documento", "Documento")).col(texto("telefono", "Teléfono"))
                .col(texto("municipio", "Municipio")).col(de("fincas", "Fincas", Formato.ENTERO)).col(de("certificado_bpa", "BPA", Formato.BOOLEANO))
                .det(texto("email", "Correo")).det(de("fecha_vinculacion", "Vinculación", Formato.FECHA)).det(texto("usuario", "Usuario del sistema"))
                .det(de("activo", "Activo", Formato.BOOLEANO))
                .campo(texto("nombres", "Nombres", 60)).campo(texto("apellidos", "Apellidos", 60))
                .campo(opciones("tipo_documento", "Tipo de documento", "CC", "CE", "PPT"))
                .campo(texto("numero_documento", "Número de documento", 20))
                .campo(telefono("telefono", "Teléfono")).campo(email("email", "Correo electrónico").opcional())
                .campo(fk("id_municipio", "Municipio de residencia", FK_MUNICIPIO))
                .campo(fecha("fecha_vinculacion", "Fecha de vinculación"))
                .campo(booleano("certificado_bpa", "Certificado BPA (ICA)"))
                .campo(fk("id_usuario", "Usuario vinculado", FK_USUARIO_LIBRE).opcional())
                .campo(booleano("activo", "Activo")));

        agregar(new Entidad("fincas", "finca", "id_finca", "Fincas", "finca", "tree", "Producción")
                .lista("""
                        SELECT f.id_finca AS id, f.nombre, CONCAT(ca.nombres, ' ', ca.apellidos) AS campesino, f.vereda, m.nombre AS municipio,
                               f.area_hectareas, f.altitud_msnm,
                               (SELECT COUNT(*) FROM lote l WHERE l.id_finca = f.id_finca) AS lotes
                          FROM finca f JOIN campesino ca ON ca.id_campesino = f.id_campesino JOIN municipio m ON m.id_municipio = f.id_municipio""")
                .orden("nombre")
                .col(texto("nombre", "Finca")).col(texto("campesino", "Propietario")).col(texto("vereda", "Vereda"))
                .col(texto("municipio", "Municipio")).col(de("area_hectareas", "Área (ha)", Formato.NUMERO)).col(de("altitud_msnm", "Altitud (m s. n. m.)", Formato.ENTERO))
                .det(de("lotes", "Lotes entregados", Formato.ENTERO))
                .campo(texto("nombre", "Nombre de la finca", 80))
                .campo(fk("id_campesino", "Propietario", "SELECT id_campesino, CONCAT(nombres, ' ', apellidos) FROM campesino ORDER BY nombres"))
                .campo(fk("id_municipio", "Municipio", FK_MUNICIPIO)).campo(texto("vereda", "Vereda", 80))
                .campo(decimal("area_hectareas", "Área (hectáreas)").minimo(0.01))
                .campo(entero("altitud_msnm", "Altitud (m s. n. m.)")));

        // ------------------------------------------------------------- Logistica
        agregar(new Entidad("puntos", "punto_acopio", "id_punto", "Puntos de acopio", "punto de acopio", "building", "Logística")
                .lista("""
                        SELECT pa.id_punto AS id, pa.nombre, m.nombre AS municipio, pa.direccion, pa.telefono, pa.capacidad_kg, pa.refrigerado, pa.activo,
                               (SELECT COUNT(*) FROM empleado e WHERE e.id_punto = pa.id_punto AND e.activo = 1) AS empleados
                          FROM punto_acopio pa JOIN municipio m ON m.id_municipio = pa.id_municipio""").orden("nombre")
                .col(texto("nombre", "Nombre")).col(texto("municipio", "Municipio")).col(texto("direccion", "Dirección"))
                .col(de("capacidad_kg", "Capacidad", Formato.KG)).col(de("refrigerado", "Cuarto frío", Formato.BOOLEANO))
                .col(de("empleados", "Empleados", Formato.ENTERO)).det(texto("telefono", "Teléfono")).det(de("activo", "Activo", Formato.BOOLEANO))
                .campo(texto("nombre", "Nombre", 80)).campo(fk("id_municipio", "Municipio", FK_MUNICIPIO))
                .campo(texto("direccion", "Dirección", 120)).campo(telefono("telefono", "Teléfono"))
                .campo(decimal("capacidad_kg", "Capacidad (kg)").minimo(1)).campo(booleano("refrigerado", "Tiene cuarto frío"))
                .campo(booleano("activo", "Activo")));

        agregar(new Entidad("despachos", "despacho", "id_despacho", "Despachos", "despacho", "truck", "Logística")
                .descripcion("Guías de despacho. Se generan al despachar un pedido pagado.")
                .lista("""
                        SELECT d.id_despacho AS id, d.numero_guia, d.id_pedido AS pedido, c.nombre AS cliente, t.nombre AS transportador, t.placa,
                               d.fecha_salida, d.fecha_entrega, d.estado, d.temperatura_c, d.observaciones
                          FROM despacho d JOIN pedido p ON p.id_pedido = d.id_pedido JOIN cliente c ON c.id_cliente = p.id_cliente
                          JOIN transportador t ON t.id_transportador = d.id_transportador""")
                .col(texto("numero_guia", "Guía")).col(de("pedido", "Pedido", Formato.ENTERO)).col(texto("cliente", "Cliente"))
                .col(texto("transportador", "Transportador")).col(de("fecha_salida", "Salida", Formato.FECHA_HORA)).col(de("estado", "Estado", Formato.ESTADO))
                .det(texto("placa", "Placa")).det(de("fecha_entrega", "Entrega", Formato.FECHA_HORA))
                .det(de("temperatura_c", "Temperatura (°C)", Formato.NUMERO)).det(texto("observaciones", "Observaciones"))
                .campo(fk("id_transportador", "Transportador", "SELECT id_transportador, CONCAT(nombre, ' - ', placa) FROM transportador ORDER BY nombre"))
                .campo(fechaHora("fecha_salida", "Fecha y hora de salida"))
                .campo(fechaHora("fecha_entrega", "Fecha y hora de entrega").opcional())
                .campo(opciones("estado", "Estado", "EN_RUTA", "ENTREGADO", "DEVUELTO"))
                .campo(decimal("temperatura_c", "Temperatura de la carga (°C)").opcional())
                .campo(largo("observaciones", "Observaciones", 300).opcional())
                .sinCrear());

        agregar(new Entidad("transportadores", "transportador", "id_transportador", "Transportadores", "transportador", "truck-front", "Logística")
                .lista("""
                        SELECT t.id_transportador AS id, t.nombre, t.numero_documento, t.telefono, t.placa, t.tipo_vehiculo, t.capacidad_kg, t.refrigerado, t.activo,
                               (SELECT COUNT(*) FROM despacho d WHERE d.id_transportador = t.id_transportador) AS despachos
                          FROM transportador t""").orden("nombre")
                .col(texto("nombre", "Nombre")).col(texto("placa", "Placa")).col(texto("tipo_vehiculo", "Vehículo"))
                .col(de("capacidad_kg", "Capacidad", Formato.KG)).col(de("refrigerado", "Refrigerado", Formato.BOOLEANO)).col(de("despachos", "Despachos", Formato.ENTERO))
                .det(texto("numero_documento", "Documento / NIT")).det(texto("telefono", "Teléfono")).det(de("activo", "Activo", Formato.BOOLEANO))
                .campo(texto("nombre", "Nombre o razón social", 120)).campo(texto("numero_documento", "Documento o NIT", 20))
                .campo(telefono("telefono", "Teléfono")).campo(texto("placa", "Placa", 6).ayuda("Seis caracteres, p. ej. TKM482"))
                .campo(opciones("tipo_vehiculo", "Tipo de vehículo", "Camión NHR", "Camión NPR", "Turbo", "Furgón refrigerado", "Camioneta D-Max", "Furgoneta eléctrica"))
                .campo(decimal("capacidad_kg", "Capacidad (kg)").minimo(1)).campo(booleano("refrigerado", "Refrigerado"))
                .campo(booleano("activo", "Activo")));

        agregar(new Entidad("metodos", "metodo_pago", "id_metodo", "Medios de pago", "medio de pago", "wallet2", "Logística")
                .lista("""
                        SELECT mp.id_metodo AS id, mp.nombre, mp.tipo, mp.comision_pct, mp.en_linea, mp.activo,
                               (SELECT COUNT(*) FROM pago pg WHERE pg.id_metodo = mp.id_metodo) AS pagos
                          FROM metodo_pago mp""").orden("nombre")
                .col(texto("nombre", "Medio")).col(de("tipo", "Tipo", Formato.ESTADO)).col(de("comision_pct", "Comisión", Formato.PORCENTAJE))
                .col(de("en_linea", "En línea", Formato.BOOLEANO)).col(de("pagos", "Pagos", Formato.ENTERO)).det(de("activo", "Activo", Formato.BOOLEANO))
                .campo(texto("nombre", "Nombre", 40)).campo(opciones("tipo", "Tipo", "TARJETA", "BILLETERA", "PSE", "EFECTIVO", "TRANSFERENCIA"))
                .campo(decimal("comision_pct", "Comisión de la pasarela (%)")).campo(booleano("en_linea", "Se paga en línea"))
                .campo(booleano("activo", "Activo")));

        agregar(new Entidad("municipios", "municipio", "id_municipio", "Municipios", "municipio", "geo-alt", "Logística")
                .lista("""
                        SELECT m.id_municipio AS id, m.nombre, m.departamento, m.codigo_dane,
                               (SELECT COUNT(*) FROM cliente c WHERE c.id_municipio = m.id_municipio) AS clientes,
                               (SELECT COUNT(*) FROM finca f WHERE f.id_municipio = m.id_municipio) AS fincas
                          FROM municipio m""").orden("nombre")
                .col(texto("nombre", "Municipio")).col(texto("departamento", "Departamento")).col(texto("codigo_dane", "Código DANE"))
                .col(de("clientes", "Clientes", Formato.ENTERO)).col(de("fincas", "Fincas", Formato.ENTERO))
                .campo(texto("nombre", "Nombre", 80)).campo(texto("departamento", "Departamento", 60))
                .campo(texto("codigo_dane", "Código DANE", 5).ayuda("Cinco dígitos, p. ej. 63001")));

        // ------------------------------------------------------------- Administracion
        agregar(new Entidad("empleados", "empleado", "id_empleado", "Empleados", "empleado", "person-vcard", "Administración")
                .soloAdmin()
                .lista("""
                        SELECT e.id_empleado AS id, CONCAT(e.nombres, ' ', e.apellidos) AS nombre, e.numero_documento, e.cargo, pa.nombre AS punto,
                               e.salario, e.fecha_ingreso, e.telefono, e.email, e.activo, u.username AS usuario
                          FROM empleado e JOIN punto_acopio pa ON pa.id_punto = e.id_punto LEFT JOIN usuario u ON u.id_usuario = e.id_usuario""")
                .orden("nombre")
                .col(texto("nombre", "Nombre")).col(texto("cargo", "Cargo")).col(texto("punto", "Punto de acopio"))
                .col(de("salario", "Salario", Formato.MONEDA)).col(de("fecha_ingreso", "Ingreso", Formato.FECHA)).col(de("activo", "Activo", Formato.BOOLEANO))
                .det(texto("numero_documento", "Cédula")).det(texto("telefono", "Teléfono")).det(texto("email", "Correo")).det(texto("usuario", "Usuario del sistema"))
                .campo(texto("nombres", "Nombres", 60)).campo(texto("apellidos", "Apellidos", 60))
                .campo(texto("numero_documento", "Cédula", 20)).campo(texto("cargo", "Cargo", 40))
                .campo(fk("id_punto", "Punto de acopio", FK_PUNTO))
                .campo(moneda("salario", "Salario mensual").minimo(1750905).ayuda("No puede ser menor al SMMLV 2026: $ 1.750.905"))
                .campo(fecha("fecha_ingreso", "Fecha de ingreso")).campo(telefono("telefono", "Teléfono"))
                .campo(email("email", "Correo")).campo(fk("id_usuario", "Usuario vinculado", FK_USUARIO_LIBRE).opcional())
                .campo(booleano("activo", "Activo")));

        agregar(new Entidad("usuarios", "usuario", "id_usuario", "Usuarios", "usuario", "person-lock", "Administración")
                .soloAdmin()
                .descripcion("Cuentas de acceso al sistema. Las contraseñas se guardan cifradas con BCrypt.")
                .lista("""
                        SELECT u.id_usuario AS id, u.username, u.nombre_completo, r.nombre AS rol, u.email, u.telefono, u.activo,
                               u.fecha_registro, u.ultimo_acceso
                          FROM usuario u JOIN rol r ON r.id_rol = u.id_rol""").orden("username")
                .col(texto("username", "Usuario")).col(texto("nombre_completo", "Nombre")).col(de("rol", "Rol", Formato.ESTADO))
                .col(texto("email", "Correo")).col(de("ultimo_acceso", "Último acceso", Formato.FECHA_HORA)).col(de("activo", "Activo", Formato.BOOLEANO))
                .det(texto("telefono", "Teléfono")).det(de("fecha_registro", "Registro", Formato.FECHA_HORA))
                .campo(texto("username", "Usuario", 40)).campo(Campo.clave("password_hash", "Contraseña"))
                .campo(texto("nombre_completo", "Nombre completo", 120))
                .campo(fk("id_rol", "Rol", "SELECT id_rol, nombre FROM rol ORDER BY id_rol"))
                .campo(email("email", "Correo")).campo(telefono("telefono", "Teléfono").opcional())
                .campo(booleano("activo", "Activo")));

        agregar(new Entidad("roles", "rol", "id_rol", "Roles", "rol", "shield-lock", "Administración")
                .soloAdmin()
                .lista("""
                        SELECT r.id_rol AS id, r.nombre, r.descripcion, (SELECT COUNT(*) FROM usuario u WHERE u.id_rol = r.id_rol) AS usuarios
                          FROM rol r""").orden("id")
                .col(de("nombre", "Rol", Formato.ESTADO)).col(texto("descripcion", "Descripción")).col(de("usuarios", "Usuarios", Formato.ENTERO))
                .campo(texto("nombre", "Nombre", 30)).campo(largo("descripcion", "Descripción", 200)));
    }

    private void agregar(Entidad e) {
        entidades.put(e.getClave(), e);
    }

    public Optional<Entidad> buscar(String clave) {
        return Optional.ofNullable(entidades.get(clave));
    }

    public List<Entidad> todas() {
        return List.copyOf(entidades.values());
    }

    /** Entidades agrupadas para el menu lateral */
    public Map<String, List<Entidad>> porGrupo(boolean admin) {
        Map<String, List<Entidad>> m = new LinkedHashMap<>();
        for (Entidad e : entidades.values()) {
            if (!e.isSoloAdmin() || admin) {
                m.computeIfAbsent(e.getGrupo(), g -> new java.util.ArrayList<>()).add(e);
            }
        }
        return m;
    }
}
