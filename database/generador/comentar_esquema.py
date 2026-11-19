"""Agrega COMMENT a tablas y columnas de 01_esquema.sql a partir de diccionario.py (idempotente)."""
import re
from pathlib import Path
from diccionario import TABLAS, C

ruta = Path(__file__).resolve().parent.parent / "01_esquema.sql"
out, tabla = [], None
TIPOS = r"(INT|VARCHAR|CHAR|DECIMAL|TINYINT|DATE|DATETIME)\b"
for linea in ruta.read_text(encoding="utf-8").splitlines():
    m = re.match(r"CREATE TABLE (\w+) \(", linea)
    if m:
        tabla = m.group(1)
    m = re.match(r"^(    (\w+)\s+" + TIPOS + r".*?)(,?)(\s*--.*)?$", linea)
    if tabla and m and "COMMENT" not in linea and m.group(2) in C[tabla]:
        texto = C[tabla][m.group(2)].replace("'", "''")
        linea = f"{m.group(1).rstrip()} COMMENT '{texto}'{m.group(4)}" + (f"  {m.group(5).strip()}" if m.group(5) else "")
    if linea.startswith(") ENGINE = InnoDB;") and tabla:
        linea = f") ENGINE = InnoDB COMMENT = '{TABLAS[tabla]}';"
        tabla = None
    out.append(linea)
ruta.write_text("\n".join(out) + "\n", encoding="utf-8")
