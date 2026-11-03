#!/usr/bin/env bash
# Crea la base frutimarket_bd con esquema, triggers, vistas, datos y usuario de la app.
# Uso: ./database/instalar.sh          (pide la clave de root de MariaDB)
set -e
cd "$(dirname "$0")"
read -rsp "Clave de root de MariaDB: " CLAVE; echo
for f in 01_esquema.sql 02_objetos.sql 03_datos.sql 00_usuario_app.sql; do
  echo "-> $f"
  mariadb -u root -p"$CLAVE" --default-character-set=utf8mb4 < "$f"
done
mariadb -u root -p"$CLAVE" frutimarket_bd -e "SELECT COUNT(*) AS pedidos FROM pedido; SELECT COUNT(*) AS lotes FROM lote;"
echo "Listo. Base frutimarket_bd instalada."
