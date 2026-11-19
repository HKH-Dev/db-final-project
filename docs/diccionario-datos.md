# Diccionario de datos

Una tabla por entidad de `frutimarket_bd` (MariaDB). Columnas: **Nulo** = admite NULL; **Único** = restricción UNIQUE («compuesto» si es junto con otra columna); **Check** = restricción de comprobación o valor por defecto; **PK** = llave primaria; **FK** = llave foránea y tabla a la que referencia. Los tipos TINYINT(1) guardan 0 o 1 (falso o verdadero).

## 1. municipio

**Finalidad:** Municipios donde hay clientes, fincas o puntos de acopio, con su código DANE.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_municipio | Identificador del municipio. | INT | No |  | autoincremental | Sí |  |
| nombre | Nombre del municipio. | VARCHAR(80) | No |  |  |  |  |
| departamento | Departamento al que pertenece. | VARCHAR(60) | No |  |  |  | departamento(nombre) |
| codigo_dane | Código DANE de cinco dígitos del municipio. | CHAR(5) | No | Sí |  |  |  |

## 2. rol

**Finalidad:** Roles de seguridad que definen qué puede hacer cada usuario en el sistema.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_rol | Identificador del rol. | INT | No |  | autoincremental | Sí |  |
| nombre | Nombre del rol (ADMIN, OPERADOR, CAMPESINO, CLIENTE). | VARCHAR(30) | No | Sí |  |  |  |
| descripcion | Qué puede hacer un usuario con este rol. | VARCHAR(200) | No |  |  |  |  |

## 3. usuario

**Finalidad:** Cuentas de acceso al sistema (inicio de sesión, perfil y notificaciones).

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_usuario | Identificador del usuario. | INT | No |  | autoincremental | Sí |  |
| id_rol | Rol que determina los permisos del usuario. | INT | No |  |  |  | rol(id_rol) |
| username | Nombre de usuario con el que inicia sesión. | VARCHAR(40) | No | Sí |  |  |  |
| password_hash | Contraseña cifrada con BCrypt (nunca se guarda en texto plano). | VARCHAR(100) | No |  |  |  |  |
| nombre_completo | Nombre que se muestra en el perfil y en los documentos. | VARCHAR(120) | No |  |  |  |  |
| email | Correo electrónico; también sirve para iniciar sesión. | VARCHAR(120) | No | Sí |  |  |  |
| telefono | Celular de contacto. | VARCHAR(20) | Sí |  |  |  |  |
| activo | Indica si la cuenta puede iniciar sesión. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |
| fecha_registro | Fecha y hora de creación de la cuenta. | DATETIME | No |  | por defecto CURRENT_TIMESTAMP |  |  |
| ultimo_acceso | Fecha y hora del último inicio de sesión exitoso. | DATETIME | Sí |  |  |  |  |

## 4. campesino

**Finalidad:** Productores del Quindío vinculados que entregan sus cosechas a FrutiMarket.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_campesino | Identificador del campesino. | INT | No |  | autoincremental | Sí |  |
| id_usuario | Cuenta con la que el campesino entra al portal (opcional). | INT | Sí | Sí |  |  | usuario(id_usuario), al borrar: pone NULL |
| id_municipio | Municipio donde reside. | INT | No |  |  |  | municipio(id_municipio) |
| tipo_documento | Tipo de documento de identidad (CC, CE o PPT). | VARCHAR(5) | No |  | `tipo_documento IN ('CC', 'CE', 'PPT')`; por defecto 'CC' |  |  |
| numero_documento | Número del documento de identidad. | VARCHAR(20) | No | Sí |  |  |  |
| nombres | Nombres del campesino. | VARCHAR(60) | No |  |  |  |  |
| apellidos | Apellidos del campesino. | VARCHAR(60) | No |  |  |  |  |
| telefono | Celular de contacto. | VARCHAR(20) | No |  |  |  |  |
| email | Correo electrónico (opcional). | VARCHAR(120) | Sí |  |  |  |  |
| fecha_vinculacion | Fecha en que empezó a proveer a FrutiMarket. | DATE | No |  |  |  |  |
| certificado_bpa | Indica si tiene certificado de Buenas Prácticas Agrícolas del ICA. | TINYINT(1) | No |  | 0 o 1; por defecto 0 |  |  |
| activo | Indica si sigue vinculado. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |

