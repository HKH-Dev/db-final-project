-- =====================================================================================
--  FrutiMarket BD · Modelo de datos para Oracle SQL Developer Data Modeler (Oracle 12c+)
--  Universidad del Quindío · Bases de Datos 2026-2
--
--  GENERADO desde database/01_esquema.sql (MariaDB) por database/generador/generar_oracle.py.
--  Es el mismo modelo que usa la aplicación: 31 tablas (30 entidades fuertes y la entidad
--  débil DETALLE_PEDIDO), con sus columnas, llaves, restricciones CHECK, índices y comentarios.
--  Equivalencias de tipos: INT -> NUMBER(10), DECIMAL(p,s) -> NUMBER(p,s), VARCHAR -> VARCHAR2,
--  TINYINT(1) -> NUMBER(1) con CHECK (0,1), DATETIME -> TIMESTAMP, AUTO_INCREMENT -> IDENTITY.
--
--  Uso en Data Modeler:  File > Import > DDL File > este archivo > Oracle Database 12c
--  (crea el modelo relacional; Design > Engineer to Logical Model genera el modelo lógico).
-- =====================================================================================


CREATE TABLE DEPARTAMENTO (
    nombre                    VARCHAR2(60)   NOT NULL,
    codigo_dane               CHAR(2)        NOT NULL,
    CONSTRAINT PK_DEPARTAMENTO PRIMARY KEY (nombre),
    CONSTRAINT UK_DEPARTAMENTO_DANE UNIQUE (codigo_dane),
    CONSTRAINT CK_DEPARTAMENTO_DANE CHECK (REGEXP_LIKE(codigo_dane, '^[0-9]{2}$'))
);
COMMENT ON TABLE DEPARTAMENTO IS 'Departamentos de Colombia con su código DANE; agrupan a los municipios.';
COMMENT ON COLUMN DEPARTAMENTO.nombre IS 'Nombre oficial del departamento; es la llave natural.';
COMMENT ON COLUMN DEPARTAMENTO.codigo_dane IS 'Código DANE de dos dígitos del departamento.';

CREATE TABLE CARGO (
    nombre                    VARCHAR2(40)   NOT NULL,
    area                      VARCHAR2(20)   NOT NULL,
    descripcion               VARCHAR2(200)  NOT NULL,
    CONSTRAINT PK_CARGO PRIMARY KEY (nombre),
    CONSTRAINT CK_CARGO_AREA CHECK (area IN ('OPERACION', 'ADMINISTRACION', 'COMERCIAL', 'DIRECCION'))
);
COMMENT ON TABLE CARGO IS 'Cargos que puede ocupar un empleado de FrutiMarket, con el área a la que pertenecen.';
COMMENT ON COLUMN CARGO.nombre IS 'Nombre del cargo; es la llave natural.';
COMMENT ON COLUMN CARGO.area IS 'Área de la empresa a la que pertenece el cargo.';
COMMENT ON COLUMN CARGO.descripcion IS 'Funciones principales del cargo.';

CREATE TABLE TIPO_CLIENTE (
    codigo                    VARCHAR2(20)   NOT NULL,
    nombre                    VARCHAR2(40)   NOT NULL,
    descripcion               VARCHAR2(200)  NOT NULL,
    CONSTRAINT PK_TIPO_CLIENTE PRIMARY KEY (codigo),
    CONSTRAINT UK_TIPO_CLIENTE_NOMBRE UNIQUE (nombre)
);
COMMENT ON TABLE TIPO_CLIENTE IS 'Clasificación de los clientes (hogar, restaurante, hotel, tienda, supermercado).';
COMMENT ON COLUMN TIPO_CLIENTE.codigo IS 'Código del tipo de cliente (llave natural).';
COMMENT ON COLUMN TIPO_CLIENTE.nombre IS 'Nombre que se muestra en pantallas y reportes.';
COMMENT ON COLUMN TIPO_CLIENTE.descripcion IS 'Explicación de qué clientes entran en el tipo.';

CREATE TABLE PERIODICIDAD (
    codigo                    VARCHAR2(10)   NOT NULL,
    nombre                    VARCHAR2(30)   NOT NULL,
    dias                      NUMBER(10)     NOT NULL,
    CONSTRAINT PK_PERIODICIDAD PRIMARY KEY (codigo),
    CONSTRAINT UK_PERIODICIDAD_NOMBRE UNIQUE (nombre),
    CONSTRAINT CK_PERIODICIDAD_DIAS CHECK (dias BETWEEN 1 AND 365)
);
COMMENT ON TABLE PERIODICIDAD IS 'Frecuencia de entrega de los planes de suscripción y duración de cada periodo en días.';
COMMENT ON COLUMN PERIODICIDAD.codigo IS 'Código de la periodicidad (llave natural).';
COMMENT ON COLUMN PERIODICIDAD.nombre IS 'Nombre que se muestra.';
COMMENT ON COLUMN PERIODICIDAD.dias IS 'Duración de un periodo del plan en días; con ella se calcula la fecha de fin de la suscripción.';

CREATE TABLE TIPO_METODO_PAGO (
    codigo                    VARCHAR2(20)   NOT NULL,
    nombre                    VARCHAR2(40)   NOT NULL,
    descripcion               VARCHAR2(200)  NOT NULL,
    CONSTRAINT PK_TIPO_METODO_PAGO PRIMARY KEY (codigo),
    CONSTRAINT UK_TIPO_METODO_NOMBRE UNIQUE (nombre)
);
COMMENT ON TABLE TIPO_METODO_PAGO IS 'Familias de medios de pago (tarjeta, PSE, billetera digital, transferencia, efectivo).';
COMMENT ON COLUMN TIPO_METODO_PAGO.codigo IS 'Código del tipo de medio de pago (llave natural).';
COMMENT ON COLUMN TIPO_METODO_PAGO.nombre IS 'Nombre que se muestra.';
COMMENT ON COLUMN TIPO_METODO_PAGO.descripcion IS 'Cómo funciona ese tipo de pago.';

CREATE TABLE TIPO_NOTIFICACION (
    codigo                    VARCHAR2(15)   NOT NULL,
    nombre                    VARCHAR2(40)   NOT NULL,
    descripcion               VARCHAR2(200)  NOT NULL,
    CONSTRAINT PK_TIPO_NOTIFICACION PRIMARY KEY (codigo),
    CONSTRAINT UK_TIPO_NOTIFICACION_NOMBRE UNIQUE (nombre)
);
COMMENT ON TABLE TIPO_NOTIFICACION IS 'Clases de notificación y recordatorio que recibe un usuario.';
COMMENT ON COLUMN TIPO_NOTIFICACION.codigo IS 'Código del tipo de notificación (llave natural).';
COMMENT ON COLUMN TIPO_NOTIFICACION.nombre IS 'Nombre que se muestra.';
COMMENT ON COLUMN TIPO_NOTIFICACION.descripcion IS 'Cuándo se genera ese tipo de aviso.';

