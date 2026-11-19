-- =====================================================================
-- FrutiMarket BD - Sistema de informacion para la comercializacion de
-- frutas y productos campesinos del Quindio.
-- Proyecto final Bases de Datos - Universidad del Quindio 2026-2
--
-- Motor: MariaDB 10.6+ (compatible con MySQL 8)
-- Ejecutar en orden: 01_esquema.sql, 02_objetos.sql, 03_datos.sql
--
-- Entidades fuertes (30):
--   Catalogos (10): departamento, cargo, tipo_cliente, periodicidad,
--     tipo_metodo_pago, tipo_notificacion, estado_suscripcion,
--     estado_pedido, estado_pago, estado_despacho
--   Negocio (20): municipio, rol, usuario, campesino, finca, categoria,
--     producto, punto_acopio, empleado, transportador, cliente, plan,
--     metodo_pago, suscripcion, lote, pedido, pago, factura, despacho,
--     notificacion
-- Entidad debil (1): detalle_pedido (depende de pedido)
--
-- Los catalogos usan llave natural (codigo o nombre): las tablas que los
-- referencian guardan ese codigo, de modo que un reporte sobre una sola
-- tabla sigue siendo legible sin JOIN y la llave foranea garantiza que
-- el valor exista.
-- =====================================================================

DROP DATABASE IF EXISTS frutimarket_bd;
CREATE DATABASE frutimarket_bd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE frutimarket_bd;

-- ---------------------------------------------------------------------
-- Catalogos (listas de valores normalizadas)
-- ---------------------------------------------------------------------
CREATE TABLE departamento (
    nombre       VARCHAR(60)  NOT NULL COMMENT 'Nombre oficial del departamento; es la llave natural.',
    codigo_dane  CHAR(2)      NOT NULL COMMENT 'Código DANE de dos dígitos del departamento.',
    CONSTRAINT pk_departamento PRIMARY KEY (nombre),
    CONSTRAINT uk_departamento_dane UNIQUE (codigo_dane),
    CONSTRAINT ck_departamento_dane CHECK (codigo_dane REGEXP '^[0-9]{2}$')
) ENGINE = InnoDB COMMENT = 'Departamentos de Colombia con su código DANE; agrupan a los municipios.';

CREATE TABLE cargo (
    nombre       VARCHAR(40)  NOT NULL COMMENT 'Nombre del cargo; es la llave natural.',
    area         VARCHAR(20)  NOT NULL COMMENT 'Área de la empresa a la que pertenece el cargo.',
    descripcion  VARCHAR(200) NOT NULL COMMENT 'Funciones principales del cargo.',
    CONSTRAINT pk_cargo PRIMARY KEY (nombre),
    CONSTRAINT ck_cargo_area CHECK (area IN ('OPERACION', 'ADMINISTRACION', 'COMERCIAL', 'DIRECCION'))
) ENGINE = InnoDB COMMENT = 'Cargos que puede ocupar un empleado de FrutiMarket, con el área a la que pertenecen.';