## 5. finca

**Finalidad:** Predios de los campesinos donde se cultivan los productos.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_finca | Identificador de la finca. | INT | No |  | autoincremental | Sí |  |
| id_campesino | Campesino propietario o a cargo de la finca. | INT | No |  |  |  | campesino(id_campesino) |
| id_municipio | Municipio donde está la finca. | INT | No |  |  |  | municipio(id_municipio) |
| nombre | Nombre de la finca. | VARCHAR(80) | No |  |  |  |  |
| vereda | Vereda donde se ubica. | VARCHAR(80) | No |  |  |  |  |
| area_hectareas | Área cultivable en hectáreas. | DECIMAL(8,2) | No |  | `area_hectareas > 0` |  |  |
| altitud_msnm | Altitud en metros sobre el nivel del mar (define qué se puede cultivar). | INT | No |  | `altitud_msnm BETWEEN 0 AND 4000` |  |  |

## 6. categoria

**Finalidad:** Agrupación de los productos del catálogo (musáceas, cítricos, café…).

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_categoria | Identificador de la categoría. | INT | No |  | autoincremental | Sí |  |
| nombre | Nombre de la categoría. | VARCHAR(60) | No | Sí |  |  |  |
| descripcion | Qué productos agrupa. | VARCHAR(250) | No |  |  |  |  |

## 7. producto

**Finalidad:** Productos que se compran a los campesinos y se venden por kilo.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_producto | Identificador del producto. | INT | No |  | autoincremental | Sí |  |
| id_categoria | Categoría a la que pertenece. | INT | No |  |  |  | categoria(id_categoria) |
| nombre | Nombre del producto (p. ej. Aguacate). | VARCHAR(80) | No | Compuesto (nombre, variedad) |  |  |  |
| variedad | Variedad o presentación (p. ej. Hass). | VARCHAR(60) | No | Compuesto (nombre, variedad) |  |  |  |
| unidad_medida | Unidad en la que se vende (kilogramo). | VARCHAR(10) | No |  | por defecto 'KG' |  |  |
| precio_venta_kg | Precio de venta por kilo en pesos colombianos. | DECIMAL(12,0) | No |  | `precio_venta_kg > 0` |  |  |
| iva_pct | Porcentaje de IVA que aplica (0, 5 o 19 %). | DECIMAL(5,2) | No |  | `iva_pct IN (0, 5, 19)`; por defecto 0 |  |  |
| vida_util_dias | Días que dura el producto desde la cosecha; con ellos se calcula el vencimiento del lote. | INT | No |  | `vida_util_dias > 0` |  |  |
| requiere_refrigeracion | Indica si debe transportarse y almacenarse refrigerado. | TINYINT(1) | No |  | 0 o 1; por defecto 0 |  |  |
| activo | Indica si se sigue ofreciendo. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |

## 8. punto_acopio

**Finalidad:** Centros donde se reciben, almacenan y despachan los productos.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_punto | Identificador del punto de acopio. | INT | No |  | autoincremental | Sí |  |
| id_municipio | Municipio donde está el punto. | INT | No |  |  |  | municipio(id_municipio) |
| nombre | Nombre del punto. | VARCHAR(80) | No | Sí |  |  |  |
| direccion | Dirección física. | VARCHAR(120) | No |  |  |  |  |
| telefono | Teléfono del punto. | VARCHAR(20) | No |  |  |  |  |
| capacidad_kg | Capacidad de almacenamiento en kilos. | DECIMAL(10,0) | No |  | `capacidad_kg > 0` |  |  |
| refrigerado | Indica si tiene cuarto frío. | TINYINT(1) | No |  | 0 o 1; por defecto 0 |  |  |
| activo | Indica si está operando. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |

## 9. empleado