CREATE TABLE ESTADO_SUSCRIPCION (
    codigo                    VARCHAR2(12)   NOT NULL,
    nombre                    VARCHAR2(30)   NOT NULL,
    descripcion               VARCHAR2(200)  NOT NULL,
    CONSTRAINT PK_ESTADO_SUSCRIPCION PRIMARY KEY (codigo),
    CONSTRAINT UK_ESTADO_SUSCRIPCION_NOMBRE UNIQUE (nombre)
);
COMMENT ON TABLE ESTADO_SUSCRIPCION IS 'Estados posibles de una suscripción a un plan.';
COMMENT ON COLUMN ESTADO_SUSCRIPCION.codigo IS 'Código del estado (llave natural).';
COMMENT ON COLUMN ESTADO_SUSCRIPCION.nombre IS 'Nombre que se muestra.';
COMMENT ON COLUMN ESTADO_SUSCRIPCION.descripcion IS 'Significado del estado.';

CREATE TABLE ESTADO_PEDIDO (
    codigo                    VARCHAR2(12)   NOT NULL,
    nombre                    VARCHAR2(30)   NOT NULL,
    descripcion               VARCHAR2(200)  NOT NULL,
    orden                     NUMBER(10)     NOT NULL,
    CONSTRAINT PK_ESTADO_PEDIDO PRIMARY KEY (codigo),
    CONSTRAINT UK_ESTADO_PEDIDO_NOMBRE UNIQUE (nombre),
    CONSTRAINT UK_ESTADO_PEDIDO_ORDEN UNIQUE (orden)
);
COMMENT ON TABLE ESTADO_PEDIDO IS 'Estados del flujo de un pedido, con su orden.';
COMMENT ON COLUMN ESTADO_PEDIDO.codigo IS 'Código del estado (llave natural).';
COMMENT ON COLUMN ESTADO_PEDIDO.nombre IS 'Nombre que se muestra.';
COMMENT ON COLUMN ESTADO_PEDIDO.descripcion IS 'Significado del estado.';
COMMENT ON COLUMN ESTADO_PEDIDO.orden IS 'Posición del estado en el flujo del pedido.';

CREATE TABLE ESTADO_PAGO (
    codigo                    VARCHAR2(12)   NOT NULL,
    nombre                    VARCHAR2(30)   NOT NULL,
    descripcion               VARCHAR2(200)  NOT NULL,
    CONSTRAINT PK_ESTADO_PAGO PRIMARY KEY (codigo),
    CONSTRAINT UK_ESTADO_PAGO_NOMBRE UNIQUE (nombre)
);
COMMENT ON TABLE ESTADO_PAGO IS 'Resultados posibles de un pago en la pasarela.';
COMMENT ON COLUMN ESTADO_PAGO.codigo IS 'Código del estado (llave natural).';
COMMENT ON COLUMN ESTADO_PAGO.nombre IS 'Nombre que se muestra.';
COMMENT ON COLUMN ESTADO_PAGO.descripcion IS 'Significado del estado.';

CREATE TABLE ESTADO_DESPACHO (
    codigo                    VARCHAR2(12)   NOT NULL,
    nombre                    VARCHAR2(30)   NOT NULL,
    descripcion               VARCHAR2(200)  NOT NULL,
    CONSTRAINT PK_ESTADO_DESPACHO PRIMARY KEY (codigo),
    CONSTRAINT UK_ESTADO_DESPACHO_NOMBRE UNIQUE (nombre)
);
COMMENT ON TABLE ESTADO_DESPACHO IS 'Estados de la entrega de un pedido por el transportador.';
COMMENT ON COLUMN ESTADO_DESPACHO.codigo IS 'Código del estado (llave natural).';
COMMENT ON COLUMN ESTADO_DESPACHO.nombre IS 'Nombre que se muestra.';
COMMENT ON COLUMN ESTADO_DESPACHO.descripcion IS 'Significado del estado.';

CREATE TABLE MUNICIPIO (
    id_municipio              NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    nombre                    VARCHAR2(80)   NOT NULL,
    departamento              VARCHAR2(60)   NOT NULL,
    codigo_dane               CHAR(5)        NOT NULL,
    CONSTRAINT PK_MUNICIPIO PRIMARY KEY (id_municipio),
    CONSTRAINT UK_MUNICIPIO_DANE UNIQUE (codigo_dane),
    CONSTRAINT FK_MUNICIPIO_DEPARTAMENTO FOREIGN KEY (departamento) REFERENCES DEPARTAMENTO (nombre)
);
COMMENT ON TABLE MUNICIPIO IS 'Municipios donde hay clientes, fincas o puntos de acopio, con su código DANE.';
COMMENT ON COLUMN MUNICIPIO.id_municipio IS 'Identificador del municipio.';
COMMENT ON COLUMN MUNICIPIO.nombre IS 'Nombre del municipio.';
COMMENT ON COLUMN MUNICIPIO.departamento IS 'Departamento al que pertenece.';
COMMENT ON COLUMN MUNICIPIO.codigo_dane IS 'Código DANE de cinco dígitos del municipio.';

CREATE TABLE ROL (
    id_rol                    NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    nombre                    VARCHAR2(30)   NOT NULL,
    descripcion               VARCHAR2(200)  NOT NULL,
    CONSTRAINT PK_ROL PRIMARY KEY (id_rol),
    CONSTRAINT UK_ROL_NOMBRE UNIQUE (nombre)
);
COMMENT ON TABLE ROL IS 'Roles de seguridad que definen qué puede hacer cada usuario en el sistema.';
COMMENT ON COLUMN ROL.id_rol IS 'Identificador del rol.';
COMMENT ON COLUMN ROL.nombre IS 'Nombre del rol (ADMIN, OPERADOR, CAMPESINO, CLIENTE).';
COMMENT ON COLUMN ROL.descripcion IS 'Qué puede hacer un usuario con este rol.';

CREATE TABLE USUARIO (
    id_usuario                NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_rol                    NUMBER(10)     NOT NULL,
    username                  VARCHAR2(40)   NOT NULL,
    password_hash             VARCHAR2(100)  NOT NULL,
    nombre_completo           VARCHAR2(120)  NOT NULL,
    email                     VARCHAR2(120)  NOT NULL,
    telefono                  VARCHAR2(20),
    activo                    NUMBER(1)      DEFAULT 1 NOT NULL,
    fecha_registro            TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    ultimo_acceso             TIMESTAMP,
    CONSTRAINT PK_USUARIO PRIMARY KEY (id_usuario),
    CONSTRAINT UK_USUARIO_USERNAME UNIQUE (username),
    CONSTRAINT UK_USUARIO_EMAIL UNIQUE (email),
    CONSTRAINT FK_USUARIO_ROL FOREIGN KEY (id_rol) REFERENCES ROL (id_rol),
    CONSTRAINT CK_USUARIO_ACTIVO CHECK (activo IN (0, 1))
);
COMMENT ON TABLE USUARIO IS 'Cuentas de acceso al sistema (inicio de sesión, perfil y notificaciones).';
COMMENT ON COLUMN USUARIO.id_usuario IS 'Identificador del usuario.';
COMMENT ON COLUMN USUARIO.id_rol IS 'Rol que determina los permisos del usuario.';
COMMENT ON COLUMN USUARIO.username IS 'Nombre de usuario con el que inicia sesión.';
COMMENT ON COLUMN USUARIO.password_hash IS 'Contraseña cifrada con BCrypt (nunca se guarda en texto plano).';
COMMENT ON COLUMN USUARIO.nombre_completo IS 'Nombre que se muestra en el perfil y en los documentos.';
COMMENT ON COLUMN USUARIO.email IS 'Correo electrónico; también sirve para iniciar sesión.';
COMMENT ON COLUMN USUARIO.telefono IS 'Celular de contacto.';
COMMENT ON COLUMN USUARIO.activo IS 'Indica si la cuenta puede iniciar sesión.';
COMMENT ON COLUMN USUARIO.fecha_registro IS 'Fecha y hora de creación de la cuenta.';
COMMENT ON COLUMN USUARIO.ultimo_acceso IS 'Fecha y hora del último inicio de sesión exitoso.';

