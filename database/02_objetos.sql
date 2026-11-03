-- =====================================================================
-- Objetos de base de datos: triggers y vistas.
-- Ejecutar despues de 01_esquema.sql (y antes de 03_datos.sql, porque los
-- triggers mantienen el inventario mientras se cargan los pedidos).
-- =====================================================================
USE frutimarket_bd;

DELIMITER $$

-- Al vender, se descuenta del inventario del lote (si el pedido no esta cancelado).
CREATE TRIGGER trg_detalle_descontar_inventario
BEFORE INSERT ON detalle_pedido
FOR EACH ROW
BEGIN
    DECLARE v_disponible DECIMAL(10,1);
    DECLARE v_estado_pedido VARCHAR(12);

    SELECT estado INTO v_estado_pedido FROM pedido WHERE id_pedido = NEW.id_pedido;
    SELECT cantidad_disponible_kg INTO v_disponible FROM lote WHERE id_lote = NEW.id_lote FOR UPDATE;

    IF v_estado_pedido <> 'CANCELADO' THEN
        IF v_disponible < NEW.cantidad_kg THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Inventario insuficiente en el lote';
        END IF;
        UPDATE lote
           -- estado va primero: MariaDB evalua las asignaciones en orden y la segunda
           -- ya veria la cantidad descontada
           SET estado = IF(cantidad_disponible_kg - NEW.cantidad_kg = 0, 'AGOTADO', estado),
               cantidad_disponible_kg = cantidad_disponible_kg - NEW.cantidad_kg
         WHERE id_lote = NEW.id_lote;
    END IF;
END$$

-- Si se elimina una linea de un pedido vigente, el producto vuelve al lote.
CREATE TRIGGER trg_detalle_devolver_inventario
AFTER DELETE ON detalle_pedido
FOR EACH ROW
BEGIN
    IF (SELECT estado FROM pedido WHERE id_pedido = OLD.id_pedido) <> 'CANCELADO' THEN
        UPDATE lote
           SET cantidad_disponible_kg = cantidad_disponible_kg + OLD.cantidad_kg,
               estado = IF(estado = 'AGOTADO', 'DISPONIBLE', estado)
         WHERE id_lote = OLD.id_lote;
    END IF;
END$$

-- Cancelar un pedido devuelve todo su producto al inventario.
CREATE TRIGGER trg_pedido_cancelar
AFTER UPDATE ON pedido
FOR EACH ROW
BEGIN
    IF NEW.estado = 'CANCELADO' AND OLD.estado <> 'CANCELADO' THEN
        UPDATE lote l
          JOIN detalle_pedido d ON d.id_lote = l.id_lote
           SET l.cantidad_disponible_kg = l.cantidad_disponible_kg + d.cantidad_kg,
               l.estado = IF(l.estado = 'AGOTADO', 'DISPONIBLE', l.estado)
         WHERE d.id_pedido = NEW.id_pedido;
    END IF;
END$$

-- Eliminar un pedido vigente tambien devuelve el inventario (antes del borrado en cascada).
CREATE TRIGGER trg_pedido_eliminar
BEFORE DELETE ON pedido
FOR EACH ROW
BEGIN
    IF OLD.estado <> 'CANCELADO' THEN
        UPDATE lote l
          JOIN detalle_pedido d ON d.id_lote = l.id_lote
           SET l.cantidad_disponible_kg = l.cantidad_disponible_kg + d.cantidad_kg,
               l.estado = IF(l.estado = 'AGOTADO', 'DISPONIBLE', l.estado)
         WHERE d.id_pedido = OLD.id_pedido;
        -- el borrado en cascada de detalle_pedido no dispara triggers en MariaDB,
        -- por eso la devolucion se hace aqui y no en trg_detalle_devolver_inventario
    END IF;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------
-- Vistas de apoyo
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_ventas_detalladas AS
SELECT p.id_pedido,
       p.fecha_pedido,
       p.estado,
       c.id_cliente,
       c.nombre            AS cliente,
       c.tipo_cliente,
       pa.nombre           AS punto_acopio,
       pr.id_producto,
       pr.nombre           AS producto,
       cat.nombre          AS categoria,
       l.codigo            AS lote,
       f.nombre            AS finca,
       CONCAT(ca.nombres, ' ', ca.apellidos) AS campesino,
       d.cantidad_kg,
       d.precio_kg,
       l.precio_finca_kg,
       d.subtotal
  FROM pedido p
  JOIN cliente c         ON c.id_cliente = p.id_cliente
  JOIN punto_acopio pa   ON pa.id_punto = p.id_punto
  JOIN detalle_pedido d  ON d.id_pedido = p.id_pedido
  JOIN lote l            ON l.id_lote = d.id_lote
  JOIN producto pr       ON pr.id_producto = l.id_producto
  JOIN categoria cat     ON cat.id_categoria = pr.id_categoria
  JOIN finca f           ON f.id_finca = l.id_finca
  JOIN campesino ca      ON ca.id_campesino = f.id_campesino;

CREATE OR REPLACE VIEW v_inventario_disponible AS
SELECT pr.id_producto,
       pr.nombre AS producto,
       pr.variedad,
       pr.precio_venta_kg,
       SUM(l.cantidad_disponible_kg) AS disponible_kg,
       MIN(l.fecha_vencimiento)      AS proximo_vencimiento
  FROM producto pr
  JOIN lote l ON l.id_producto = pr.id_producto
 WHERE l.estado = 'DISPONIBLE'
   AND l.cantidad_disponible_kg > 0
   AND l.fecha_vencimiento >= CURDATE()
 GROUP BY pr.id_producto, pr.nombre, pr.variedad, pr.precio_venta_kg;