**Finalidad:** Personal de FrutiMarket asignado a un punto de acopio.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_empleado | Identificador del empleado. | INT | No |  | autoincremental | Sí |  |
| id_punto | Punto de acopio donde trabaja. | INT | No |  |  |  | punto_acopio(id_punto) |
| id_usuario | Cuenta con la que entra al sistema (solo los coordinadores). | INT | Sí | Sí |  |  | usuario(id_usuario), al borrar: pone NULL |
| numero_documento | Cédula del empleado. | VARCHAR(20) | No | Sí |  |  |  |
| nombres | Nombres del empleado. | VARCHAR(60) | No |  |  |  |  |
| apellidos | Apellidos del empleado. | VARCHAR(60) | No |  |  |  |  |
| cargo | Cargo que ocupa. | VARCHAR(40) | No |  |  |  | cargo(nombre) |
| salario | Salario mensual en pesos (mínimo el SMMLV 2026). | DECIMAL(12,0) | No |  | `salario >= 1750905` |  |  |
| fecha_ingreso | Fecha de ingreso a la empresa. | DATE | No |  |  |  |  |
| telefono | Celular de contacto. | VARCHAR(20) | No |  |  |  |  |
| email | Correo corporativo o personal. | VARCHAR(120) | No |  |  |  |  |
| activo | Indica si sigue trabajando en la empresa. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |

## 10. transportador

**Finalidad:** Transportadores (personas o empresas) con su vehículo, que llevan los pedidos.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_transportador | Identificador del transportador. | INT | No |  | autoincremental | Sí |  |
| numero_documento | Cédula o NIT del transportador. | VARCHAR(20) | No | Sí |  |  |  |
| nombre | Nombre de la persona o razón social. | VARCHAR(120) | No |  |  |  |  |
| telefono | Celular de contacto. | VARCHAR(20) | No |  |  |  |  |
| placa | Placa del vehículo. | CHAR(6) | No | Sí |  |  |  |
| tipo_vehiculo | Tipo de vehículo (camión NHR, turbo, furgón…). | VARCHAR(30) | No |  |  |  |  |
| capacidad_kg | Carga máxima del vehículo en kilos. | DECIMAL(10,0) | No |  |  |  |  |
| refrigerado | Indica si el vehículo tiene refrigeración. | TINYINT(1) | No |  | 0 o 1; por defecto 0 |  |  |
| activo | Indica si se le pueden asignar despachos. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |

## 11. cliente

**Finalidad:** Compradores: hogares, restaurantes, hoteles, tiendas y supermercados.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_cliente | Identificador del cliente. | INT | No |  | autoincremental | Sí |  |
| id_usuario | Cuenta con la que el cliente compra en línea (opcional). | INT | Sí | Sí |  |  | usuario(id_usuario), al borrar: pone NULL |
| id_municipio | Municipio de la dirección del cliente. | INT | No |  |  |  | municipio(id_municipio) |
| tipo_cliente | Tipo de cliente. | VARCHAR(20) | No |  |  |  | tipo_cliente(codigo) |
| tipo_documento | Tipo de documento (CC, CE o NIT). | VARCHAR(5) | No |  | `tipo_documento IN ('CC', 'CE', 'NIT')` |  |  |
| numero_documento | Número de documento o NIT con dígito de verificación. | VARCHAR(20) | No | Sí |  |  |  |
| nombre | Nombre de la persona o razón social. | VARCHAR(120) | No |  |  |  |  |
| telefono | Teléfono de contacto. | VARCHAR(20) | No |  |  |  |  |
| email | Correo al que se envían facturas y avisos. | VARCHAR(120) | No |  |  |  |  |
| direccion | Dirección de entrega por defecto. | VARCHAR(150) | No |  |  |  |  |
| fecha_registro | Fecha en que se registró como cliente. | DATE | No |  |  |  |  |
| activo | Indica si puede hacer pedidos. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |

## 12. plan