CREATE TABLE CAMPESINO (
    id_campesino              NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_usuario                NUMBER(10),
    id_municipio              NUMBER(10)     NOT NULL,
    tipo_documento            VARCHAR2(5)    DEFAULT 'CC' NOT NULL,
    numero_documento          VARCHAR2(20)   NOT NULL,
    nombres                   VARCHAR2(60)   NOT NULL,
    apellidos                 VARCHAR2(60)   NOT NULL,
    telefono                  VARCHAR2(20)   NOT NULL,
    email                     VARCHAR2(120),
    fecha_vinculacion         DATE           NOT NULL,
    certificado_bpa           NUMBER(1)      DEFAULT 0 NOT NULL,
    activo                    NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_CAMPESINO PRIMARY KEY (id_campesino),
    CONSTRAINT UK_CAMPESINO_DOCUMENTO UNIQUE (numero_documento),
    CONSTRAINT UK_CAMPESINO_USUARIO UNIQUE (id_usuario),
    CONSTRAINT FK_CAMPESINO_USUARIO FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE SET NULL,
    CONSTRAINT FK_CAMPESINO_MUNICIPIO FOREIGN KEY (id_municipio) REFERENCES MUNICIPIO (id_municipio),
    CONSTRAINT CK_CAMPESINO_TIPO_DOC CHECK (tipo_documento IN ('CC', 'CE', 'PPT')),
    CONSTRAINT CK_CAMPESINO_CERTIFICADO_BPA CHECK (certificado_bpa IN (0, 1)),
    CONSTRAINT CK_CAMPESINO_ACTIVO CHECK (activo IN (0, 1))
);
COMMENT ON TABLE CAMPESINO IS 'Productores del Quindío vinculados que entregan sus cosechas a FrutiMarket.';
COMMENT ON COLUMN CAMPESINO.id_campesino IS 'Identificador del campesino.';
COMMENT ON COLUMN CAMPESINO.id_usuario IS 'Cuenta con la que el campesino entra al portal (opcional).';
COMMENT ON COLUMN CAMPESINO.id_municipio IS 'Municipio donde reside.';
COMMENT ON COLUMN CAMPESINO.tipo_documento IS 'Tipo de documento de identidad (CC, CE o PPT).';
COMMENT ON COLUMN CAMPESINO.numero_documento IS 'Número del documento de identidad.';
COMMENT ON COLUMN CAMPESINO.nombres IS 'Nombres del campesino.';
COMMENT ON COLUMN CAMPESINO.apellidos IS 'Apellidos del campesino.';
COMMENT ON COLUMN CAMPESINO.telefono IS 'Celular de contacto.';
COMMENT ON COLUMN CAMPESINO.email IS 'Correo electrónico (opcional).';
COMMENT ON COLUMN CAMPESINO.fecha_vinculacion IS 'Fecha en que empezó a proveer a FrutiMarket.';
COMMENT ON COLUMN CAMPESINO.certificado_bpa IS 'Indica si tiene certificado de Buenas Prácticas Agrícolas del ICA.';
COMMENT ON COLUMN CAMPESINO.activo IS 'Indica si sigue vinculado.';

CREATE TABLE FINCA (
    id_finca                  NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_campesino              NUMBER(10)     NOT NULL,
    id_municipio              NUMBER(10)     NOT NULL,
    nombre                    VARCHAR2(80)   NOT NULL,
    vereda                    VARCHAR2(80)   NOT NULL,
    area_hectareas            NUMBER(8,2)    NOT NULL,
    altitud_msnm              NUMBER(10)     NOT NULL,
    CONSTRAINT PK_FINCA PRIMARY KEY (id_finca),
    CONSTRAINT FK_FINCA_CAMPESINO FOREIGN KEY (id_campesino) REFERENCES CAMPESINO (id_campesino),
    CONSTRAINT FK_FINCA_MUNICIPIO FOREIGN KEY (id_municipio) REFERENCES MUNICIPIO (id_municipio),
    CONSTRAINT CK_FINCA_AREA CHECK (area_hectareas > 0),
    CONSTRAINT CK_FINCA_ALTITUD CHECK (altitud_msnm BETWEEN 0 AND 4000)
);
COMMENT ON TABLE FINCA IS 'Predios de los campesinos donde se cultivan los productos.';
COMMENT ON COLUMN FINCA.id_finca IS 'Identificador de la finca.';
COMMENT ON COLUMN FINCA.id_campesino IS 'Campesino propietario o a cargo de la finca.';
COMMENT ON COLUMN FINCA.id_municipio IS 'Municipio donde está la finca.';
COMMENT ON COLUMN FINCA.nombre IS 'Nombre de la finca.';
COMMENT ON COLUMN FINCA.vereda IS 'Vereda donde se ubica.';
COMMENT ON COLUMN FINCA.area_hectareas IS 'Área cultivable en hectáreas.';
COMMENT ON COLUMN FINCA.altitud_msnm IS 'Altitud en metros sobre el nivel del mar (define qué se puede cultivar).';

CREATE TABLE CATEGORIA (
    id_categoria              NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    nombre                    VARCHAR2(60)   NOT NULL,
    descripcion               VARCHAR2(250)  NOT NULL,
    CONSTRAINT PK_CATEGORIA PRIMARY KEY (id_categoria),
    CONSTRAINT UK_CATEGORIA_NOMBRE UNIQUE (nombre)
);
COMMENT ON TABLE CATEGORIA IS 'Agrupación de los productos del catálogo (musáceas, cítricos, café…).';
COMMENT ON COLUMN CATEGORIA.id_categoria IS 'Identificador de la categoría.';
COMMENT ON COLUMN CATEGORIA.nombre IS 'Nombre de la categoría.';
COMMENT ON COLUMN CATEGORIA.descripcion IS 'Qué productos agrupa.';

