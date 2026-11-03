-- =====================================================================
-- Usuario de la aplicacion (no se usa root desde el programa).
-- Ejecutar como root DESPUES de 01_esquema.sql. Cambie la clave y pongala en app/.env
-- =====================================================================
CREATE USER IF NOT EXISTS 'frutimarket_app'@'localhost' IDENTIFIED BY 'FrutiMarket2026*';
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON frutimarket_bd.* TO 'frutimarket_app'@'localhost';
FLUSH PRIVILEGES;