**Finalidad:** Planes de suscripción (canastas periódicas) que se ofrecen a los clientes.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_plan | Identificador del plan. | INT | No |  | autoincremental | Sí |  |
| nombre | Nombre comercial del plan. | VARCHAR(60) | No | Sí |  |  |  |
| descripcion | Qué incluye el plan. | VARCHAR(300) | No |  |  |  |  |
| periodicidad | Frecuencia del plan. | VARCHAR(10) | No |  |  |  | periodicidad(codigo) |
| precio | Precio de cada periodo en pesos. | DECIMAL(12,0) | No |  |  |  |  |
| kg_incluidos | Kilos de fruta incluidos en cada periodo. | DECIMAL(8,1) | No |  |  |  |  |
| descuento_pct | Descuento que recibe el suscriptor en sus pedidos adicionales. | DECIMAL(5,2) | No |  | `descuento_pct BETWEEN 0 AND 30`; por defecto 0 |  |  |
| activo | Indica si el plan se sigue ofreciendo. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |

## 13. metodo_pago

**Finalidad:** Medios de pago aceptados, con la comisión que cobra la pasarela.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_metodo | Identificador del medio de pago. | INT | No |  | autoincremental | Sí |  |
| nombre | Nombre del medio (p. ej. Nequi). | VARCHAR(40) | No | Sí |  |  |  |
| tipo | Tipo de medio de pago. | VARCHAR(20) | No |  |  |  | tipo_metodo_pago(codigo) |
| comision_pct | Comisión que cobra la pasarela sobre cada pago. | DECIMAL(5,2) | No |  | `comision_pct BETWEEN 0 AND 10`; por defecto 0 |  |  |
| en_linea | Indica si se paga en línea (o contra entrega). | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |
| activo | Indica si se acepta actualmente. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |

## 14. suscripcion

**Finalidad:** Contratación de un plan por parte de un cliente durante un periodo.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_suscripcion | Identificador de la suscripción. | INT | No |  | autoincremental | Sí |  |
| id_cliente | Cliente que contrató el plan. | INT | No |  |  |  | cliente(id_cliente) |
| id_plan | Plan contratado. | INT | No |  |  |  | plan(id_plan) |
| fecha_inicio | Inicio del periodo contratado. | DATE | No |  | `fecha_fin > fecha_inicio` |  |  |
| fecha_fin | Fin del periodo contratado. | DATE | No |  | `fecha_fin > fecha_inicio` |  |  |
| estado | Estado de la suscripción. | VARCHAR(12) | No |  |  |  | estado_suscripcion(codigo) |
| renovacion_automatica | Indica si se renueva sola al terminar el periodo. | TINYINT(1) | No |  | 0 o 1; por defecto 1 |  |  |

## 15. lote

**Finalidad:** Cosecha de un producto entregada por una finca en un punto de acopio; lleva el inventario disponible.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_lote | Identificador del lote. | INT | No |  | autoincremental | Sí |  |
| codigo | Código de trazabilidad impreso en las canastillas. | VARCHAR(20) | No | Sí |  |  |  |
| id_finca | Finca de donde viene la cosecha. | INT | No |  |  |  | finca(id_finca) |
| id_producto | Producto cosechado. | INT | No |  |  |  | producto(id_producto) |
| id_punto | Punto de acopio donde se recibió y se almacena. | INT | No |  |  |  | punto_acopio(id_punto) |
| fecha_cosecha | Fecha de cosecha. | DATE | No |  | `fecha_vencimiento > fecha_cosecha` |  |  |
| fecha_vencimiento | Fecha hasta la que se puede vender (cosecha + vida útil). | DATE | No |  | `fecha_vencimiento > fecha_cosecha` |  |  |
| cantidad_kg | Kilos recibidos. | DECIMAL(10,1) | No |  | `cantidad_kg > 0 AND cantidad_disponible_kg BETWEEN 0 AND cantidad_kg` |  |  |
| cantidad_disponible_kg | Kilos que quedan para vender; los triggers lo actualizan al vender o cancelar. | DECIMAL(10,1) | No |  | `cantidad_kg > 0 AND cantidad_disponible_kg BETWEEN 0 AND cantidad_kg` |  |  |
| precio_finca_kg | Precio por kilo que se le paga al campesino. | DECIMAL(12,0) | No |  |  |  |  |
| calidad | Clasificación de calidad (EXTRA, PRIMERA o SEGUNDA). | VARCHAR(10) | No |  | `calidad IN ('EXTRA', 'PRIMERA', 'SEGUNDA')` |  |  |
| estado | Estado del lote (DISPONIBLE, AGOTADO o VENCIDO). | VARCHAR(12) | No |  | `estado IN ('DISPONIBLE', 'AGOTADO', 'VENCIDO')`; por defecto 'DISPONIBLE' |  |  |