CREATE TABLE PRODUCTO (
    id_producto               NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_categoria              NUMBER(10)     NOT NULL,
    nombre                    VARCHAR2(80)   NOT NULL,
    variedad                  VARCHAR2(60)   NOT NULL,
    unidad_medida             VARCHAR2(10)   DEFAULT 'KG' NOT NULL,
    precio_venta_kg           NUMBER(12)     NOT NULL,
    iva_pct                   NUMBER(5,2)    DEFAULT 0 NOT NULL,
    vida_util_dias            NUMBER(10)     NOT NULL,
    requiere_refrigeracion    NUMBER(1)      DEFAULT 0 NOT NULL,
    activo                    NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_PRODUCTO PRIMARY KEY (id_producto),
    CONSTRAINT UK_PRODUCTO_NOMBRE UNIQUE (nombre, variedad),
    CONSTRAINT FK_PRODUCTO_CATEGORIA FOREIGN KEY (id_categoria) REFERENCES CATEGORIA (id_categoria),
    CONSTRAINT CK_PRODUCTO_PRECIO CHECK (precio_venta_kg > 0),
    CONSTRAINT CK_PRODUCTO_IVA CHECK (iva_pct IN (0, 5, 19)),
    CONSTRAINT CK_PRODUCTO_VIDA CHECK (vida_util_dias > 0),
    CONSTRAINT CK_PRODUCTO_REQUIERE_REFRIGERA CHECK (requiere_refrigeracion IN (0, 1)),
    CONSTRAINT CK_PRODUCTO_ACTIVO CHECK (activo IN (0, 1))
);
COMMENT ON TABLE PRODUCTO IS 'Productos que se compran a los campesinos y se venden por kilo.';
COMMENT ON COLUMN PRODUCTO.id_producto IS 'Identificador del producto.';
COMMENT ON COLUMN PRODUCTO.id_categoria IS 'Categoría a la que pertenece.';
COMMENT ON COLUMN PRODUCTO.nombre IS 'Nombre del producto (p. ej. Aguacate).';
COMMENT ON COLUMN PRODUCTO.variedad IS 'Variedad o presentación (p. ej. Hass).';
COMMENT ON COLUMN PRODUCTO.unidad_medida IS 'Unidad en la que se vende (kilogramo).';
COMMENT ON COLUMN PRODUCTO.precio_venta_kg IS 'Precio de venta por kilo en pesos colombianos.';
COMMENT ON COLUMN PRODUCTO.iva_pct IS 'Porcentaje de IVA que aplica (0, 5 o 19 %).';
COMMENT ON COLUMN PRODUCTO.vida_util_dias IS 'Días que dura el producto desde la cosecha; con ellos se calcula el vencimiento del lote.';
COMMENT ON COLUMN PRODUCTO.requiere_refrigeracion IS 'Indica si debe transportarse y almacenarse refrigerado.';
COMMENT ON COLUMN PRODUCTO.activo IS 'Indica si se sigue ofreciendo.';

CREATE TABLE PUNTO_ACOPIO (
    id_punto                  NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_municipio              NUMBER(10)     NOT NULL,
    nombre                    VARCHAR2(80)   NOT NULL,
    direccion                 VARCHAR2(120)  NOT NULL,
    telefono                  VARCHAR2(20)   NOT NULL,
    capacidad_kg              NUMBER(10)     NOT NULL,
    refrigerado               NUMBER(1)      DEFAULT 0 NOT NULL,
    activo                    NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_PUNTO_ACOPIO PRIMARY KEY (id_punto),
    CONSTRAINT UK_PUNTO_NOMBRE UNIQUE (nombre),
    CONSTRAINT FK_PUNTO_MUNICIPIO FOREIGN KEY (id_municipio) REFERENCES MUNICIPIO (id_municipio),
    CONSTRAINT CK_PUNTO_CAPACIDAD CHECK (capacidad_kg > 0),
    CONSTRAINT CK_PUNTO_ACOP_REFRIGERADO CHECK (refrigerado IN (0, 1)),
    CONSTRAINT CK_PUNTO_ACOP_ACTIVO CHECK (activo IN (0, 1))
);
COMMENT ON TABLE PUNTO_ACOPIO IS 'Centros donde se reciben, almacenan y despachan los productos.';
COMMENT ON COLUMN PUNTO_ACOPIO.id_punto IS 'Identificador del punto de acopio.';
COMMENT ON COLUMN PUNTO_ACOPIO.id_municipio IS 'Municipio donde está el punto.';
COMMENT ON COLUMN PUNTO_ACOPIO.nombre IS 'Nombre del punto.';
COMMENT ON COLUMN PUNTO_ACOPIO.direccion IS 'Dirección física.';
COMMENT ON COLUMN PUNTO_ACOPIO.telefono IS 'Teléfono del punto.';
COMMENT ON COLUMN PUNTO_ACOPIO.capacidad_kg IS 'Capacidad de almacenamiento en kilos.';
COMMENT ON COLUMN PUNTO_ACOPIO.refrigerado IS 'Indica si tiene cuarto frío.';
COMMENT ON COLUMN PUNTO_ACOPIO.activo IS 'Indica si está operando.';

CREATE TABLE EMPLEADO (
    id_empleado               NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_punto                  NUMBER(10)     NOT NULL,
    id_usuario                NUMBER(10),
    numero_documento          VARCHAR2(20)   NOT NULL,
    nombres                   VARCHAR2(60)   NOT NULL,
    apellidos                 VARCHAR2(60)   NOT NULL,
    cargo                     VARCHAR2(40)   NOT NULL,
    salario                   NUMBER(12)     NOT NULL,
    fecha_ingreso             DATE           NOT NULL,
    telefono                  VARCHAR2(20)   NOT NULL,
    email                     VARCHAR2(120)  NOT NULL,
    activo                    NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_EMPLEADO PRIMARY KEY (id_empleado),
    CONSTRAINT UK_EMPLEADO_DOCUMENTO UNIQUE (numero_documento),
    CONSTRAINT UK_EMPLEADO_USUARIO UNIQUE (id_usuario),
    CONSTRAINT FK_EMPLEADO_PUNTO FOREIGN KEY (id_punto) REFERENCES PUNTO_ACOPIO (id_punto),
    CONSTRAINT FK_EMPLEADO_USUARIO FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE SET NULL,
    CONSTRAINT FK_EMPLEADO_CARGO FOREIGN KEY (cargo) REFERENCES CARGO (nombre),
    CONSTRAINT CK_EMPLEADO_SALARIO CHECK (salario >= 1750905),
    CONSTRAINT CK_EMPLEADO_ACTIVO CHECK (activo IN (0, 1))
);
COMMENT ON TABLE EMPLEADO IS 'Personal de FrutiMarket asignado a un punto de acopio.';
COMMENT ON COLUMN EMPLEADO.id_empleado IS 'Identificador del empleado.';
COMMENT ON COLUMN EMPLEADO.id_punto IS 'Punto de acopio donde trabaja.';
COMMENT ON COLUMN EMPLEADO.id_usuario IS 'Cuenta con la que entra al sistema (solo los coordinadores).';
COMMENT ON COLUMN EMPLEADO.numero_documento IS 'Cédula del empleado.';
COMMENT ON COLUMN EMPLEADO.nombres IS 'Nombres del empleado.';
COMMENT ON COLUMN EMPLEADO.apellidos IS 'Apellidos del empleado.';
COMMENT ON COLUMN EMPLEADO.cargo IS 'Cargo que ocupa.';
COMMENT ON COLUMN EMPLEADO.salario IS 'Salario mensual en pesos (mínimo el SMMLV 2026).';
COMMENT ON COLUMN EMPLEADO.fecha_ingreso IS 'Fecha de ingreso a la empresa.';
COMMENT ON COLUMN EMPLEADO.telefono IS 'Celular de contacto.';
COMMENT ON COLUMN EMPLEADO.email IS 'Correo corporativo o personal.';
COMMENT ON COLUMN EMPLEADO.activo IS 'Indica si sigue trabajando en la empresa.';

