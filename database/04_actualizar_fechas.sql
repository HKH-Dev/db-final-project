-- =====================================================================
-- Opcional: corre todas las fechas de los datos de ejemplo para que el
-- ultimo dia con ventas sea AYER. Util si sustenta el proyecto semanas
-- despues de cargar los datos (asi "lotes por vencer", "ventas del mes"
-- y "suscriptores inactivos" siguen mostrando informacion).
-- Uso:  mariadb -u root -p < database/04_actualizar_fechas.sql
-- =====================================================================
USE frutimarket_bd;

DROP PROCEDURE IF EXISTS sp_actualizar_fechas;
DELIMITER $$
CREATE PROCEDURE sp_actualizar_fechas()
BEGIN
    DECLARE v_dias INT;
    SELECT DATEDIFF(CURDATE(), DATE(MAX(fecha_pedido))) - 1 INTO v_dias FROM pedido;
    IF v_dias > 0 THEN
        START TRANSACTION;
        UPDATE usuario      SET fecha_registro = fecha_registro + INTERVAL v_dias DAY,
                                ultimo_acceso = ultimo_acceso + INTERVAL v_dias DAY;
        UPDATE campesino    SET fecha_vinculacion = fecha_vinculacion + INTERVAL v_dias DAY;
        UPDATE cliente      SET fecha_registro = fecha_registro + INTERVAL v_dias DAY;
        UPDATE suscripcion  SET fecha_inicio = fecha_inicio + INTERVAL v_dias DAY,
                                fecha_fin = fecha_fin + INTERVAL v_dias DAY;
        UPDATE lote         SET fecha_cosecha = fecha_cosecha + INTERVAL v_dias DAY,
                                fecha_vencimiento = fecha_vencimiento + INTERVAL v_dias DAY;
        UPDATE pedido       SET fecha_pedido = fecha_pedido + INTERVAL v_dias DAY,
                                fecha_entrega_programada = fecha_entrega_programada + INTERVAL v_dias DAY;
        UPDATE pago         SET fecha_pago = fecha_pago + INTERVAL v_dias DAY;
        UPDATE factura      SET fecha_emision = fecha_emision + INTERVAL v_dias DAY;
        UPDATE despacho     SET fecha_salida = fecha_salida + INTERVAL v_dias DAY,
                                fecha_entrega = fecha_entrega + INTERVAL v_dias DAY;
        UPDATE notificacion SET fecha_programada = fecha_programada + INTERVAL v_dias DAY,
                                fecha_creacion = fecha_creacion + INTERVAL v_dias DAY;
        UPDATE lote SET estado = 'VENCIDO' WHERE estado = 'DISPONIBLE' AND fecha_vencimiento < CURDATE();
        UPDATE suscripcion SET estado = 'VENCIDA' WHERE estado = 'ACTIVA' AND fecha_fin < CURDATE();
        COMMIT;
    END IF;
    SELECT v_dias AS dias_desplazados;
END$$
DELIMITER ;

CALL sp_actualizar_fechas();