## 16. pedido

**Finalidad:** Compra de un cliente atendida desde un punto de acopio (transacción principal del negocio).

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_pedido | Número del pedido. | INT | No |  | autoincremental | Sí |  |
| id_cliente | Cliente que compra. | INT | No |  |  |  | cliente(id_cliente) |
| id_punto | Punto de acopio que atiende el pedido. | INT | No |  |  |  | punto_acopio(id_punto) |
| id_suscripcion | Suscripción cuyo descuento se aplicó (si la hay). | INT | Sí |  |  |  | suscripcion(id_suscripcion), al borrar: pone NULL |
| fecha_pedido | Fecha y hora en que se creó el pedido. | DATETIME | No |  |  |  |  |
| fecha_entrega_programada | Fecha acordada para la entrega. | DATE | No |  |  |  |  |
| direccion_entrega | Dirección donde se entrega. | VARCHAR(150) | No |  |  |  |  |
| estado | Estado del pedido en su flujo. | VARCHAR(12) | No |  | por defecto 'PENDIENTE' |  | estado_pedido(codigo) |
| subtotal | Suma de las líneas antes de descuento e IVA. | DECIMAL(14,0) | No |  | `subtotal >= 0 AND descuento >= 0 AND iva >= 0 AND total >= 0`; por defecto 0 |  |  |
| descuento | Descuento del plan de suscripción. | DECIMAL(14,0) | No |  | `subtotal >= 0 AND descuento >= 0 AND iva >= 0 AND total >= 0`; por defecto 0 |  |  |
| iva | IVA de los productos gravados. | DECIMAL(14,0) | No |  | `subtotal >= 0 AND descuento >= 0 AND iva >= 0 AND total >= 0`; por defecto 0 |  |  |
| total | Valor a pagar (subtotal − descuento + IVA). | DECIMAL(14,0) | No |  | `subtotal >= 0 AND descuento >= 0 AND iva >= 0 AND total >= 0`; por defecto 0 |  |  |
| observaciones | Indicaciones del cliente para la entrega. | VARCHAR(300) | Sí |  |  |  |  |

## 17. pago

**Finalidad:** Pagos en línea de un pedido o de una suscripción.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_pago | Identificador del pago. | INT | No |  | autoincremental | Sí |  |
| id_metodo | Medio con el que se pagó. | INT | No |  |  |  | metodo_pago(id_metodo) |
| id_pedido | Pedido que se paga (si es pago de pedido). | INT | Sí |  | `(id_pedido IS NULL) <> (id_suscripcion IS NULL)` |  | pedido(id_pedido), al borrar: cascada |
| id_suscripcion | Suscripción que se paga (si es pago de plan). | INT | Sí |  | `(id_pedido IS NULL) <> (id_suscripcion IS NULL)` |  | suscripcion(id_suscripcion), al borrar: cascada |
| fecha_pago | Fecha y hora de la transacción. | DATETIME | No |  |  |  |  |
| valor | Valor pagado en pesos. | DECIMAL(14,0) | No |  | `valor > 0` |  |  |
| referencia | Referencia única que entrega la pasarela. | VARCHAR(30) | No | Sí |  |  |  |
| estado | Resultado del pago. | VARCHAR(12) | No |  |  |  | estado_pago(codigo) |

## 18. factura

