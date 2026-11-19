-- =====================================================================================
--  FrutiMarket BD · Datos de los catalogos para Oracle (ejecutar despues de modelo-datos-oracle.sql)
--  GENERADO desde database/03_datos.sql por database/generador/generar_oracle.py
-- =====================================================================================

INSERT INTO DEPARTAMENTO (nombre, codigo_dane) VALUES ('Quindío', '63');
INSERT INTO DEPARTAMENTO (nombre, codigo_dane) VALUES ('Risaralda', '66');
INSERT INTO DEPARTAMENTO (nombre, codigo_dane) VALUES ('Caldas', '17');
INSERT INTO DEPARTAMENTO (nombre, codigo_dane) VALUES ('Valle del Cauca', '76');
INSERT INTO DEPARTAMENTO (nombre, codigo_dane) VALUES ('Tolima', '73');

INSERT INTO CARGO (nombre, area, descripcion) VALUES ('Gerente general', 'DIRECCION', 'Dirige la operación de FrutiMarket y responde por los resultados del negocio.');
INSERT INTO CARGO (nombre, area, descripcion) VALUES ('Contador', 'ADMINISTRACION', 'Lleva la contabilidad, la facturación electrónica y las obligaciones tributarias.');
INSERT INTO CARGO (nombre, area, descripcion) VALUES ('Auxiliar administrativo', 'ADMINISTRACION', 'Apoya la gestión documental, la nómina y la atención a proveedores.');
INSERT INTO CARGO (nombre, area, descripcion) VALUES ('Ejecutivo comercial', 'COMERCIAL', 'Atiende hoteles, restaurantes y tiendas, y vende los planes de suscripción.');
INSERT INTO CARGO (nombre, area, descripcion) VALUES ('Coordinador de logística', 'OPERACION', 'Programa rutas, transportadores y despachos de todos los puntos.');
INSERT INTO CARGO (nombre, area, descripcion) VALUES ('Coordinador de acopio', 'OPERACION', 'Administra un punto de acopio: recibe lotes, gestiona pedidos y despachos.');
INSERT INTO CARGO (nombre, area, descripcion) VALUES ('Analista de calidad', 'OPERACION', 'Clasifica los lotes recibidos (extra, primera, segunda) y controla la cadena de frío.');
INSERT INTO CARGO (nombre, area, descripcion) VALUES ('Auxiliar de bodega', 'OPERACION', 'Recibe, pesa, almacena y alista los productos del punto de acopio.');
INSERT INTO CARGO (nombre, area, descripcion) VALUES ('Operario de cargue', 'OPERACION', 'Carga y descarga los vehículos de los transportadores.');

INSERT INTO TIPO_CLIENTE (codigo, nombre, descripcion) VALUES ('HOGAR', 'Hogar', 'Familia que compra para su consumo en casa.');
INSERT INTO TIPO_CLIENTE (codigo, nombre, descripcion) VALUES ('RESTAURANTE', 'Restaurante', 'Restaurante, cafetería o servicio de alimentos.');
INSERT INTO TIPO_CLIENTE (codigo, nombre, descripcion) VALUES ('HOTEL', 'Hotel', 'Hotel, finca turística o glamping de la zona cafetera.');
INSERT INTO TIPO_CLIENTE (codigo, nombre, descripcion) VALUES ('TIENDA', 'Tienda', 'Tienda de barrio o frutería que revende al detal.');
INSERT INTO TIPO_CLIENTE (codigo, nombre, descripcion) VALUES ('SUPERMERCADO', 'Supermercado', 'Cadena o supermercado con compras de volumen.');

INSERT INTO PERIODICIDAD (codigo, nombre, dias) VALUES ('SEMANAL', 'Semanal', 7);
INSERT INTO PERIODICIDAD (codigo, nombre, dias) VALUES ('QUINCENAL', 'Quincenal', 15);
INSERT INTO PERIODICIDAD (codigo, nombre, dias) VALUES ('MENSUAL', 'Mensual', 30);

INSERT INTO TIPO_METODO_PAGO (codigo, nombre, descripcion) VALUES ('TARJETA', 'Tarjeta', 'Tarjeta de crédito o débito procesada por la pasarela de pagos.');
INSERT INTO TIPO_METODO_PAGO (codigo, nombre, descripcion) VALUES ('PSE', 'PSE', 'Débito a cuenta de ahorros o corriente por el botón PSE.');
INSERT INTO TIPO_METODO_PAGO (codigo, nombre, descripcion) VALUES ('BILLETERA', 'Billetera digital', 'Pago desde el celular con Nequi o Daviplata.');
INSERT INTO TIPO_METODO_PAGO (codigo, nombre, descripcion) VALUES ('TRANSFERENCIA', 'Transferencia', 'Transferencia bancaria a la cuenta de FrutiMarket.');
INSERT INTO TIPO_METODO_PAGO (codigo, nombre, descripcion) VALUES ('EFECTIVO', 'Efectivo', 'Pago en efectivo al recibir el pedido.');

INSERT INTO TIPO_NOTIFICACION (codigo, nombre, descripcion) VALUES ('RECORDATORIO', 'Recordatorio', 'Recordatorio programado por el usuario o por el sistema (renovación, pago pendiente).');
INSERT INTO TIPO_NOTIFICACION (codigo, nombre, descripcion) VALUES ('PEDIDO', 'Pedido', 'Cambios de estado de un pedido: creado, despachado, entregado o cancelado.');
INSERT INTO TIPO_NOTIFICACION (codigo, nombre, descripcion) VALUES ('PAGO', 'Pago', 'Resultado de un pago en línea o de un reembolso.');
INSERT INTO TIPO_NOTIFICACION (codigo, nombre, descripcion) VALUES ('SUSCRIPCION', 'Suscripción', 'Activación, renovación o cancelación de un plan.');
INSERT INTO TIPO_NOTIFICACION (codigo, nombre, descripcion) VALUES ('INVENTARIO', 'Inventario', 'Alertas de lotes por vencer o con pocas existencias.');
INSERT INTO TIPO_NOTIFICACION (codigo, nombre, descripcion) VALUES ('SISTEMA', 'Sistema', 'Avisos generales de la plataforma.');

