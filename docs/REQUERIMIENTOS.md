# Requerimientos funcionales · FrutiMarket BD

Documento para aprobación del docente. Proyecto final de Bases de Datos, Universidad del Quindío 2026-2.

**Caso de estudio:** plataforma que conecta a clientes (hogares, restaurantes, hoteles, tiendas) con campesinos del
Quindío a través de puntos de acopio, con planes de suscripción y pago en línea. Adapta el caso de turismo y hotelería:
consumidores, prestadores de servicios e intermediarios; registro y perfil de usuario; recordatorios y notificaciones;
planes y pago en línea.

## 1. Seguridad y cuenta

| Código | Requerimiento |
|---|---|
| RF-01 | Iniciar sesión con usuario (o correo) y contraseña validados contra la base de datos; contraseñas cifradas con BCrypt. |
| RF-02 | Cerrar sesión. |
| RF-03 | Registrar una cuenta de cliente (datos personales o de empresa, dirección, municipio, usuario y contraseña) con validación de formato y de datos únicos. |
| RF-04 | Consultar y modificar el perfil propio; cambiar la contraseña verificando la actual. |
| RF-05 | Restringir las opciones según el rol: administrador, operador de acopio, campesino o cliente. |

## 2. Gestión de información (registro, modificación, eliminación y consulta)

| Código | Entidad | Código | Entidad |
|---|---|---|---|
| RF-06 | Municipios | RF-16 | Planes de suscripción |
| RF-07 | Roles | RF-17 | Medios de pago |
| RF-08 | Usuarios | RF-18 | Suscripciones |
| RF-09 | Campesinos | RF-19 | Lotes de cosecha |
| RF-10 | Fincas | RF-20 | Pedidos |
| RF-11 | Categorías | RF-21 | Pagos |
| RF-12 | Productos | RF-22 | Facturas (consulta y eliminación) |
| RF-13 | Puntos de acopio | RF-23 | Despachos |
| RF-14 | Empleados | RF-24 | Notificaciones |
| RF-15 | Transportadores | RF-25 | Clientes |

Cada entidad permite listar con búsqueda, orden y paginación, ver la ficha, registrar, modificar y eliminar. La
eliminación de registros con dependencias se impide y se informa con un mensaje claro.

## 3. Proceso de venta

| Código | Requerimiento |
|---|---|
| RF-26 | Crear un pedido eligiendo punto de acopio, cliente, productos y kilos; el sistema asigna los lotes que vencen primero y descuenta el inventario (trigger). |
| RF-27 | Aplicar automáticamente el descuento del plan de suscripción vigente del cliente. |
| RF-28 | Pagar un pedido en línea eligiendo el medio de pago (pasarela simulada, con opción de simular rechazo). |
| RF-29 | Generar la factura de venta al aprobarse el pago. |
| RF-30 | Despachar un pedido pagado asignando transportador (validando capacidad) y generar la guía. |
| RF-31 | Marcar un pedido como entregado. |
| RF-32 | Cancelar un pedido pendiente o pagado, devolviendo el inventario y registrando el reembolso. |
| RF-33 | Suscribirse a un plan con pago en línea y cancelar la suscripción. |

## 4. Recordatorios y notificaciones

| Código | Requerimiento |
|---|---|
| RF-34 | Notificar automáticamente al cliente la creación, pago, despacho, entrega y cancelación de sus pedidos, y la activación de su plan. |
| RF-35 | Programar un recordatorio antes de la renovación del plan y cuando un pedido quede pendiente de pago. |
| RF-36 | Crear recordatorios personales con fecha y hora; listar recibidas, sin leer y programadas; marcar como leídas y eliminar. |

## 5. Consultas

| Código | Consulta |
|---|---|
| RF-37 | Productos que compró un cliente |
| RF-38 | Pedidos de un cliente (filtro por estado) |
| RF-39 | Detalle de un pedido con lotes, fincas y pagos |
| RF-40 | Trazabilidad de un lote: finca, campesino y clientes a los que se vendió |
| RF-41 | Datos de un campesino y sus fincas |
| RF-42 | Existencias vigentes de un producto por punto |
| RF-43 | Plan y pagos de un cliente |
| RF-44 | Personal de un punto de acopio |

## 6. Reportes (visibles en la aplicación y exportables a PDF)

| Código | Nivel | Reporte |
|---|---|---|
| RF-45 | Simple | Catálogo de productos y precios |
| RF-46 | Simple | Nómina de empleados con su salario |
| RF-47 | Simple | Lotes próximos a vencer |
| RF-48 | Intermedio | Ventas realizadas mes a mes (gráfico) |
| RF-49 | Intermedio | Ventas por categoría de producto (gráfico) |
| RF-50 | Intermedio | Clientes frecuentes |
| RF-51 | Intermedio | Recaudo y comisiones por medio de pago (gráfico) |
| RF-52 | Complejo | Productos estrella: ventas sobre el promedio (gráfico) |
| RF-53 | Complejo | Suscriptores en riesgo de abandono |
| RF-54 | Complejo | Desempeño de los puntos de acopio (gráfico) |

## 7. Documentos con plantilla (PDF)

| Código | Documento |
|---|---|
| RF-55 | Factura electrónica de venta |
| RF-56 | Comprobante de pago |
| RF-57 | Guía de despacho / remisión con espacio de firmas |
| RF-58 | Certificado de suscripción |

## 8. Tablero

| Código | Requerimiento |
|---|---|
| RF-59 | Tablero de inicio con ventas del mes, pedidos, inventario, lotes por vencer y planes activos, y gráfico de ventas de los últimos 30 días. |
| RF-60 | Portal del campesino con sus fincas, lotes entregados, kilos vendidos y valor liquidado. |