**Finalidad:** Factura electrónica de venta emitida al aprobarse el pago de un pedido.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_factura | Identificador de la factura. | INT | No |  | autoincremental | Sí |  |
| id_pedido | Pedido facturado (una factura por pedido). | INT | No | Sí |  |  | pedido(id_pedido), al borrar: cascada |
| numero | Número consecutivo de facturación electrónica (FM-…). | VARCHAR(20) | No | Sí |  |  |  |
| fecha_emision | Fecha y hora de emisión. | DATETIME | No |  |  |  |  |
| subtotal | Subtotal facturado. | DECIMAL(14,0) | No |  |  |  |  |
| descuento | Descuento facturado. | DECIMAL(14,0) | No |  |  |  |  |
| iva | IVA facturado. | DECIMAL(14,0) | No |  |  |  |  |
| total | Total de la factura. | DECIMAL(14,0) | No |  |  |  |  |

## 19. despacho

**Finalidad:** Envío de un pedido con un transportador (guía de despacho).

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_despacho | Identificador del despacho. | INT | No |  | autoincremental | Sí |  |
| id_pedido | Pedido despachado (un despacho por pedido). | INT | No | Sí |  |  | pedido(id_pedido), al borrar: cascada |
| id_transportador | Transportador que lleva el pedido. | INT | No |  |  |  | transportador(id_transportador) |
| numero_guia | Número de la guía de despacho (GD-…). | VARCHAR(20) | No | Sí |  |  |  |
| fecha_salida | Fecha y hora de salida del punto de acopio. | DATETIME | No |  |  |  |  |
| fecha_entrega | Fecha y hora de entrega al cliente. | DATETIME | Sí |  |  |  |  |
| estado | Estado del despacho. | VARCHAR(12) | No |  |  |  | estado_despacho(codigo) |
| temperatura_c | Temperatura de la carga refrigerada al salir, en °C. | DECIMAL(4,1) | Sí |  |  |  |  |
| observaciones | Novedades de la entrega. | VARCHAR(300) | Sí |  |  |  |  |

## 20. notificacion

**Finalidad:** Avisos y recordatorios personalizados que recibe cada usuario.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_notificacion | Identificador de la notificación. | INT | No |  | autoincremental | Sí |  |
| id_usuario | Usuario que la recibe. | INT | No |  |  |  | usuario(id_usuario), al borrar: cascada |
| tipo | Tipo de notificación. | VARCHAR(15) | No |  |  |  | tipo_notificacion(codigo) |
| titulo | Título corto del aviso. | VARCHAR(100) | No |  |  |  |  |
| mensaje | Texto completo del aviso o recordatorio. | VARCHAR(400) | No |  |  |  |  |
| fecha_programada | Fecha y hora a partir de la cual se muestra (recordatorios futuros). | DATETIME | No |  |  |  |  |
| leida | Indica si el usuario ya la leyó. | TINYINT(1) | No |  | 0 o 1; por defecto 0 |  |  |
| fecha_creacion | Fecha y hora en que se creó. | DATETIME | No |  | por defecto CURRENT_TIMESTAMP |  |  |

## 21. detalle_pedido

**Finalidad:** Líneas de un pedido: qué lote se vende, cuántos kilos y a qué precio (entidad débil).

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id_pedido | Pedido al que pertenece la línea (parte de la llave). | INT | No |  |  | Sí | pedido(id_pedido), al borrar: cascada |
| linea | Número de la línea dentro del pedido (llave parcial). | INT | No |  |  | Sí |  |
| id_lote | Lote del que sale el producto (el que vence primero). | INT | No |  |  |  | lote(id_lote) |
| cantidad_kg | Kilos vendidos en la línea. | DECIMAL(10,1) | No |  | `cantidad_kg > 0` |  |  |
| precio_kg | Precio por kilo al momento de la venta. | DECIMAL(12,0) | No |  |  |  |  |
| iva_pct | Porcentaje de IVA de la línea. | DECIMAL(5,2) | No |  | por defecto 0 |  |  |
| subtotal | Cantidad × precio de la línea. | DECIMAL(14,0) | No |  |  |  |  |

## 22. departamento

**Finalidad:** Departamentos de Colombia con su código DANE; agrupan a los municipios.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| nombre | Nombre oficial del departamento; es la llave natural. | VARCHAR(60) | No |  |  | Sí |  |
| codigo_dane | Código DANE de dos dígitos del departamento. | CHAR(2) | No | Sí | `codigo_dane REGEXP '^[0-9]{2}$'` |  |  |