CREATE TABLE tipo_cliente (
    codigo       VARCHAR(20)  NOT NULL COMMENT 'Código del tipo de cliente (llave natural).',
    nombre       VARCHAR(40)  NOT NULL COMMENT 'Nombre que se muestra en pantallas y reportes.',
    descripcion  VARCHAR(200) NOT NULL COMMENT 'Explicación de qué clientes entran en el tipo.',
    CONSTRAINT pk_tipo_cliente PRIMARY KEY (codigo),
    CONSTRAINT uk_tipo_cliente_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Clasificación de los clientes (hogar, restaurante, hotel, tienda, supermercado).';

CREATE TABLE periodicidad (
    codigo  VARCHAR(10)  NOT NULL COMMENT 'Código de la periodicidad (llave natural).',
    nombre  VARCHAR(30)  NOT NULL COMMENT 'Nombre que se muestra.',
    dias    INT          NOT NULL COMMENT 'Duración de un periodo del plan en días; con ella se calcula la fecha de fin de la suscripción.',
    CONSTRAINT pk_periodicidad PRIMARY KEY (codigo),
    CONSTRAINT uk_periodicidad_nombre UNIQUE (nombre),
    CONSTRAINT ck_periodicidad_dias CHECK (dias BETWEEN 1 AND 365)
) ENGINE = InnoDB COMMENT = 'Frecuencia de entrega de los planes de suscripción y duración de cada periodo en días.';

CREATE TABLE tipo_metodo_pago (
    codigo       VARCHAR(20)  NOT NULL COMMENT 'Código del tipo de medio de pago (llave natural).',
    nombre       VARCHAR(40)  NOT NULL COMMENT 'Nombre que se muestra.',
    descripcion  VARCHAR(200) NOT NULL COMMENT 'Cómo funciona ese tipo de pago.',
    CONSTRAINT pk_tipo_metodo_pago PRIMARY KEY (codigo),
    CONSTRAINT uk_tipo_metodo_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Familias de medios de pago (tarjeta, PSE, billetera digital, transferencia, efectivo).';

CREATE TABLE tipo_notificacion (
    codigo       VARCHAR(15)  NOT NULL COMMENT 'Código del tipo de notificación (llave natural).',
    nombre       VARCHAR(40)  NOT NULL COMMENT 'Nombre que se muestra.',
    descripcion  VARCHAR(200) NOT NULL COMMENT 'Cuándo se genera ese tipo de aviso.',
    CONSTRAINT pk_tipo_notificacion PRIMARY KEY (codigo),
    CONSTRAINT uk_tipo_notificacion_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Clases de notificación y recordatorio que recibe un usuario.';

CREATE TABLE estado_suscripcion (
    codigo       VARCHAR(12)  NOT NULL COMMENT 'Código del estado (llave natural).',
    nombre       VARCHAR(30)  NOT NULL COMMENT 'Nombre que se muestra.',
    descripcion  VARCHAR(200) NOT NULL COMMENT 'Significado del estado.',
    CONSTRAINT pk_estado_suscripcion PRIMARY KEY (codigo),
    CONSTRAINT uk_estado_suscripcion_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Estados posibles de una suscripción a un plan.';

CREATE TABLE estado_pedido (
    codigo       VARCHAR(12)  NOT NULL COMMENT 'Código del estado (llave natural).',
    nombre       VARCHAR(30)  NOT NULL COMMENT 'Nombre que se muestra.',
    descripcion  VARCHAR(200) NOT NULL COMMENT 'Significado del estado.',
    orden        INT          NOT NULL COMMENT 'Posición del estado en el flujo del pedido.',
    CONSTRAINT pk_estado_pedido PRIMARY KEY (codigo),
    CONSTRAINT uk_estado_pedido_nombre UNIQUE (nombre),
    CONSTRAINT uk_estado_pedido_orden UNIQUE (orden)
) ENGINE = InnoDB COMMENT = 'Estados del flujo de un pedido, con su orden.';

CREATE TABLE estado_pago (
    codigo       VARCHAR(12)  NOT NULL COMMENT 'Código del estado (llave natural).',
    nombre       VARCHAR(30)  NOT NULL COMMENT 'Nombre que se muestra.',
    descripcion  VARCHAR(200) NOT NULL COMMENT 'Significado del estado.',
    CONSTRAINT pk_estado_pago PRIMARY KEY (codigo),
    CONSTRAINT uk_estado_pago_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Resultados posibles de un pago en la pasarela.';

CREATE TABLE estado_despacho (
    codigo       VARCHAR(12)  NOT NULL COMMENT 'Código del estado (llave natural).',
    nombre       VARCHAR(30)  NOT NULL COMMENT 'Nombre que se muestra.',
    descripcion  VARCHAR(200) NOT NULL COMMENT 'Significado del estado.',
    CONSTRAINT pk_estado_despacho PRIMARY KEY (codigo),
    CONSTRAINT uk_estado_despacho_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Estados de la entrega de un pedido por el transportador.';

-- ---------------------------------------------------------------------
-- Ubicacion
-- ---------------------------------------------------------------------
CREATE TABLE municipio (
    id_municipio   INT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador del municipio.',
    nombre         VARCHAR(80)  NOT NULL COMMENT 'Nombre del municipio.',
    departamento   VARCHAR(60)  NOT NULL COMMENT 'Departamento al que pertenece.',
    codigo_dane    CHAR(5)      NOT NULL COMMENT 'Código DANE de cinco dígitos del municipio.',
    CONSTRAINT pk_municipio PRIMARY KEY (id_municipio),
    CONSTRAINT uk_municipio_dane UNIQUE (codigo_dane),
    CONSTRAINT fk_municipio_departamento FOREIGN KEY (departamento) REFERENCES departamento (nombre)
) ENGINE = InnoDB COMMENT = 'Municipios donde hay clientes, fincas o puntos de acopio, con su código DANE.';

-- ---------------------------------------------------------------------
-- Seguridad: usuarios del sistema (registro, perfil y login)
-- ---------------------------------------------------------------------
CREATE TABLE rol (
    id_rol       INT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador del rol.',
    nombre       VARCHAR(30)  NOT NULL COMMENT 'Nombre del rol (ADMIN, OPERADOR, CAMPESINO, CLIENTE).',
    descripcion  VARCHAR(200) NOT NULL COMMENT 'Qué puede hacer un usuario con este rol.',
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uk_rol_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Roles de seguridad que definen qué puede hacer cada usuario en el sistema.';

CREATE TABLE usuario (
    id_usuario       INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del usuario.',
    id_rol           INT           NOT NULL COMMENT 'Rol que determina los permisos del usuario.',
    username         VARCHAR(40)   NOT NULL COMMENT 'Nombre de usuario con el que inicia sesión.',
    password_hash    VARCHAR(100)  NOT NULL COMMENT 'Contraseña cifrada con BCrypt (nunca se guarda en texto plano).',
    nombre_completo  VARCHAR(120)  NOT NULL COMMENT 'Nombre que se muestra en el perfil y en los documentos.',
    email            VARCHAR(120)  NOT NULL COMMENT 'Correo electrónico; también sirve para iniciar sesión.',
    telefono         VARCHAR(20)   NULL COMMENT 'Celular de contacto.',
    activo           TINYINT(1)    NOT NULL DEFAULT 1 COMMENT 'Indica si la cuenta puede iniciar sesión.',
    fecha_registro   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creación de la cuenta.',
    ultimo_acceso    DATETIME      NULL COMMENT 'Fecha y hora del último inicio de sesión exitoso.',
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_username UNIQUE (username),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
) ENGINE = InnoDB COMMENT = 'Cuentas de acceso al sistema (inicio de sesión, perfil y notificaciones).';

-- ---------------------------------------------------------------------
-- Productores
-- ---------------------------------------------------------------------
CREATE TABLE campesino (
    id_campesino       INT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador del campesino.',
    id_usuario         INT          NULL COMMENT 'Cuenta con la que el campesino entra al portal (opcional).',
    id_municipio       INT          NOT NULL COMMENT 'Municipio donde reside.',
    tipo_documento     VARCHAR(5)   NOT NULL DEFAULT 'CC' COMMENT 'Tipo de documento de identidad (CC, CE o PPT).',
    numero_documento   VARCHAR(20)  NOT NULL COMMENT 'Número del documento de identidad.',
    nombres            VARCHAR(60)  NOT NULL COMMENT 'Nombres del campesino.',
    apellidos          VARCHAR(60)  NOT NULL COMMENT 'Apellidos del campesino.',
    telefono           VARCHAR(20)  NOT NULL COMMENT 'Celular de contacto.',
    email              VARCHAR(120) NULL COMMENT 'Correo electrónico (opcional).',
    fecha_vinculacion  DATE         NOT NULL COMMENT 'Fecha en que empezó a proveer a FrutiMarket.',
    certificado_bpa    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Indica si tiene certificado de Buenas Prácticas Agrícolas del ICA.',
    activo             TINYINT(1)   NOT NULL DEFAULT 1 COMMENT 'Indica si sigue vinculado.',
    CONSTRAINT pk_campesino PRIMARY KEY (id_campesino),
    CONSTRAINT uk_campesino_documento UNIQUE (numero_documento),
    CONSTRAINT uk_campesino_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_campesino_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL,
    CONSTRAINT fk_campesino_municipio FOREIGN KEY (id_municipio) REFERENCES municipio (id_municipio),
    CONSTRAINT ck_campesino_tipo_doc CHECK (tipo_documento IN ('CC', 'CE', 'PPT'))
) ENGINE = InnoDB COMMENT = 'Productores del Quindío vinculados que entregan sus cosechas a FrutiMarket.';

CREATE TABLE finca (
    id_finca        INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la finca.',
    id_campesino    INT           NOT NULL COMMENT 'Campesino propietario o a cargo de la finca.',
    id_municipio    INT           NOT NULL COMMENT 'Municipio donde está la finca.',
    nombre          VARCHAR(80)   NOT NULL COMMENT 'Nombre de la finca.',
    vereda          VARCHAR(80)   NOT NULL COMMENT 'Vereda donde se ubica.',
    area_hectareas  DECIMAL(8,2)  NOT NULL COMMENT 'Área cultivable en hectáreas.',
    altitud_msnm    INT           NOT NULL COMMENT 'Altitud en metros sobre el nivel del mar (define qué se puede cultivar).',
    CONSTRAINT pk_finca PRIMARY KEY (id_finca),
    CONSTRAINT fk_finca_campesino FOREIGN KEY (id_campesino) REFERENCES campesino (id_campesino),
    CONSTRAINT fk_finca_municipio FOREIGN KEY (id_municipio) REFERENCES municipio (id_municipio),
    CONSTRAINT ck_finca_area CHECK (area_hectareas > 0),
    CONSTRAINT ck_finca_altitud CHECK (altitud_msnm BETWEEN 0 AND 4000)
) ENGINE = InnoDB COMMENT = 'Predios de los campesinos donde se cultivan los productos.';

-- ---------------------------------------------------------------------
-- Productos
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id_categoria  INT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la categoría.',
    nombre        VARCHAR(60)  NOT NULL COMMENT 'Nombre de la categoría.',
    descripcion   VARCHAR(250) NOT NULL COMMENT 'Qué productos agrupa.',
    CONSTRAINT pk_categoria PRIMARY KEY (id_categoria),
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Agrupación de los productos del catálogo (musáceas, cítricos, café…).';

CREATE TABLE producto (
    id_producto             INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del producto.',
    id_categoria            INT           NOT NULL COMMENT 'Categoría a la que pertenece.',
    nombre                  VARCHAR(80)   NOT NULL COMMENT 'Nombre del producto (p. ej. Aguacate).',
    variedad                VARCHAR(60)   NOT NULL COMMENT 'Variedad o presentación (p. ej. Hass).',
    unidad_medida           VARCHAR(10)   NOT NULL DEFAULT 'KG' COMMENT 'Unidad en la que se vende (kilogramo).',
    precio_venta_kg         DECIMAL(12,0) NOT NULL COMMENT 'Precio de venta por kilo en pesos colombianos.',
    iva_pct                 DECIMAL(5,2)  NOT NULL DEFAULT 0 COMMENT 'Porcentaje de IVA que aplica (0, 5 o 19 %).',
    vida_util_dias          INT           NOT NULL COMMENT 'Días que dura el producto desde la cosecha; con ellos se calcula el vencimiento del lote.',
    requiere_refrigeracion  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT 'Indica si debe transportarse y almacenarse refrigerado.',
    activo                  TINYINT(1)    NOT NULL DEFAULT 1 COMMENT 'Indica si se sigue ofreciendo.',
    CONSTRAINT pk_producto PRIMARY KEY (id_producto),
    CONSTRAINT uk_producto_nombre UNIQUE (nombre, variedad),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria),
    CONSTRAINT ck_producto_precio CHECK (precio_venta_kg > 0),
    CONSTRAINT ck_producto_iva CHECK (iva_pct IN (0, 5, 19)),
    CONSTRAINT ck_producto_vida CHECK (vida_util_dias > 0)
) ENGINE = InnoDB COMMENT = 'Productos que se compran a los campesinos y se venden por kilo.';

-- ---------------------------------------------------------------------
-- Logistica y personal
-- ---------------------------------------------------------------------
CREATE TABLE punto_acopio (
    id_punto      INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del punto de acopio.',
    id_municipio  INT           NOT NULL COMMENT 'Municipio donde está el punto.',
    nombre        VARCHAR(80)   NOT NULL COMMENT 'Nombre del punto.',
    direccion     VARCHAR(120)  NOT NULL COMMENT 'Dirección física.',
    telefono      VARCHAR(20)   NOT NULL COMMENT 'Teléfono del punto.',
    capacidad_kg  DECIMAL(10,0) NOT NULL COMMENT 'Capacidad de almacenamiento en kilos.',
    refrigerado   TINYINT(1)    NOT NULL DEFAULT 0 COMMENT 'Indica si tiene cuarto frío.',
    activo        TINYINT(1)    NOT NULL DEFAULT 1 COMMENT 'Indica si está operando.',
    CONSTRAINT pk_punto_acopio PRIMARY KEY (id_punto),
    CONSTRAINT uk_punto_nombre UNIQUE (nombre),
    CONSTRAINT fk_punto_municipio FOREIGN KEY (id_municipio) REFERENCES municipio (id_municipio),
    CONSTRAINT ck_punto_capacidad CHECK (capacidad_kg > 0)
) ENGINE = InnoDB COMMENT = 'Centros donde se reciben, almacenan y despachan los productos.';

CREATE TABLE empleado (
    id_empleado       INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del empleado.',
    id_punto          INT           NOT NULL COMMENT 'Punto de acopio donde trabaja.',
    id_usuario        INT           NULL COMMENT 'Cuenta con la que entra al sistema (solo los coordinadores).',
    numero_documento  VARCHAR(20)   NOT NULL COMMENT 'Cédula del empleado.',
    nombres           VARCHAR(60)   NOT NULL COMMENT 'Nombres del empleado.',
    apellidos         VARCHAR(60)   NOT NULL COMMENT 'Apellidos del empleado.',
    cargo             VARCHAR(40)   NOT NULL COMMENT 'Cargo que ocupa.',
    salario           DECIMAL(12,0) NOT NULL COMMENT 'Salario mensual en pesos (mínimo el SMMLV 2026).',
    fecha_ingreso     DATE          NOT NULL COMMENT 'Fecha de ingreso a la empresa.',
    telefono          VARCHAR(20)   NOT NULL COMMENT 'Celular de contacto.',
    email             VARCHAR(120)  NOT NULL COMMENT 'Correo corporativo o personal.',
    activo            TINYINT(1)    NOT NULL DEFAULT 1 COMMENT 'Indica si sigue trabajando en la empresa.',
    CONSTRAINT pk_empleado PRIMARY KEY (id_empleado),
    CONSTRAINT uk_empleado_documento UNIQUE (numero_documento),
    CONSTRAINT uk_empleado_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_empleado_punto FOREIGN KEY (id_punto) REFERENCES punto_acopio (id_punto),
    CONSTRAINT fk_empleado_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL,
    CONSTRAINT fk_empleado_cargo FOREIGN KEY (cargo) REFERENCES cargo (nombre),
    CONSTRAINT ck_empleado_salario CHECK (salario >= 1750905)   -- SMMLV 2026
) ENGINE = InnoDB COMMENT = 'Personal de FrutiMarket asignado a un punto de acopio.';

CREATE TABLE transportador (
    id_transportador  INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del transportador.',
    numero_documento  VARCHAR(20)   NOT NULL COMMENT 'Cédula o NIT del transportador.',
    nombre            VARCHAR(120)  NOT NULL COMMENT 'Nombre de la persona o razón social.',
    telefono          VARCHAR(20)   NOT NULL COMMENT 'Celular de contacto.',
    placa             CHAR(6)       NOT NULL COMMENT 'Placa del vehículo.',
    tipo_vehiculo     VARCHAR(30)   NOT NULL COMMENT 'Tipo de vehículo (camión NHR, turbo, furgón…).',
    capacidad_kg      DECIMAL(10,0) NOT NULL COMMENT 'Carga máxima del vehículo en kilos.',
    refrigerado       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT 'Indica si el vehículo tiene refrigeración.',
    activo            TINYINT(1)    NOT NULL DEFAULT 1 COMMENT 'Indica si se le pueden asignar despachos.',
    CONSTRAINT pk_transportador PRIMARY KEY (id_transportador),
    CONSTRAINT uk_transportador_documento UNIQUE (numero_documento),
    CONSTRAINT uk_transportador_placa UNIQUE (placa)
) ENGINE = InnoDB COMMENT = 'Transportadores (personas o empresas) con su vehículo, que llevan los pedidos.';

-- ---------------------------------------------------------------------
-- Clientes, planes y medios de pago
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    id_cliente        INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del cliente.',
    id_usuario        INT           NULL COMMENT 'Cuenta con la que el cliente compra en línea (opcional).',
    id_municipio      INT           NOT NULL COMMENT 'Municipio de la dirección del cliente.',
    tipo_cliente      VARCHAR(20)   NOT NULL COMMENT 'Tipo de cliente.',
    tipo_documento    VARCHAR(5)    NOT NULL COMMENT 'Tipo de documento (CC, CE o NIT).',
    numero_documento  VARCHAR(20)   NOT NULL COMMENT 'Número de documento o NIT con dígito de verificación.',
    nombre            VARCHAR(120)  NOT NULL COMMENT 'Nombre de la persona o razón social.',
    telefono          VARCHAR(20)   NOT NULL COMMENT 'Teléfono de contacto.',
    email             VARCHAR(120)  NOT NULL COMMENT 'Correo al que se envían facturas y avisos.',
    direccion         VARCHAR(150)  NOT NULL COMMENT 'Dirección de entrega por defecto.',
    fecha_registro    DATE          NOT NULL COMMENT 'Fecha en que se registró como cliente.',
    activo            TINYINT(1)    NOT NULL DEFAULT 1 COMMENT 'Indica si puede hacer pedidos.',
    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
    CONSTRAINT uk_cliente_documento UNIQUE (numero_documento),
    CONSTRAINT uk_cliente_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL,
    CONSTRAINT fk_cliente_municipio FOREIGN KEY (id_municipio) REFERENCES municipio (id_municipio),
    CONSTRAINT fk_cliente_tipo FOREIGN KEY (tipo_cliente) REFERENCES tipo_cliente (codigo),
    CONSTRAINT ck_cliente_tipo_doc CHECK (tipo_documento IN ('CC', 'CE', 'NIT'))
) ENGINE = InnoDB COMMENT = 'Compradores: hogares, restaurantes, hoteles, tiendas y supermercados.';

CREATE TABLE plan (
    id_plan        INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del plan.',
    nombre         VARCHAR(60)   NOT NULL COMMENT 'Nombre comercial del plan.',
    descripcion    VARCHAR(300)  NOT NULL COMMENT 'Qué incluye el plan.',
    periodicidad   VARCHAR(10)   NOT NULL COMMENT 'Frecuencia del plan.',
    precio         DECIMAL(12,0) NOT NULL COMMENT 'Precio de cada periodo en pesos.',
    kg_incluidos   DECIMAL(8,1)  NOT NULL COMMENT 'Kilos de fruta incluidos en cada periodo.',
    descuento_pct  DECIMAL(5,2)  NOT NULL DEFAULT 0 COMMENT 'Descuento que recibe el suscriptor en sus pedidos adicionales.',
    activo         TINYINT(1)    NOT NULL DEFAULT 1 COMMENT 'Indica si el plan se sigue ofreciendo.',
    CONSTRAINT pk_plan PRIMARY KEY (id_plan),
    CONSTRAINT uk_plan_nombre UNIQUE (nombre),
    CONSTRAINT fk_plan_periodicidad FOREIGN KEY (periodicidad) REFERENCES periodicidad (codigo),
    CONSTRAINT ck_plan_descuento CHECK (descuento_pct BETWEEN 0 AND 30)
) ENGINE = InnoDB COMMENT = 'Planes de suscripción (canastas periódicas) que se ofrecen a los clientes.';

CREATE TABLE metodo_pago (
    id_metodo     INT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador del medio de pago.',
    nombre        VARCHAR(40)  NOT NULL COMMENT 'Nombre del medio (p. ej. Nequi).',
    tipo          VARCHAR(20)  NOT NULL COMMENT 'Tipo de medio de pago.',
    comision_pct  DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT 'Comisión que cobra la pasarela sobre cada pago.',
    en_linea      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT 'Indica si se paga en línea (o contra entrega).',
    activo        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT 'Indica si se acepta actualmente.',
    CONSTRAINT pk_metodo_pago PRIMARY KEY (id_metodo),
    CONSTRAINT uk_metodo_nombre UNIQUE (nombre),
    CONSTRAINT fk_metodo_tipo FOREIGN KEY (tipo) REFERENCES tipo_metodo_pago (codigo),
    CONSTRAINT ck_metodo_comision CHECK (comision_pct BETWEEN 0 AND 10)
) ENGINE = InnoDB COMMENT = 'Medios de pago aceptados, con la comisión que cobra la pasarela.';

CREATE TABLE suscripcion (
    id_suscripcion          INT          NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la suscripción.',
    id_cliente              INT          NOT NULL COMMENT 'Cliente que contrató el plan.',
    id_plan                 INT          NOT NULL COMMENT 'Plan contratado.',
    fecha_inicio            DATE         NOT NULL COMMENT 'Inicio del periodo contratado.',
    fecha_fin               DATE         NOT NULL COMMENT 'Fin del periodo contratado.',
    estado                  VARCHAR(12)  NOT NULL COMMENT 'Estado de la suscripción.',
    renovacion_automatica   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT 'Indica si se renueva sola al terminar el periodo.',
    CONSTRAINT pk_suscripcion PRIMARY KEY (id_suscripcion),
    CONSTRAINT fk_suscripcion_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_suscripcion_plan FOREIGN KEY (id_plan) REFERENCES plan (id_plan),
    CONSTRAINT fk_suscripcion_estado FOREIGN KEY (estado) REFERENCES estado_suscripcion (codigo),
    CONSTRAINT ck_suscripcion_fechas CHECK (fecha_fin > fecha_inicio)
) ENGINE = InnoDB COMMENT = 'Contratación de un plan por parte de un cliente durante un periodo.';

-- ---------------------------------------------------------------------
-- Inventario: lotes de cosecha recibidos en un punto de acopio
-- ---------------------------------------------------------------------
CREATE TABLE lote (
    id_lote                 INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del lote.',
    codigo                  VARCHAR(20)   NOT NULL COMMENT 'Código de trazabilidad impreso en las canastillas.',
    id_finca                INT           NOT NULL COMMENT 'Finca de donde viene la cosecha.',
    id_producto             INT           NOT NULL COMMENT 'Producto cosechado.',
    id_punto                INT           NOT NULL COMMENT 'Punto de acopio donde se recibió y se almacena.',
    fecha_cosecha           DATE          NOT NULL COMMENT 'Fecha de cosecha.',
    fecha_vencimiento       DATE          NOT NULL COMMENT 'Fecha hasta la que se puede vender (cosecha + vida útil).',
    cantidad_kg             DECIMAL(10,1) NOT NULL COMMENT 'Kilos recibidos.',
    cantidad_disponible_kg  DECIMAL(10,1) NOT NULL COMMENT 'Kilos que quedan para vender; los triggers lo actualizan al vender o cancelar.',
    precio_finca_kg         DECIMAL(12,0) NOT NULL COMMENT 'Precio por kilo que se le paga al campesino.',
    calidad                 VARCHAR(10)   NOT NULL COMMENT 'Clasificación de calidad (EXTRA, PRIMERA o SEGUNDA).',
    estado                  VARCHAR(12)   NOT NULL DEFAULT 'DISPONIBLE' COMMENT 'Estado del lote (DISPONIBLE, AGOTADO o VENCIDO).',
    CONSTRAINT pk_lote PRIMARY KEY (id_lote),
    CONSTRAINT uk_lote_codigo UNIQUE (codigo),
    CONSTRAINT fk_lote_finca FOREIGN KEY (id_finca) REFERENCES finca (id_finca),
    CONSTRAINT fk_lote_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_lote_punto FOREIGN KEY (id_punto) REFERENCES punto_acopio (id_punto),
    CONSTRAINT ck_lote_cantidad CHECK (cantidad_kg > 0 AND cantidad_disponible_kg BETWEEN 0 AND cantidad_kg),
    CONSTRAINT ck_lote_fechas CHECK (fecha_vencimiento > fecha_cosecha),
    CONSTRAINT ck_lote_calidad CHECK (calidad IN ('EXTRA', 'PRIMERA', 'SEGUNDA')),
    CONSTRAINT ck_lote_estado CHECK (estado IN ('DISPONIBLE', 'AGOTADO', 'VENCIDO'))
) ENGINE = InnoDB COMMENT = 'Cosecha de un producto entregada por una finca en un punto de acopio; lleva el inventario disponible.';

CREATE INDEX ix_lote_producto_estado ON lote (id_producto, estado, fecha_vencimiento);

-- ---------------------------------------------------------------------
-- Ventas
-- ---------------------------------------------------------------------
CREATE TABLE pedido (
    id_pedido                 INT           NOT NULL AUTO_INCREMENT COMMENT 'Número del pedido.',
    id_cliente                INT           NOT NULL COMMENT 'Cliente que compra.',
    id_punto                  INT           NOT NULL COMMENT 'Punto de acopio que atiende el pedido.',
    id_suscripcion            INT           NULL COMMENT 'Suscripción cuyo descuento se aplicó (si la hay).',
    fecha_pedido              DATETIME      NOT NULL COMMENT 'Fecha y hora en que se creó el pedido.',
    fecha_entrega_programada  DATE          NOT NULL COMMENT 'Fecha acordada para la entrega.',
    direccion_entrega         VARCHAR(150)  NOT NULL COMMENT 'Dirección donde se entrega.',
    estado                    VARCHAR(12)   NOT NULL DEFAULT 'PENDIENTE' COMMENT 'Estado del pedido en su flujo.',
    subtotal                  DECIMAL(14,0) NOT NULL DEFAULT 0 COMMENT 'Suma de las líneas antes de descuento e IVA.',
    descuento                 DECIMAL(14,0) NOT NULL DEFAULT 0 COMMENT 'Descuento del plan de suscripción.',
    iva                       DECIMAL(14,0) NOT NULL DEFAULT 0 COMMENT 'IVA de los productos gravados.',
    total                     DECIMAL(14,0) NOT NULL DEFAULT 0 COMMENT 'Valor a pagar (subtotal − descuento + IVA).',
    observaciones             VARCHAR(300)  NULL COMMENT 'Indicaciones del cliente para la entrega.',
    CONSTRAINT pk_pedido PRIMARY KEY (id_pedido),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_pedido_punto FOREIGN KEY (id_punto) REFERENCES punto_acopio (id_punto),
    CONSTRAINT fk_pedido_suscripcion FOREIGN KEY (id_suscripcion) REFERENCES suscripcion (id_suscripcion) ON DELETE SET NULL,
    CONSTRAINT fk_pedido_estado FOREIGN KEY (estado) REFERENCES estado_pedido (codigo),
    CONSTRAINT ck_pedido_totales CHECK (subtotal >= 0 AND descuento >= 0 AND iva >= 0 AND total >= 0)
) ENGINE = InnoDB COMMENT = 'Compra de un cliente atendida desde un punto de acopio (transacción principal del negocio).';

CREATE INDEX ix_pedido_fecha ON pedido (fecha_pedido);

-- Entidad debil: su llave incluye la del pedido
CREATE TABLE detalle_pedido (
    id_pedido    INT           NOT NULL COMMENT 'Pedido al que pertenece la línea (parte de la llave).',
    linea        INT           NOT NULL COMMENT 'Número de la línea dentro del pedido (llave parcial).',
    id_lote      INT           NOT NULL COMMENT 'Lote del que sale el producto (el que vence primero).',
    cantidad_kg  DECIMAL(10,1) NOT NULL COMMENT 'Kilos vendidos en la línea.',
    precio_kg    DECIMAL(12,0) NOT NULL COMMENT 'Precio por kilo al momento de la venta.',
    iva_pct      DECIMAL(5,2)  NOT NULL DEFAULT 0 COMMENT 'Porcentaje de IVA de la línea.',
    subtotal     DECIMAL(14,0) NOT NULL COMMENT 'Cantidad × precio de la línea.',
    CONSTRAINT pk_detalle_pedido PRIMARY KEY (id_pedido, linea),
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_lote FOREIGN KEY (id_lote) REFERENCES lote (id_lote),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad_kg > 0)
) ENGINE = InnoDB COMMENT = 'Líneas de un pedido: qué lote se vende, cuántos kilos y a qué precio (entidad débil).';

CREATE TABLE pago (
    id_pago         INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del pago.',
    id_metodo       INT           NOT NULL COMMENT 'Medio con el que se pagó.',
    id_pedido       INT           NULL COMMENT 'Pedido que se paga (si es pago de pedido).',
    id_suscripcion  INT           NULL COMMENT 'Suscripción que se paga (si es pago de plan).',
    fecha_pago      DATETIME      NOT NULL COMMENT 'Fecha y hora de la transacción.',
    valor           DECIMAL(14,0) NOT NULL COMMENT 'Valor pagado en pesos.',
    referencia      VARCHAR(30)   NOT NULL COMMENT 'Referencia única que entrega la pasarela.',
    estado          VARCHAR(12)   NOT NULL COMMENT 'Resultado del pago.',
    CONSTRAINT pk_pago PRIMARY KEY (id_pago),
    CONSTRAINT uk_pago_referencia UNIQUE (referencia),
    CONSTRAINT fk_pago_metodo FOREIGN KEY (id_metodo) REFERENCES metodo_pago (id_metodo),
    CONSTRAINT fk_pago_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_pago_suscripcion FOREIGN KEY (id_suscripcion) REFERENCES suscripcion (id_suscripcion) ON DELETE CASCADE,
    CONSTRAINT ck_pago_valor CHECK (valor > 0),
    CONSTRAINT fk_pago_estado FOREIGN KEY (estado) REFERENCES estado_pago (codigo),
    -- un pago es de un pedido o de una suscripcion, nunca de ambos
    CONSTRAINT ck_pago_origen CHECK ((id_pedido IS NULL) <> (id_suscripcion IS NULL))
) ENGINE = InnoDB COMMENT = 'Pagos en línea de un pedido o de una suscripción.';

CREATE TABLE factura (
    id_factura     INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la factura.',
    id_pedido      INT           NOT NULL COMMENT 'Pedido facturado (una factura por pedido).',
    numero         VARCHAR(20)   NOT NULL COMMENT 'Número consecutivo de facturación electrónica (FM-…).',
    fecha_emision  DATETIME      NOT NULL COMMENT 'Fecha y hora de emisión.',
    subtotal       DECIMAL(14,0) NOT NULL COMMENT 'Subtotal facturado.',
    descuento      DECIMAL(14,0) NOT NULL COMMENT 'Descuento facturado.',
    iva            DECIMAL(14,0) NOT NULL COMMENT 'IVA facturado.',
    total          DECIMAL(14,0) NOT NULL COMMENT 'Total de la factura.',
    CONSTRAINT pk_factura PRIMARY KEY (id_factura),
    CONSTRAINT uk_factura_numero UNIQUE (numero),
    CONSTRAINT uk_factura_pedido UNIQUE (id_pedido),
    CONSTRAINT fk_factura_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = 'Factura electrónica de venta emitida al aprobarse el pago de un pedido.';

CREATE TABLE despacho (
    id_despacho       INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador del despacho.',
    id_pedido         INT           NOT NULL COMMENT 'Pedido despachado (un despacho por pedido).',
    id_transportador  INT           NOT NULL COMMENT 'Transportador que lleva el pedido.',
    numero_guia       VARCHAR(20)   NOT NULL COMMENT 'Número de la guía de despacho (GD-…).',
    fecha_salida      DATETIME      NOT NULL COMMENT 'Fecha y hora de salida del punto de acopio.',
    fecha_entrega     DATETIME      NULL COMMENT 'Fecha y hora de entrega al cliente.',
    estado            VARCHAR(12)   NOT NULL COMMENT 'Estado del despacho.',
    temperatura_c     DECIMAL(4,1)  NULL COMMENT 'Temperatura de la carga refrigerada al salir, en °C.',
    observaciones     VARCHAR(300)  NULL COMMENT 'Novedades de la entrega.',
    CONSTRAINT pk_despacho PRIMARY KEY (id_despacho),
    CONSTRAINT uk_despacho_guia UNIQUE (numero_guia),
    CONSTRAINT uk_despacho_pedido UNIQUE (id_pedido),
    CONSTRAINT fk_despacho_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_despacho_transportador FOREIGN KEY (id_transportador) REFERENCES transportador (id_transportador),
    CONSTRAINT fk_despacho_estado FOREIGN KEY (estado) REFERENCES estado_despacho (codigo)
) ENGINE = InnoDB COMMENT = 'Envío de un pedido con un transportador (guía de despacho).';

-- ---------------------------------------------------------------------
-- Recordatorios y notificaciones personalizadas
-- ---------------------------------------------------------------------
CREATE TABLE notificacion (
    id_notificacion   INT           NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la notificación.',
    id_usuario        INT           NOT NULL COMMENT 'Usuario que la recibe.',
    tipo              VARCHAR(15)   NOT NULL COMMENT 'Tipo de notificación.',
    titulo            VARCHAR(100)  NOT NULL COMMENT 'Título corto del aviso.',
    mensaje           VARCHAR(400)  NOT NULL COMMENT 'Texto completo del aviso o recordatorio.',
    fecha_programada  DATETIME      NOT NULL COMMENT 'Fecha y hora a partir de la cual se muestra (recordatorios futuros).',
    leida             TINYINT(1)    NOT NULL DEFAULT 0 COMMENT 'Indica si el usuario ya la leyó.',
    fecha_creacion    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que se creó.',
    CONSTRAINT pk_notificacion PRIMARY KEY (id_notificacion),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_notificacion_tipo FOREIGN KEY (tipo) REFERENCES tipo_notificacion (codigo)
) ENGINE = InnoDB COMMENT = 'Avisos y recordatorios personalizados que recibe cada usuario.';

CREATE INDEX ix_notificacion_usuario ON notificacion (id_usuario, leida, fecha_programada);
