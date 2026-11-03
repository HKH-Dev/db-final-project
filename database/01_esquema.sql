-- =====================================================================
-- FrutiMarket BD - Sistema de informacion para la comercializacion de
-- frutas y productos campesinos del Quindio.
-- Proyecto final Bases de Datos - Universidad del Quindio 2026-2
--
-- Motor: MariaDB 10.6+ (compatible con MySQL 8)
-- Ejecutar en orden: 01_esquema.sql, 02_objetos.sql, 03_datos.sql
--
-- Entidades fuertes (20): municipio, rol, usuario, campesino, finca,
--   categoria, producto, punto_acopio, empleado, cliente, plan,
--   metodo_pago, transportador, lote, pedido, suscripcion, pago,
--   factura, despacho, notificacion
-- Entidad debil (1): detalle_pedido (depende de pedido)
-- =====================================================================

DROP DATABASE IF EXISTS frutimarket_bd;
CREATE DATABASE frutimarket_bd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE frutimarket_bd;

-- ---------------------------------------------------------------------
-- Ubicacion
-- ---------------------------------------------------------------------
CREATE TABLE municipio (
    id_municipio   INT          NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(80)  NOT NULL,
    departamento   VARCHAR(60)  NOT NULL,
    codigo_dane    CHAR(5)      NOT NULL,
    CONSTRAINT pk_municipio PRIMARY KEY (id_municipio),
    CONSTRAINT uk_municipio_dane UNIQUE (codigo_dane)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Seguridad: usuarios del sistema (registro, perfil y login)
-- ---------------------------------------------------------------------
CREATE TABLE rol (
    id_rol       INT          NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(30)  NOT NULL,
    descripcion  VARCHAR(200) NOT NULL,
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uk_rol_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE usuario (
    id_usuario       INT           NOT NULL AUTO_INCREMENT,
    id_rol           INT           NOT NULL,
    username         VARCHAR(40)   NOT NULL,
    password_hash    VARCHAR(100)  NOT NULL,          -- BCrypt
    nombre_completo  VARCHAR(120)  NOT NULL,
    email            VARCHAR(120)  NOT NULL,
    telefono         VARCHAR(20)   NULL,
    activo           TINYINT(1)    NOT NULL DEFAULT 1,
    fecha_registro   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso    DATETIME      NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_username UNIQUE (username),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Productores
-- ---------------------------------------------------------------------
CREATE TABLE campesino (
    id_campesino       INT          NOT NULL AUTO_INCREMENT,
    id_usuario         INT          NULL,
    id_municipio       INT          NOT NULL,
    tipo_documento     VARCHAR(5)   NOT NULL DEFAULT 'CC',
    numero_documento   VARCHAR(20)  NOT NULL,
    nombres            VARCHAR(60)  NOT NULL,
    apellidos          VARCHAR(60)  NOT NULL,
    telefono           VARCHAR(20)  NOT NULL,
    email              VARCHAR(120) NULL,
    fecha_vinculacion  DATE         NOT NULL,
    certificado_bpa    TINYINT(1)   NOT NULL DEFAULT 0,  -- Buenas Practicas Agricolas (ICA)
    activo             TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT pk_campesino PRIMARY KEY (id_campesino),
    CONSTRAINT uk_campesino_documento UNIQUE (numero_documento),
    CONSTRAINT uk_campesino_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_campesino_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL,
    CONSTRAINT fk_campesino_municipio FOREIGN KEY (id_municipio) REFERENCES municipio (id_municipio),
    CONSTRAINT ck_campesino_tipo_doc CHECK (tipo_documento IN ('CC', 'CE', 'PPT'))
) ENGINE = InnoDB;

CREATE TABLE finca (
    id_finca        INT           NOT NULL AUTO_INCREMENT,
    id_campesino    INT           NOT NULL,
    id_municipio    INT           NOT NULL,
    nombre          VARCHAR(80)   NOT NULL,
    vereda          VARCHAR(80)   NOT NULL,
    area_hectareas  DECIMAL(8,2)  NOT NULL,
    altitud_msnm    INT           NOT NULL,
    CONSTRAINT pk_finca PRIMARY KEY (id_finca),
    CONSTRAINT fk_finca_campesino FOREIGN KEY (id_campesino) REFERENCES campesino (id_campesino),
    CONSTRAINT fk_finca_municipio FOREIGN KEY (id_municipio) REFERENCES municipio (id_municipio),
    CONSTRAINT ck_finca_area CHECK (area_hectareas > 0),
    CONSTRAINT ck_finca_altitud CHECK (altitud_msnm BETWEEN 0 AND 4000)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Catalogo
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id_categoria  INT          NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(60)  NOT NULL,
    descripcion   VARCHAR(250) NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (id_categoria),
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE producto (
    id_producto             INT           NOT NULL AUTO_INCREMENT,
    id_categoria            INT           NOT NULL,
    nombre                  VARCHAR(80)   NOT NULL,
    variedad                VARCHAR(60)   NOT NULL,
    unidad_medida           VARCHAR(10)   NOT NULL DEFAULT 'KG',
    precio_venta_kg         DECIMAL(12,0) NOT NULL,   -- pesos colombianos
    iva_pct                 DECIMAL(5,2)  NOT NULL DEFAULT 0,
    vida_util_dias          INT           NOT NULL,
    requiere_refrigeracion  TINYINT(1)    NOT NULL DEFAULT 0,
    activo                  TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT pk_producto PRIMARY KEY (id_producto),
    CONSTRAINT uk_producto_nombre UNIQUE (nombre, variedad),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria),
    CONSTRAINT ck_producto_precio CHECK (precio_venta_kg > 0),
    CONSTRAINT ck_producto_iva CHECK (iva_pct IN (0, 5, 19)),
    CONSTRAINT ck_producto_vida CHECK (vida_util_dias > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Logistica y personal
-- ---------------------------------------------------------------------
CREATE TABLE punto_acopio (
    id_punto      INT           NOT NULL AUTO_INCREMENT,
    id_municipio  INT           NOT NULL,
    nombre        VARCHAR(80)   NOT NULL,
    direccion     VARCHAR(120)  NOT NULL,
    telefono      VARCHAR(20)   NOT NULL,
    capacidad_kg  DECIMAL(10,0) NOT NULL,
    refrigerado   TINYINT(1)    NOT NULL DEFAULT 0,
    activo        TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT pk_punto_acopio PRIMARY KEY (id_punto),
    CONSTRAINT uk_punto_nombre UNIQUE (nombre),
    CONSTRAINT fk_punto_municipio FOREIGN KEY (id_municipio) REFERENCES municipio (id_municipio),
    CONSTRAINT ck_punto_capacidad CHECK (capacidad_kg > 0)
) ENGINE = InnoDB;

CREATE TABLE empleado (
    id_empleado       INT           NOT NULL AUTO_INCREMENT,
    id_punto          INT           NOT NULL,
    id_usuario        INT           NULL,
    numero_documento  VARCHAR(20)   NOT NULL,
    nombres           VARCHAR(60)   NOT NULL,
    apellidos         VARCHAR(60)   NOT NULL,
    cargo             VARCHAR(40)   NOT NULL,
    salario           DECIMAL(12,0) NOT NULL,
    fecha_ingreso     DATE          NOT NULL,
    telefono          VARCHAR(20)   NOT NULL,
    email             VARCHAR(120)  NOT NULL,
    activo            TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT pk_empleado PRIMARY KEY (id_empleado),
    CONSTRAINT uk_empleado_documento UNIQUE (numero_documento),
    CONSTRAINT uk_empleado_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_empleado_punto FOREIGN KEY (id_punto) REFERENCES punto_acopio (id_punto),
    CONSTRAINT fk_empleado_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL,
    CONSTRAINT ck_empleado_salario CHECK (salario >= 1750905)   -- SMMLV 2026
) ENGINE = InnoDB;

CREATE TABLE transportador (
    id_transportador  INT           NOT NULL AUTO_INCREMENT,
    numero_documento  VARCHAR(20)   NOT NULL,
    nombre            VARCHAR(120)  NOT NULL,
    telefono          VARCHAR(20)   NOT NULL,
    placa             CHAR(6)       NOT NULL,
    tipo_vehiculo     VARCHAR(30)   NOT NULL,
    capacidad_kg      DECIMAL(10,0) NOT NULL,
    refrigerado       TINYINT(1)    NOT NULL DEFAULT 0,
    activo            TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT pk_transportador PRIMARY KEY (id_transportador),
    CONSTRAINT uk_transportador_documento UNIQUE (numero_documento),
    CONSTRAINT uk_transportador_placa UNIQUE (placa)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Clientes, planes y medios de pago
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    id_cliente        INT           NOT NULL AUTO_INCREMENT,
    id_usuario        INT           NULL,
    id_municipio      INT           NOT NULL,
    tipo_cliente      VARCHAR(20)   NOT NULL,
    tipo_documento    VARCHAR(5)    NOT NULL,
    numero_documento  VARCHAR(20)   NOT NULL,
    nombre            VARCHAR(120)  NOT NULL,
    telefono          VARCHAR(20)   NOT NULL,
    email             VARCHAR(120)  NOT NULL,
    direccion         VARCHAR(150)  NOT NULL,
    fecha_registro    DATE          NOT NULL,
    activo            TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
    CONSTRAINT uk_cliente_documento UNIQUE (numero_documento),
    CONSTRAINT uk_cliente_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL,
    CONSTRAINT fk_cliente_municipio FOREIGN KEY (id_municipio) REFERENCES municipio (id_municipio),
    CONSTRAINT ck_cliente_tipo CHECK (tipo_cliente IN ('HOGAR', 'RESTAURANTE', 'HOTEL', 'TIENDA', 'SUPERMERCADO')),
    CONSTRAINT ck_cliente_tipo_doc CHECK (tipo_documento IN ('CC', 'CE', 'NIT'))
) ENGINE = InnoDB;

CREATE TABLE plan (
    id_plan        INT           NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(60)   NOT NULL,
    descripcion    VARCHAR(300)  NOT NULL,
    periodicidad   VARCHAR(10)   NOT NULL,
    precio         DECIMAL(12,0) NOT NULL,
    kg_incluidos   DECIMAL(8,1)  NOT NULL,
    descuento_pct  DECIMAL(5,2)  NOT NULL DEFAULT 0,   -- descuento en pedidos adicionales
    activo         TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT pk_plan PRIMARY KEY (id_plan),
    CONSTRAINT uk_plan_nombre UNIQUE (nombre),
    CONSTRAINT ck_plan_periodicidad CHECK (periodicidad IN ('SEMANAL', 'QUINCENAL', 'MENSUAL')),
    CONSTRAINT ck_plan_descuento CHECK (descuento_pct BETWEEN 0 AND 30)
) ENGINE = InnoDB;

CREATE TABLE metodo_pago (
    id_metodo     INT          NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(40)  NOT NULL,
    tipo          VARCHAR(20)  NOT NULL,
    comision_pct  DECIMAL(5,2) NOT NULL DEFAULT 0,
    en_linea      TINYINT(1)   NOT NULL DEFAULT 1,
    activo        TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT pk_metodo_pago PRIMARY KEY (id_metodo),
    CONSTRAINT uk_metodo_nombre UNIQUE (nombre),
    CONSTRAINT ck_metodo_tipo CHECK (tipo IN ('TARJETA', 'BILLETERA', 'PSE', 'EFECTIVO', 'TRANSFERENCIA'))
) ENGINE = InnoDB;

CREATE TABLE suscripcion (
    id_suscripcion          INT          NOT NULL AUTO_INCREMENT,
    id_cliente              INT          NOT NULL,
    id_plan                 INT          NOT NULL,
    fecha_inicio            DATE         NOT NULL,
    fecha_fin               DATE         NOT NULL,
    estado                  VARCHAR(12)  NOT NULL,
    renovacion_automatica   TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT pk_suscripcion PRIMARY KEY (id_suscripcion),
    CONSTRAINT fk_suscripcion_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_suscripcion_plan FOREIGN KEY (id_plan) REFERENCES plan (id_plan),
    CONSTRAINT ck_suscripcion_estado CHECK (estado IN ('ACTIVA', 'PAUSADA', 'CANCELADA', 'VENCIDA')),
    CONSTRAINT ck_suscripcion_fechas CHECK (fecha_fin > fecha_inicio)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Inventario: lotes de cosecha recibidos en un punto de acopio
-- ---------------------------------------------------------------------
CREATE TABLE lote (
    id_lote                 INT           NOT NULL AUTO_INCREMENT,
    codigo                  VARCHAR(20)   NOT NULL,
    id_finca                INT           NOT NULL,
    id_producto             INT           NOT NULL,
    id_punto                INT           NOT NULL,
    fecha_cosecha           DATE          NOT NULL,
    fecha_vencimiento       DATE          NOT NULL,
    cantidad_kg             DECIMAL(10,1) NOT NULL,
    cantidad_disponible_kg  DECIMAL(10,1) NOT NULL,
    precio_finca_kg         DECIMAL(12,0) NOT NULL,   -- lo que se le paga al campesino
    calidad                 VARCHAR(10)   NOT NULL,
    estado                  VARCHAR(12)   NOT NULL DEFAULT 'DISPONIBLE',
    CONSTRAINT pk_lote PRIMARY KEY (id_lote),
    CONSTRAINT uk_lote_codigo UNIQUE (codigo),
    CONSTRAINT fk_lote_finca FOREIGN KEY (id_finca) REFERENCES finca (id_finca),
    CONSTRAINT fk_lote_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_lote_punto FOREIGN KEY (id_punto) REFERENCES punto_acopio (id_punto),
    CONSTRAINT ck_lote_cantidad CHECK (cantidad_kg > 0 AND cantidad_disponible_kg BETWEEN 0 AND cantidad_kg),
    CONSTRAINT ck_lote_fechas CHECK (fecha_vencimiento > fecha_cosecha),
    CONSTRAINT ck_lote_calidad CHECK (calidad IN ('EXTRA', 'PRIMERA', 'SEGUNDA')),
    CONSTRAINT ck_lote_estado CHECK (estado IN ('DISPONIBLE', 'AGOTADO', 'VENCIDO'))
) ENGINE = InnoDB;

CREATE INDEX ix_lote_producto_estado ON lote (id_producto, estado, fecha_vencimiento);

-- ---------------------------------------------------------------------
-- Ventas
-- ---------------------------------------------------------------------
CREATE TABLE pedido (
    id_pedido                 INT           NOT NULL AUTO_INCREMENT,
    id_cliente                INT           NOT NULL,
    id_punto                  INT           NOT NULL,
    id_suscripcion            INT           NULL,      -- pedido generado por un plan
    fecha_pedido              DATETIME      NOT NULL,
    fecha_entrega_programada  DATE          NOT NULL,
    direccion_entrega         VARCHAR(150)  NOT NULL,
    estado                    VARCHAR(12)   NOT NULL DEFAULT 'PENDIENTE',
    subtotal                  DECIMAL(14,0) NOT NULL DEFAULT 0,
    descuento                 DECIMAL(14,0) NOT NULL DEFAULT 0,
    iva                       DECIMAL(14,0) NOT NULL DEFAULT 0,
    total                     DECIMAL(14,0) NOT NULL DEFAULT 0,
    observaciones             VARCHAR(300)  NULL,
    CONSTRAINT pk_pedido PRIMARY KEY (id_pedido),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_pedido_punto FOREIGN KEY (id_punto) REFERENCES punto_acopio (id_punto),
    CONSTRAINT fk_pedido_suscripcion FOREIGN KEY (id_suscripcion) REFERENCES suscripcion (id_suscripcion) ON DELETE SET NULL,
    CONSTRAINT ck_pedido_estado CHECK (estado IN ('PENDIENTE', 'PAGADO', 'DESPACHADO', 'ENTREGADO', 'CANCELADO'))
) ENGINE = InnoDB;

CREATE INDEX ix_pedido_fecha ON pedido (fecha_pedido);

-- Entidad debil: su llave incluye la del pedido
CREATE TABLE detalle_pedido (
    id_pedido    INT           NOT NULL,
    linea        INT           NOT NULL,
    id_lote      INT           NOT NULL,
    cantidad_kg  DECIMAL(10,1) NOT NULL,
    precio_kg    DECIMAL(12,0) NOT NULL,
    iva_pct      DECIMAL(5,2)  NOT NULL DEFAULT 0,
    subtotal     DECIMAL(14,0) NOT NULL,
    CONSTRAINT pk_detalle_pedido PRIMARY KEY (id_pedido, linea),
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_lote FOREIGN KEY (id_lote) REFERENCES lote (id_lote),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad_kg > 0)
) ENGINE = InnoDB;

CREATE TABLE pago (
    id_pago         INT           NOT NULL AUTO_INCREMENT,
    id_metodo       INT           NOT NULL,
    id_pedido       INT           NULL,
    id_suscripcion  INT           NULL,
    fecha_pago      DATETIME      NOT NULL,
    valor           DECIMAL(14,0) NOT NULL,
    referencia      VARCHAR(30)   NOT NULL,
    estado          VARCHAR(12)   NOT NULL,
    CONSTRAINT pk_pago PRIMARY KEY (id_pago),
    CONSTRAINT uk_pago_referencia UNIQUE (referencia),
    CONSTRAINT fk_pago_metodo FOREIGN KEY (id_metodo) REFERENCES metodo_pago (id_metodo),
    CONSTRAINT fk_pago_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_pago_suscripcion FOREIGN KEY (id_suscripcion) REFERENCES suscripcion (id_suscripcion) ON DELETE CASCADE,
    CONSTRAINT ck_pago_valor CHECK (valor > 0),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('APROBADO', 'RECHAZADO', 'PENDIENTE', 'REEMBOLSADO')),
    -- un pago es de un pedido o de una suscripcion, nunca de ambos
    CONSTRAINT ck_pago_origen CHECK ((id_pedido IS NULL) <> (id_suscripcion IS NULL))
) ENGINE = InnoDB;

CREATE TABLE factura (
    id_factura     INT           NOT NULL AUTO_INCREMENT,
    id_pedido      INT           NOT NULL,
    numero         VARCHAR(20)   NOT NULL,
    fecha_emision  DATETIME      NOT NULL,
    subtotal       DECIMAL(14,0) NOT NULL,
    descuento      DECIMAL(14,0) NOT NULL,
    iva            DECIMAL(14,0) NOT NULL,
    total          DECIMAL(14,0) NOT NULL,
    CONSTRAINT pk_factura PRIMARY KEY (id_factura),
    CONSTRAINT uk_factura_numero UNIQUE (numero),
    CONSTRAINT uk_factura_pedido UNIQUE (id_pedido),
    CONSTRAINT fk_factura_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE despacho (
    id_despacho       INT           NOT NULL AUTO_INCREMENT,
    id_pedido         INT           NOT NULL,
    id_transportador  INT           NOT NULL,
    numero_guia       VARCHAR(20)   NOT NULL,
    fecha_salida      DATETIME      NOT NULL,
    fecha_entrega     DATETIME      NULL,
    estado            VARCHAR(12)   NOT NULL,
    temperatura_c     DECIMAL(4,1)  NULL,
    observaciones     VARCHAR(300)  NULL,
    CONSTRAINT pk_despacho PRIMARY KEY (id_despacho),
    CONSTRAINT uk_despacho_guia UNIQUE (numero_guia),
    CONSTRAINT uk_despacho_pedido UNIQUE (id_pedido),
    CONSTRAINT fk_despacho_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_despacho_transportador FOREIGN KEY (id_transportador) REFERENCES transportador (id_transportador),
    CONSTRAINT ck_despacho_estado CHECK (estado IN ('EN_RUTA', 'ENTREGADO', 'DEVUELTO'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Recordatorios y notificaciones personalizadas
-- ---------------------------------------------------------------------
CREATE TABLE notificacion (
    id_notificacion   INT           NOT NULL AUTO_INCREMENT,
    id_usuario        INT           NOT NULL,
    tipo              VARCHAR(15)   NOT NULL,
    titulo            VARCHAR(100)  NOT NULL,
    mensaje           VARCHAR(400)  NOT NULL,
    fecha_programada  DATETIME      NOT NULL,      -- cuando debe mostrarse (recordatorio)
    leida             TINYINT(1)    NOT NULL DEFAULT 0,
    fecha_creacion    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notificacion PRIMARY KEY (id_notificacion),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_notificacion_tipo CHECK (tipo IN ('RECORDATORIO', 'PEDIDO', 'PAGO', 'SUSCRIPCION', 'INVENTARIO', 'SISTEMA'))
) ENGINE = InnoDB;

CREATE INDEX ix_notificacion_usuario ON notificacion (id_usuario, leida, fecha_programada);