## 23. cargo

**Finalidad:** Cargos que puede ocupar un empleado de FrutiMarket, con el área a la que pertenecen.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| nombre | Nombre del cargo; es la llave natural. | VARCHAR(40) | No |  |  | Sí |  |
| area | Área de la empresa a la que pertenece el cargo. | VARCHAR(20) | No |  | `area IN ('OPERACION', 'ADMINISTRACION', 'COMERCIAL', 'DIRECCION')` |  |  |
| descripcion | Funciones principales del cargo. | VARCHAR(200) | No |  |  |  |  |

## 24. tipo_cliente

**Finalidad:** Clasificación de los clientes (hogar, restaurante, hotel, tienda, supermercado).

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| codigo | Código del tipo de cliente (llave natural). | VARCHAR(20) | No |  |  | Sí |  |
| nombre | Nombre que se muestra en pantallas y reportes. | VARCHAR(40) | No | Sí |  |  |  |
| descripcion | Explicación de qué clientes entran en el tipo. | VARCHAR(200) | No |  |  |  |  |

## 25. periodicidad

**Finalidad:** Frecuencia de entrega de los planes de suscripción y duración de cada periodo en días.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| codigo | Código de la periodicidad (llave natural). | VARCHAR(10) | No |  |  | Sí |  |
| nombre | Nombre que se muestra. | VARCHAR(30) | No | Sí |  |  |  |
| dias | Duración de un periodo del plan en días; con ella se calcula la fecha de fin de la suscripción. | INT | No |  | `dias BETWEEN 1 AND 365` |  |  |

## 26. tipo_metodo_pago

**Finalidad:** Familias de medios de pago (tarjeta, PSE, billetera digital, transferencia, efectivo).

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| codigo | Código del tipo de medio de pago (llave natural). | VARCHAR(20) | No |  |  | Sí |  |
| nombre | Nombre que se muestra. | VARCHAR(40) | No | Sí |  |  |  |
| descripcion | Cómo funciona ese tipo de pago. | VARCHAR(200) | No |  |  |  |  |

## 27. tipo_notificacion

**Finalidad:** Clases de notificación y recordatorio que recibe un usuario.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| codigo | Código del tipo de notificación (llave natural). | VARCHAR(15) | No |  |  | Sí |  |
| nombre | Nombre que se muestra. | VARCHAR(40) | No | Sí |  |  |  |
| descripcion | Cuándo se genera ese tipo de aviso. | VARCHAR(200) | No |  |  |  |  |

## 28. estado_suscripcion

**Finalidad:** Estados posibles de una suscripción a un plan.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| codigo | Código del estado (llave natural). | VARCHAR(12) | No |  |  | Sí |  |
| nombre | Nombre que se muestra. | VARCHAR(30) | No | Sí |  |  |  |
| descripcion | Significado del estado. | VARCHAR(200) | No |  |  |  |  |

## 29. estado_pedido

**Finalidad:** Estados del flujo de un pedido, con su orden.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| codigo | Código del estado (llave natural). | VARCHAR(12) | No |  |  | Sí |  |
| nombre | Nombre que se muestra. | VARCHAR(30) | No | Sí |  |  |  |
| descripcion | Significado del estado. | VARCHAR(200) | No |  |  |  |  |
| orden | Posición del estado en el flujo del pedido. | INT | No | Sí |  |  |  |

## 30. estado_pago

**Finalidad:** Resultados posibles de un pago en la pasarela.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| codigo | Código del estado (llave natural). | VARCHAR(12) | No |  |  | Sí |  |
| nombre | Nombre que se muestra. | VARCHAR(30) | No | Sí |  |  |  |
| descripcion | Significado del estado. | VARCHAR(200) | No |  |  |  |  |

## 31. estado_despacho

**Finalidad:** Estados de la entrega de un pedido por el transportador.

| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| codigo | Código del estado (llave natural). | VARCHAR(12) | No |  |  | Sí |  |
| nombre | Nombre que se muestra. | VARCHAR(30) | No | Sí |  |  |  |
| descripcion | Significado del estado. | VARCHAR(200) | No |  |  |  |  |