CREATE TABLE TRANSPORTADOR (
    id_transportador          NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    numero_documento          VARCHAR2(20)   NOT NULL,
    nombre                    VARCHAR2(120)  NOT NULL,
    telefono                  VARCHAR2(20)   NOT NULL,
    placa                     CHAR(6)        NOT NULL,
    tipo_vehiculo             VARCHAR2(30)   NOT NULL,
    capacidad_kg              NUMBER(10)     NOT NULL,
    refrigerado               NUMBER(1)      DEFAULT 0 NOT NULL,
    activo                    NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_TRANSPORTADOR PRIMARY KEY (id_transportador),
    CONSTRAINT UK_TRANSPORTADOR_DOCUMENTO UNIQUE (numero_documento),
    CONSTRAINT UK_TRANSPORTADOR_PLACA UNIQUE (placa),
    CONSTRAINT CK_TRANSPORTA_REFRIGERADO CHECK (refrigerado IN (0, 1)),
    CONSTRAINT CK_TRANSPORTA_ACTIVO CHECK (activo IN (0, 1))
);
COMMENT ON TABLE TRANSPORTADOR IS 'Transportadores (personas o empresas) con su vehículo, que llevan los pedidos.';
COMMENT ON COLUMN TRANSPORTADOR.id_transportador IS 'Identificador del transportador.';
COMMENT ON COLUMN TRANSPORTADOR.numero_documento IS 'Cédula o NIT del transportador.';
COMMENT ON COLUMN TRANSPORTADOR.nombre IS 'Nombre de la persona o razón social.';
COMMENT ON COLUMN TRANSPORTADOR.telefono IS 'Celular de contacto.';
COMMENT ON COLUMN TRANSPORTADOR.placa IS 'Placa del vehículo.';
COMMENT ON COLUMN TRANSPORTADOR.tipo_vehiculo IS 'Tipo de vehículo (camión NHR, turbo, furgón…).';
COMMENT ON COLUMN TRANSPORTADOR.capacidad_kg IS 'Carga máxima del vehículo en kilos.';
COMMENT ON COLUMN TRANSPORTADOR.refrigerado IS 'Indica si el vehículo tiene refrigeración.';
COMMENT ON COLUMN TRANSPORTADOR.activo IS 'Indica si se le pueden asignar despachos.';

CREATE TABLE CLIENTE (
    id_cliente                NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_usuario                NUMBER(10),
    id_municipio              NUMBER(10)     NOT NULL,
    tipo_cliente              VARCHAR2(20)   NOT NULL,
    tipo_documento            VARCHAR2(5)    NOT NULL,
    numero_documento          VARCHAR2(20)   NOT NULL,
    nombre                    VARCHAR2(120)  NOT NULL,
    telefono                  VARCHAR2(20)   NOT NULL,
    email                     VARCHAR2(120)  NOT NULL,
    direccion                 VARCHAR2(150)  NOT NULL,
    fecha_registro            DATE           NOT NULL,
    activo                    NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_CLIENTE PRIMARY KEY (id_cliente),
    CONSTRAINT UK_CLIENTE_DOCUMENTO UNIQUE (numero_documento),
    CONSTRAINT UK_CLIENTE_USUARIO UNIQUE (id_usuario),
    CONSTRAINT FK_CLIENTE_USUARIO FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE SET NULL,
    CONSTRAINT FK_CLIENTE_MUNICIPIO FOREIGN KEY (id_municipio) REFERENCES MUNICIPIO (id_municipio),
    CONSTRAINT FK_CLIENTE_TIPO FOREIGN KEY (tipo_cliente) REFERENCES TIPO_CLIENTE (codigo),
    CONSTRAINT CK_CLIENTE_TIPO_DOC CHECK (tipo_documento IN ('CC', 'CE', 'NIT')),
    CONSTRAINT CK_CLIENTE_ACTIVO CHECK (activo IN (0, 1))
);
COMMENT ON TABLE CLIENTE IS 'Compradores: hogares, restaurantes, hoteles, tiendas y supermercados.';
COMMENT ON COLUMN CLIENTE.id_cliente IS 'Identificador del cliente.';
COMMENT ON COLUMN CLIENTE.id_usuario IS 'Cuenta con la que el cliente compra en línea (opcional).';
COMMENT ON COLUMN CLIENTE.id_municipio IS 'Municipio de la dirección del cliente.';
COMMENT ON COLUMN CLIENTE.tipo_cliente IS 'Tipo de cliente.';
COMMENT ON COLUMN CLIENTE.tipo_documento IS 'Tipo de documento (CC, CE o NIT).';
COMMENT ON COLUMN CLIENTE.numero_documento IS 'Número de documento o NIT con dígito de verificación.';
COMMENT ON COLUMN CLIENTE.nombre IS 'Nombre de la persona o razón social.';
COMMENT ON COLUMN CLIENTE.telefono IS 'Teléfono de contacto.';
COMMENT ON COLUMN CLIENTE.email IS 'Correo al que se envían facturas y avisos.';
COMMENT ON COLUMN CLIENTE.direccion IS 'Dirección de entrega por defecto.';
COMMENT ON COLUMN CLIENTE.fecha_registro IS 'Fecha en que se registró como cliente.';
COMMENT ON COLUMN CLIENTE.activo IS 'Indica si puede hacer pedidos.';

CREATE TABLE PLAN (
    id_plan                   NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    nombre                    VARCHAR2(60)   NOT NULL,
    descripcion               VARCHAR2(300)  NOT NULL,
    periodicidad              VARCHAR2(10)   NOT NULL,
    precio                    NUMBER(12)     NOT NULL,
    kg_incluidos              NUMBER(8,1)    NOT NULL,
    descuento_pct             NUMBER(5,2)    DEFAULT 0 NOT NULL,
    activo                    NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_PLAN PRIMARY KEY (id_plan),
    CONSTRAINT UK_PLAN_NOMBRE UNIQUE (nombre),
    CONSTRAINT FK_PLAN_PERIODICIDAD FOREIGN KEY (periodicidad) REFERENCES PERIODICIDAD (codigo),
    CONSTRAINT CK_PLAN_DESCUENTO CHECK (descuento_pct BETWEEN 0 AND 30),
    CONSTRAINT CK_PLAN_ACTIVO CHECK (activo IN (0, 1))
);
COMMENT ON TABLE PLAN IS 'Planes de suscripción (canastas periódicas) que se ofrecen a los clientes.';
COMMENT ON COLUMN PLAN.id_plan IS 'Identificador del plan.';
COMMENT ON COLUMN PLAN.nombre IS 'Nombre comercial del plan.';
COMMENT ON COLUMN PLAN.descripcion IS 'Qué incluye el plan.';
COMMENT ON COLUMN PLAN.periodicidad IS 'Frecuencia del plan.';
COMMENT ON COLUMN PLAN.precio IS 'Precio de cada periodo en pesos.';
COMMENT ON COLUMN PLAN.kg_incluidos IS 'Kilos de fruta incluidos en cada periodo.';
COMMENT ON COLUMN PLAN.descuento_pct IS 'Descuento que recibe el suscriptor en sus pedidos adicionales.';
COMMENT ON COLUMN PLAN.activo IS 'Indica si el plan se sigue ofreciendo.';

