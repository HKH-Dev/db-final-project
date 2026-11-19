# Requerimientos funcionales · FrutiMarket BD

Documento para aprobación del docente. Proyecto final de Bases de Datos, Universidad del Quindío 2026-2.

**Caso de estudio:** plataforma que conecta a clientes (hogares, restaurantes, hoteles, tiendas) con campesinos del
Quindío a través de puntos de acopio, con planes de suscripción y pago en línea. Adapta el caso de turismo y hotelería:
consumidores, prestadores de servicios e intermediarios; registro y perfil de usuario; recordatorios y notificaciones;
planes y pago en línea.

**Modelo:** 31 tablas en MariaDB: 30 entidades fuertes (20 de negocio y 10 catálogos) y 1 entidad débil (`detalle_pedido`).

## 1. Seguridad y cuenta

| Código | Requerimiento |
|---|---|
| RF-01 | Iniciar sesión con usuario (o correo) y contraseña validados contra la base de datos; contraseñas cifradas con BCrypt. |
| RF-02 | Cerrar sesión. |
| RF-03 | Registrar una cuenta de cliente (datos personales o de empresa, dirección, municipio, usuario y contraseña) con validación de formato y de datos únicos. |
| RF-04 | Consultar y modificar el perfil propio; cambiar la contraseña verificando la actual. |
| RF-05 | Restringir las opciones según el rol: administrador, operador de acopio, campesino o cliente. |

## 2. Gestión de las tablas principales del negocio (core)

Cada una permite **registrar, modificar, eliminar y consultar** (listado con búsqueda, orden y paginación, y ficha de detalle).
La eliminación de un registro con dependencias se impide y se informa con un mensaje claro.

| Código | Tabla | Registro | Modificación | Eliminación | Consulta |
|---|---|---|---|---|---|
| RF-06 | **Pedido** (transacción principal) | Nuevo pedido: punto, cliente, productos y kilos; asigna los lotes que vencen primero y descuenta el inventario (trigger) | Fecha y dirección de entrega, observaciones; cambios de estado (pagar, despachar, entregar, cancelar) | Elimina el pedido y sus líneas; el trigger devuelve los kilos a los lotes | Lista, ficha con líneas, pagos, factura y despacho |
| RF-07 | **Cliente** | Datos, tipo de cliente, documento, municipio, usuario opcional | Todos los datos | Se impide si tiene pedidos o suscripciones | Lista y ficha con número de pedidos |
| RF-08 | **Producto** | Categoría, nombre, variedad, precio por kilo, IVA, vida útil | Todos los datos | Se impide si tiene lotes | Lista y ficha |
| RF-09 | **Lote** | Finca, producto, punto, fechas, kilos, precio en finca, calidad | Todos los datos | Se impide si ya se vendió | Lista y ficha con existencias |

## 3. Gestión de las demás tablas

Las mismas cuatro operaciones para:

| Código | Tablas |
|---|---|
| RF-10 | Personas y cuentas: usuarios, roles, campesinos, empleados, transportadores |
| RF-11 | Producción y ubicación: fincas, categorías, puntos de acopio, municipios |
| RF-12 | Comercial: planes, suscripciones, medios de pago, pagos (consulta y modificación), facturas (consulta y eliminación), despachos, notificaciones |
| RF-13 | Catálogos (solo administrador): departamentos, cargos, tipos de cliente, periodicidades, tipos de medio de pago, tipos de notificación, estados de suscripción, de pedido, de pago y de despacho |

## 4. Proceso de venta

| Código | Requerimiento |
|---|---|
| RF-14 | Aplicar automáticamente el descuento del plan de suscripción vigente del cliente. |
| RF-15 | Pagar un pedido en línea eligiendo el medio de pago (pasarela simulada, con opción de simular rechazo). |
| RF-16 | Generar la factura de venta al aprobarse el pago. |
| RF-17 | Despachar un pedido pagado asignando transportador (validando capacidad) y generar la guía. |
| RF-18 | Marcar un pedido como entregado. |
| RF-19 | Cancelar un pedido pendiente o pagado, devolviendo el inventario y registrando el reembolso. |
| RF-20 | Suscribirse a un plan con pago en línea (la duración sale de la periodicidad del plan) y cancelar la suscripción. |

## 5. Recordatorios y notificaciones

| Código | Requerimiento |
|---|---|
| RF-21 | Notificar automáticamente al cliente la creación, pago, despacho, entrega y cancelación de sus pedidos, y la activación de su plan. |
| RF-22 | Programar un recordatorio antes de la renovación del plan y cuando un pedido quede pendiente de pago. |
| RF-23 | Crear recordatorios personales con fecha y hora; listar recibidas, sin leer y programadas; marcar como leídas y eliminar. |

## 6. Consultas (datos puntuales)

| Código | Consulta |
|---|---|
| RF-24 | Productos que compró un cliente |
| RF-25 | Pedidos de un cliente (filtro por estado) |
| RF-26 | Detalle de un pedido con lotes, fincas y pagos |
| RF-27 | Trazabilidad de un lote: finca, campesino y clientes a los que se vendió |
| RF-28 | Datos de un campesino y sus fincas |
| RF-29 | Existencias vigentes de un producto por punto |
| RF-30 | Plan y pagos de un cliente |
| RF-31 | Personal de un punto de acopio |

## 7. Reportes (visibles en la aplicación y exportables a PDF)

| Código | Nivel | Reporte |
|---|---|---|
| RF-32 | Simple | Catálogo de productos y precios |
| RF-33 | Simple | Nómina de empleados con su salario |
| RF-34 | Simple | Lotes próximos a vencer |
| RF-35 | Intermedio | Ventas realizadas mes a mes (gráfico) |
| RF-36 | Intermedio | Ventas por categoría de producto (gráfico) |
| RF-37 | Intermedio | Clientes frecuentes |
| RF-38 | Intermedio | Recaudo y comisiones por medio de pago (gráfico) |
| RF-39 | Complejo | Productos estrella: ventas sobre el promedio (gráfico) |
| RF-40 | Complejo | Suscriptores en riesgo de abandono |
| RF-41 | Complejo | Desempeño de los puntos de acopio (gráfico) |

## 8. Documentos con plantilla (PDF)

| Código | Documento |
|---|---|
| RF-42 | Factura electrónica de venta |
| RF-43 | Comprobante de pago |
| RF-44 | Guía de despacho / remisión con espacio de firmas |
| RF-45 | Certificado de suscripción |

## 9. Tableros

| Código | Requerimiento |
|---|---|
| RF-46 | Tablero de inicio con ventas del mes, pedidos, inventario, lotes por vencer y planes activos, y gráfico de ventas de los últimos 30 días. |
| RF-47 | Portal del campesino con sus fincas, lotes entregados, kilos vendidos y valor liquidado. |
| RF-48 | Portal del cliente con sus pedidos, ahorro con el plan y avisos sin leer. |