INSERT INTO ESTADO_SUSCRIPCION (codigo, nombre, descripcion) VALUES ('ACTIVA', 'Activa', 'El plan está vigente y el cliente recibe su descuento.');
INSERT INTO ESTADO_SUSCRIPCION (codigo, nombre, descripcion) VALUES ('PAUSADA', 'Pausada', 'El cliente suspendió temporalmente las entregas.');
INSERT INTO ESTADO_SUSCRIPCION (codigo, nombre, descripcion) VALUES ('CANCELADA', 'Cancelada', 'El cliente terminó el plan antes de su fecha de fin.');
INSERT INTO ESTADO_SUSCRIPCION (codigo, nombre, descripcion) VALUES ('VENCIDA', 'Vencida', 'El periodo terminó y no se renovó.');

INSERT INTO ESTADO_PEDIDO (codigo, nombre, descripcion, orden) VALUES ('PENDIENTE', 'Pendiente de pago', 'Pedido creado; el inventario está reservado y espera el pago.', 1);
INSERT INTO ESTADO_PEDIDO (codigo, nombre, descripcion, orden) VALUES ('PAGADO', 'Pagado', 'Pago aprobado y factura emitida; listo para despachar.', 2);
INSERT INTO ESTADO_PEDIDO (codigo, nombre, descripcion, orden) VALUES ('DESPACHADO', 'Despachado', 'Salió del punto de acopio con un transportador.', 3);
INSERT INTO ESTADO_PEDIDO (codigo, nombre, descripcion, orden) VALUES ('ENTREGADO', 'Entregado', 'El cliente recibió el pedido.', 4);
INSERT INTO ESTADO_PEDIDO (codigo, nombre, descripcion, orden) VALUES ('CANCELADO', 'Cancelado', 'Pedido anulado; el inventario volvió a los lotes.', 5);

INSERT INTO ESTADO_PAGO (codigo, nombre, descripcion) VALUES ('PENDIENTE', 'Pendiente', 'Pago iniciado que aún no confirma la entidad financiera.');
INSERT INTO ESTADO_PAGO (codigo, nombre, descripcion) VALUES ('APROBADO', 'Aprobado', 'La entidad financiera aprobó la transacción.');
INSERT INTO ESTADO_PAGO (codigo, nombre, descripcion) VALUES ('RECHAZADO', 'Rechazado', 'La entidad financiera rechazó la transacción.');
INSERT INTO ESTADO_PAGO (codigo, nombre, descripcion) VALUES ('REEMBOLSADO', 'Reembolsado', 'El valor se devolvió al cliente por cancelación.');

INSERT INTO ESTADO_DESPACHO (codigo, nombre, descripcion) VALUES ('EN_RUTA', 'En ruta', 'El pedido va en camino con el transportador.');
INSERT INTO ESTADO_DESPACHO (codigo, nombre, descripcion) VALUES ('ENTREGADO', 'Entregado', 'El transportador entregó el pedido al cliente.');
INSERT INTO ESTADO_DESPACHO (codigo, nombre, descripcion) VALUES ('DEVUELTO', 'Devuelto', 'El pedido no pudo entregarse y regresó al punto.');

INSERT INTO ROL (id_rol, nombre, descripcion) VALUES (1, 'ADMIN', 'Administrador general: usuarios, catálogo, reportes y configuración');
INSERT INTO ROL (id_rol, nombre, descripcion) VALUES (2, 'OPERADOR', 'Empleado de punto de acopio: recibe lotes, gestiona pedidos y despachos');
INSERT INTO ROL (id_rol, nombre, descripcion) VALUES (3, 'CAMPESINO', 'Productor vinculado: consulta sus fincas, lotes y liquidaciones');
INSERT INTO ROL (id_rol, nombre, descripcion) VALUES (4, 'CLIENTE', 'Comprador registrado: hace pedidos, se suscribe a planes y paga en línea');

INSERT INTO CATEGORIA (id_categoria, nombre, descripcion) VALUES (1, 'Musáceas', 'Plátano y banano cultivados en asocio con café en la zona cafetera');
INSERT INTO CATEGORIA (id_categoria, nombre, descripcion) VALUES (2, 'Frutas tropicales', 'Frutas de clima medio y cálido: piña, papaya, maracuyá, mango y similares');
INSERT INTO CATEGORIA (id_categoria, nombre, descripcion) VALUES (3, 'Frutas de clima frío', 'Frutas de ladera alta: mora, lulo, tomate de árbol, fresa y uchuva');
INSERT INTO CATEGORIA (id_categoria, nombre, descripcion) VALUES (4, 'Cítricos', 'Naranja, mandarina y limón de los valles bajos del Quindío');
INSERT INTO CATEGORIA (id_categoria, nombre, descripcion) VALUES (5, 'Aguacates', 'Aguacate Hass de exportación y variedades criollas');
INSERT INTO CATEGORIA (id_categoria, nombre, descripcion) VALUES (6, 'Café', 'Café de origen Quindío en pergamino y tostado');
INSERT INTO CATEGORIA (id_categoria, nombre, descripcion) VALUES (7, 'Procesados', 'Productos transformados por asociaciones campesinas: pulpas y panela');

COMMIT;