CREATE TABLE METODO_PAGO (
    id_metodo                 NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    nombre                    VARCHAR2(40)   NOT NULL,
    tipo                      VARCHAR2(20)   NOT NULL,
    comision_pct              NUMBER(5,2)    DEFAULT 0 NOT NULL,
    en_linea                  NUMBER(1)      DEFAULT 1 NOT NULL,
    activo                    NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_METODO_PAGO PRIMARY KEY (id_metodo),
    CONSTRAINT UK_METODO_NOMBRE UNIQUE (nombre),
    CONSTRAINT FK_METODO_TIPO FOREIGN KEY (tipo) REFERENCES TIPO_METODO_PAGO (codigo),
    CONSTRAINT CK_METODO_COMISION CHECK (comision_pct BETWEEN 0 AND 10),
    CONSTRAINT CK_METODO_PAG_EN_LINEA CHECK (en_linea IN (0, 1)),
    CONSTRAINT CK_METODO_PAG_ACTIVO CHECK (activo IN (0, 1))
);
COMMENT ON TABLE METODO_PAGO IS 'Medios de pago aceptados, con la comisión que cobra la pasarela.';
COMMENT ON COLUMN METODO_PAGO.id_metodo IS 'Identificador del medio de pago.';
COMMENT ON COLUMN METODO_PAGO.nombre IS 'Nombre del medio (p. ej. Nequi).';
COMMENT ON COLUMN METODO_PAGO.tipo IS 'Tipo de medio de pago.';
COMMENT ON COLUMN METODO_PAGO.comision_pct IS 'Comisión que cobra la pasarela sobre cada pago.';
COMMENT ON COLUMN METODO_PAGO.en_linea IS 'Indica si se paga en línea (o contra entrega).';
COMMENT ON COLUMN METODO_PAGO.activo IS 'Indica si se acepta actualmente.';

CREATE TABLE SUSCRIPCION (
    id_suscripcion            NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_cliente                NUMBER(10)     NOT NULL,
    id_plan                   NUMBER(10)     NOT NULL,
    fecha_inicio              DATE           NOT NULL,
    fecha_fin                 DATE           NOT NULL,
    estado                    VARCHAR2(12)   NOT NULL,
    renovacion_automatica     NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_SUSCRIPCION PRIMARY KEY (id_suscripcion),
    CONSTRAINT FK_SUSCRIPCION_CLIENTE FOREIGN KEY (id_cliente) REFERENCES CLIENTE (id_cliente),
    CONSTRAINT FK_SUSCRIPCION_PLAN FOREIGN KEY (id_plan) REFERENCES PLAN (id_plan),
    CONSTRAINT FK_SUSCRIPCION_ESTADO FOREIGN KEY (estado) REFERENCES ESTADO_SUSCRIPCION (codigo),
    CONSTRAINT CK_SUSCRIPCION_FECHAS CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT CK_SUSCRIPCIO_RENOVACION_AUTOM CHECK (renovacion_automatica IN (0, 1))
);
COMMENT ON TABLE SUSCRIPCION IS 'Contratación de un plan por parte de un cliente durante un periodo.';
COMMENT ON COLUMN SUSCRIPCION.id_suscripcion IS 'Identificador de la suscripción.';
COMMENT ON COLUMN SUSCRIPCION.id_cliente IS 'Cliente que contrató el plan.';
COMMENT ON COLUMN SUSCRIPCION.id_plan IS 'Plan contratado.';
COMMENT ON COLUMN SUSCRIPCION.fecha_inicio IS 'Inicio del periodo contratado.';
COMMENT ON COLUMN SUSCRIPCION.fecha_fin IS 'Fin del periodo contratado.';
COMMENT ON COLUMN SUSCRIPCION.estado IS 'Estado de la suscripción.';
COMMENT ON COLUMN SUSCRIPCION.renovacion_automatica IS 'Indica si se renueva sola al terminar el periodo.';

CREATE TABLE LOTE (
    id_lote                   NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    codigo                    VARCHAR2(20)   NOT NULL,
    id_finca                  NUMBER(10)     NOT NULL,
    id_producto               NUMBER(10)     NOT NULL,
    id_punto                  NUMBER(10)     NOT NULL,
    fecha_cosecha             DATE           NOT NULL,
    fecha_vencimiento         DATE           NOT NULL,
    cantidad_kg               NUMBER(10,1)   NOT NULL,
    cantidad_disponible_kg    NUMBER(10,1)   NOT NULL,
    precio_finca_kg           NUMBER(12)     NOT NULL,
    calidad                   VARCHAR2(10)   NOT NULL,
    estado                    VARCHAR2(12)   DEFAULT 'DISPONIBLE' NOT NULL,
    CONSTRAINT PK_LOTE PRIMARY KEY (id_lote),
    CONSTRAINT UK_LOTE_CODIGO UNIQUE (codigo),
    CONSTRAINT FK_LOTE_FINCA FOREIGN KEY (id_finca) REFERENCES FINCA (id_finca),
    CONSTRAINT FK_LOTE_PRODUCTO FOREIGN KEY (id_producto) REFERENCES PRODUCTO (id_producto),
    CONSTRAINT FK_LOTE_PUNTO FOREIGN KEY (id_punto) REFERENCES PUNTO_ACOPIO (id_punto),
    CONSTRAINT CK_LOTE_CANTIDAD CHECK (cantidad_kg > 0 AND cantidad_disponible_kg BETWEEN 0 AND cantidad_kg),
    CONSTRAINT CK_LOTE_FECHAS CHECK (fecha_vencimiento > fecha_cosecha),
    CONSTRAINT CK_LOTE_CALIDAD CHECK (calidad IN ('EXTRA', 'PRIMERA', 'SEGUNDA')),
    CONSTRAINT CK_LOTE_ESTADO CHECK (estado IN ('DISPONIBLE', 'AGOTADO', 'VENCIDO'))
);
CREATE INDEX IX_LOTE_PRODUCTO_ESTADO ON LOTE (id_producto, estado, fecha_vencimiento);
COMMENT ON TABLE LOTE IS 'Cosecha de un producto entregada por una finca en un punto de acopio; lleva el inventario disponible.';
COMMENT ON COLUMN LOTE.id_lote IS 'Identificador del lote.';
COMMENT ON COLUMN LOTE.codigo IS 'Código de trazabilidad impreso en las canastillas.';
COMMENT ON COLUMN LOTE.id_finca IS 'Finca de donde viene la cosecha.';
COMMENT ON COLUMN LOTE.id_producto IS 'Producto cosechado.';
COMMENT ON COLUMN LOTE.id_punto IS 'Punto de acopio donde se recibió y se almacena.';
COMMENT ON COLUMN LOTE.fecha_cosecha IS 'Fecha de cosecha.';
COMMENT ON COLUMN LOTE.fecha_vencimiento IS 'Fecha hasta la que se puede vender (cosecha + vida útil).';
COMMENT ON COLUMN LOTE.cantidad_kg IS 'Kilos recibidos.';
COMMENT ON COLUMN LOTE.cantidad_disponible_kg IS 'Kilos que quedan para vender; los triggers lo actualizan al vender o cancelar.';
COMMENT ON COLUMN LOTE.precio_finca_kg IS 'Precio por kilo que se le paga al campesino.';
COMMENT ON COLUMN LOTE.calidad IS 'Clasificación de calidad (EXTRA, PRIMERA o SEGUNDA).';
COMMENT ON COLUMN LOTE.estado IS 'Estado del lote (DISPONIBLE, AGOTADO o VENCIDO).';

CREATE TABLE PEDIDO (
    id_pedido                 NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_cliente                NUMBER(10)     NOT NULL,
    id_punto                  NUMBER(10)     NOT NULL,
    id_suscripcion            NUMBER(10),
    fecha_pedido              TIMESTAMP      NOT NULL,
    fecha_entrega_programada  DATE           NOT NULL,
    direccion_entrega         VARCHAR2(150)  NOT NULL,
    estado                    VARCHAR2(12)   DEFAULT 'PENDIENTE' NOT NULL,
    subtotal                  NUMBER(14)     DEFAULT 0 NOT NULL,
    descuento                 NUMBER(14)     DEFAULT 0 NOT NULL,
    iva                       NUMBER(14)     DEFAULT 0 NOT NULL,
    total                     NUMBER(14)     DEFAULT 0 NOT NULL,
    observaciones             VARCHAR2(300),
    CONSTRAINT PK_PEDIDO PRIMARY KEY (id_pedido),
    CONSTRAINT FK_PEDIDO_CLIENTE FOREIGN KEY (id_cliente) REFERENCES CLIENTE (id_cliente),
    CONSTRAINT FK_PEDIDO_PUNTO FOREIGN KEY (id_punto) REFERENCES PUNTO_ACOPIO (id_punto),
    CONSTRAINT FK_PEDIDO_SUSCRIPCION FOREIGN KEY (id_suscripcion) REFERENCES SUSCRIPCION (id_suscripcion) ON DELETE SET NULL,
    CONSTRAINT FK_PEDIDO_ESTADO FOREIGN KEY (estado) REFERENCES ESTADO_PEDIDO (codigo),
    CONSTRAINT CK_PEDIDO_TOTALES CHECK (subtotal >= 0 AND descuento >= 0 AND iva >= 0 AND total >= 0)
);
CREATE INDEX IX_PEDIDO_FECHA ON PEDIDO (fecha_pedido);
COMMENT ON TABLE PEDIDO IS 'Compra de un cliente atendida desde un punto de acopio (transacción principal del negocio).';
COMMENT ON COLUMN PEDIDO.id_pedido IS 'Número del pedido.';
COMMENT ON COLUMN PEDIDO.id_cliente IS 'Cliente que compra.';
COMMENT ON COLUMN PEDIDO.id_punto IS 'Punto de acopio que atiende el pedido.';
COMMENT ON COLUMN PEDIDO.id_suscripcion IS 'Suscripción cuyo descuento se aplicó (si la hay).';
COMMENT ON COLUMN PEDIDO.fecha_pedido IS 'Fecha y hora en que se creó el pedido.';
COMMENT ON COLUMN PEDIDO.fecha_entrega_programada IS 'Fecha acordada para la entrega.';
COMMENT ON COLUMN PEDIDO.direccion_entrega IS 'Dirección donde se entrega.';
COMMENT ON COLUMN PEDIDO.estado IS 'Estado del pedido en su flujo.';
COMMENT ON COLUMN PEDIDO.subtotal IS 'Suma de las líneas antes de descuento e IVA.';
COMMENT ON COLUMN PEDIDO.descuento IS 'Descuento del plan de suscripción.';
COMMENT ON COLUMN PEDIDO.iva IS 'IVA de los productos gravados.';
COMMENT ON COLUMN PEDIDO.total IS 'Valor a pagar (subtotal − descuento + IVA).';
COMMENT ON COLUMN PEDIDO.observaciones IS 'Indicaciones del cliente para la entrega.';

CREATE TABLE DETALLE_PEDIDO (
    id_pedido                 NUMBER(10)     NOT NULL,
    linea                     NUMBER(10)     NOT NULL,
    id_lote                   NUMBER(10)     NOT NULL,
    cantidad_kg               NUMBER(10,1)   NOT NULL,
    precio_kg                 NUMBER(12)     NOT NULL,
    iva_pct                   NUMBER(5,2)    DEFAULT 0 NOT NULL,
    subtotal                  NUMBER(14)     NOT NULL,
    CONSTRAINT PK_DETALLE_PEDIDO PRIMARY KEY (id_pedido, linea),
    CONSTRAINT FK_DETALLE_PEDIDO FOREIGN KEY (id_pedido) REFERENCES PEDIDO (id_pedido) ON DELETE CASCADE,
    CONSTRAINT FK_DETALLE_LOTE FOREIGN KEY (id_lote) REFERENCES LOTE (id_lote),
    CONSTRAINT CK_DETALLE_CANTIDAD CHECK (cantidad_kg > 0)
);
COMMENT ON TABLE DETALLE_PEDIDO IS 'Líneas de un pedido: qué lote se vende, cuántos kilos y a qué precio (entidad débil).';
COMMENT ON COLUMN DETALLE_PEDIDO.id_pedido IS 'Pedido al que pertenece la línea (parte de la llave).';
COMMENT ON COLUMN DETALLE_PEDIDO.linea IS 'Número de la línea dentro del pedido (llave parcial).';
COMMENT ON COLUMN DETALLE_PEDIDO.id_lote IS 'Lote del que sale el producto (el que vence primero).';
COMMENT ON COLUMN DETALLE_PEDIDO.cantidad_kg IS 'Kilos vendidos en la línea.';
COMMENT ON COLUMN DETALLE_PEDIDO.precio_kg IS 'Precio por kilo al momento de la venta.';
COMMENT ON COLUMN DETALLE_PEDIDO.iva_pct IS 'Porcentaje de IVA de la línea.';
COMMENT ON COLUMN DETALLE_PEDIDO.subtotal IS 'Cantidad × precio de la línea.';

CREATE TABLE PAGO (
    id_pago                   NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_metodo                 NUMBER(10)     NOT NULL,
    id_pedido                 NUMBER(10),
    id_suscripcion            NUMBER(10),
    fecha_pago                TIMESTAMP      NOT NULL,
    valor                     NUMBER(14)     NOT NULL,
    referencia                VARCHAR2(30)   NOT NULL,
    estado                    VARCHAR2(12)   NOT NULL,
    CONSTRAINT PK_PAGO PRIMARY KEY (id_pago),
    CONSTRAINT UK_PAGO_REFERENCIA UNIQUE (referencia),
    CONSTRAINT FK_PAGO_METODO FOREIGN KEY (id_metodo) REFERENCES METODO_PAGO (id_metodo),
    CONSTRAINT FK_PAGO_PEDIDO FOREIGN KEY (id_pedido) REFERENCES PEDIDO (id_pedido) ON DELETE CASCADE,
    CONSTRAINT FK_PAGO_SUSCRIPCION FOREIGN KEY (id_suscripcion) REFERENCES SUSCRIPCION (id_suscripcion) ON DELETE CASCADE,
    CONSTRAINT CK_PAGO_VALOR CHECK (valor > 0),
    CONSTRAINT FK_PAGO_ESTADO FOREIGN KEY (estado) REFERENCES ESTADO_PAGO (codigo),
    CONSTRAINT CK_PAGO_ORIGEN CHECK ((id_pedido IS NULL AND id_suscripcion IS NOT NULL) OR (id_pedido IS NOT NULL AND id_suscripcion IS NULL))
);
COMMENT ON TABLE PAGO IS 'Pagos en línea de un pedido o de una suscripción.';
COMMENT ON COLUMN PAGO.id_pago IS 'Identificador del pago.';
COMMENT ON COLUMN PAGO.id_metodo IS 'Medio con el que se pagó.';
COMMENT ON COLUMN PAGO.id_pedido IS 'Pedido que se paga (si es pago de pedido).';
COMMENT ON COLUMN PAGO.id_suscripcion IS 'Suscripción que se paga (si es pago de plan).';
COMMENT ON COLUMN PAGO.fecha_pago IS 'Fecha y hora de la transacción.';
COMMENT ON COLUMN PAGO.valor IS 'Valor pagado en pesos.';
COMMENT ON COLUMN PAGO.referencia IS 'Referencia única que entrega la pasarela.';
COMMENT ON COLUMN PAGO.estado IS 'Resultado del pago.';

CREATE TABLE FACTURA (
    id_factura                NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_pedido                 NUMBER(10)     NOT NULL,
    numero                    VARCHAR2(20)   NOT NULL,
    fecha_emision             TIMESTAMP      NOT NULL,
    subtotal                  NUMBER(14)     NOT NULL,
    descuento                 NUMBER(14)     NOT NULL,
    iva                       NUMBER(14)     NOT NULL,
    total                     NUMBER(14)     NOT NULL,
    CONSTRAINT PK_FACTURA PRIMARY KEY (id_factura),
    CONSTRAINT UK_FACTURA_NUMERO UNIQUE (numero),
    CONSTRAINT UK_FACTURA_PEDIDO UNIQUE (id_pedido),
    CONSTRAINT FK_FACTURA_PEDIDO FOREIGN KEY (id_pedido) REFERENCES PEDIDO (id_pedido) ON DELETE CASCADE
);
COMMENT ON TABLE FACTURA IS 'Factura electrónica de venta emitida al aprobarse el pago de un pedido.';
COMMENT ON COLUMN FACTURA.id_factura IS 'Identificador de la factura.';
COMMENT ON COLUMN FACTURA.id_pedido IS 'Pedido facturado (una factura por pedido).';
COMMENT ON COLUMN FACTURA.numero IS 'Número consecutivo de facturación electrónica (FM-…).';
COMMENT ON COLUMN FACTURA.fecha_emision IS 'Fecha y hora de emisión.';
COMMENT ON COLUMN FACTURA.subtotal IS 'Subtotal facturado.';
COMMENT ON COLUMN FACTURA.descuento IS 'Descuento facturado.';
COMMENT ON COLUMN FACTURA.iva IS 'IVA facturado.';
COMMENT ON COLUMN FACTURA.total IS 'Total de la factura.';

CREATE TABLE DESPACHO (
    id_despacho               NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_pedido                 NUMBER(10)     NOT NULL,
    id_transportador          NUMBER(10)     NOT NULL,
    numero_guia               VARCHAR2(20)   NOT NULL,
    fecha_salida              TIMESTAMP      NOT NULL,
    fecha_entrega             TIMESTAMP,
    estado                    VARCHAR2(12)   NOT NULL,
    temperatura_c             NUMBER(4,1),
    observaciones             VARCHAR2(300),
    CONSTRAINT PK_DESPACHO PRIMARY KEY (id_despacho),
    CONSTRAINT UK_DESPACHO_GUIA UNIQUE (numero_guia),
    CONSTRAINT UK_DESPACHO_PEDIDO UNIQUE (id_pedido),
    CONSTRAINT FK_DESPACHO_PEDIDO FOREIGN KEY (id_pedido) REFERENCES PEDIDO (id_pedido) ON DELETE CASCADE,
    CONSTRAINT FK_DESPACHO_TRANSPORTADOR FOREIGN KEY (id_transportador) REFERENCES TRANSPORTADOR (id_transportador),
    CONSTRAINT FK_DESPACHO_ESTADO FOREIGN KEY (estado) REFERENCES ESTADO_DESPACHO (codigo)
);
COMMENT ON TABLE DESPACHO IS 'Envío de un pedido con un transportador (guía de despacho).';
COMMENT ON COLUMN DESPACHO.id_despacho IS 'Identificador del despacho.';
COMMENT ON COLUMN DESPACHO.id_pedido IS 'Pedido despachado (un despacho por pedido).';
COMMENT ON COLUMN DESPACHO.id_transportador IS 'Transportador que lleva el pedido.';
COMMENT ON COLUMN DESPACHO.numero_guia IS 'Número de la guía de despacho (GD-…).';
COMMENT ON COLUMN DESPACHO.fecha_salida IS 'Fecha y hora de salida del punto de acopio.';
COMMENT ON COLUMN DESPACHO.fecha_entrega IS 'Fecha y hora de entrega al cliente.';
COMMENT ON COLUMN DESPACHO.estado IS 'Estado del despacho.';
COMMENT ON COLUMN DESPACHO.temperatura_c IS 'Temperatura de la carga refrigerada al salir, en °C.';
COMMENT ON COLUMN DESPACHO.observaciones IS 'Novedades de la entrega.';

CREATE TABLE NOTIFICACION (
    id_notificacion           NUMBER(10)     GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL,
    id_usuario                NUMBER(10)     NOT NULL,
    tipo                      VARCHAR2(15)   NOT NULL,
    titulo                    VARCHAR2(100)  NOT NULL,
    mensaje                   VARCHAR2(400)  NOT NULL,
    fecha_programada          TIMESTAMP      NOT NULL,
    leida                     NUMBER(1)      DEFAULT 0 NOT NULL,
    fecha_creacion            TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_NOTIFICACION PRIMARY KEY (id_notificacion),
    CONSTRAINT FK_NOTIFICACION_USUARIO FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
    CONSTRAINT FK_NOTIFICACION_TIPO FOREIGN KEY (tipo) REFERENCES TIPO_NOTIFICACION (codigo),
    CONSTRAINT CK_NOTIFICACI_LEIDA CHECK (leida IN (0, 1))
);
CREATE INDEX IX_NOTIFICACION_USUARIO ON NOTIFICACION (id_usuario, leida, fecha_programada);
COMMENT ON TABLE NOTIFICACION IS 'Avisos y recordatorios personalizados que recibe cada usuario.';
COMMENT ON COLUMN NOTIFICACION.id_notificacion IS 'Identificador de la notificación.';
COMMENT ON COLUMN NOTIFICACION.id_usuario IS 'Usuario que la recibe.';
COMMENT ON COLUMN NOTIFICACION.tipo IS 'Tipo de notificación.';
COMMENT ON COLUMN NOTIFICACION.titulo IS 'Título corto del aviso.';
COMMENT ON COLUMN NOTIFICACION.mensaje IS 'Texto completo del aviso o recordatorio.';
COMMENT ON COLUMN NOTIFICACION.fecha_programada IS 'Fecha y hora a partir de la cual se muestra (recordatorios futuros).';
COMMENT ON COLUMN NOTIFICACION.leida IS 'Indica si el usuario ya la leyó.';
COMMENT ON COLUMN NOTIFICACION.fecha_creacion IS 'Fecha y hora en que se creó.';
