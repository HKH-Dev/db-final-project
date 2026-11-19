"""Diccionario de datos en Markdown a partir de 01_esquema.sql y diccionario.py -> docs/diccionario-datos.md"""
import re
from pathlib import Path
from diccionario import TABLAS, C

raiz = Path(__file__).resolve().parent.parent.parent
sql = (raiz / "database" / "01_esquema.sql").read_text(encoding="utf-8")
ORDEN = ["departamento", "cargo", "tipo_cliente", "periodicidad", "tipo_metodo_pago", "tipo_notificacion",
         "estado_suscripcion", "estado_pedido", "estado_pago", "estado_despacho"]
out = ["# Diccionario de datos", "",
       "Una tabla por entidad de `frutimarket_bd` (MariaDB). Columnas: **Nulo** = admite NULL; **Único** = restricción UNIQUE "
       "(«compuesto» si es junto con otra columna); **Check** = restricción de comprobación o valor por defecto; "
       "**PK** = llave primaria; **FK** = llave foránea y tabla a la que referencia. Los tipos TINYINT(1) guardan 0 o 1 (falso o verdadero).", ""]
bloques = {}
for m in re.finditer(r"CREATE TABLE (\w+) \((.*?)\n\) ENGINE", sql, re.S):
    bloques[m.group(1)] = m.group(2)
negocio = [t for t in bloques if t not in ORDEN and t != "detalle_pedido"]
secuencia = negocio + ["detalle_pedido"] + ORDEN
for n, t in enumerate(secuencia, 1):
    cuerpo = bloques[t]
    cols, pk, uk, fk, ck = [], [], {}, {}, []
    for linea in cuerpo.splitlines():
        linea = re.sub(r"\s+--.*$", "", linea).strip().rstrip(",")
        if linea.startswith("CONSTRAINT"):
            if "PRIMARY KEY" in linea:
                pk = [c.strip() for c in re.search(r"PRIMARY KEY \(([^)]*)\)", linea).group(1).split(",")]
            elif " UNIQUE " in linea:
                cs = [c.strip() for c in re.search(r"UNIQUE \(([^)]*)\)", linea).group(1).split(",")]
                for c in cs:
                    uk[c] = "Sí" if len(cs) == 1 else "Compuesto (" + ", ".join(cs) + ")"
            elif "FOREIGN KEY" in linea:
                f = re.search(r"FOREIGN KEY \((\w+)\) REFERENCES (\w+) \((\w+)\)(.*)", linea)
                regla = f.group(4).strip().replace("ON DELETE ", "al borrar: ").replace("CASCADE", "cascada").replace("SET NULL", "pone NULL")
                fk[f.group(1)] = f"{f.group(2)}({f.group(3)})" + (f", {regla}" if regla else "")
            elif " CHECK " in linea:
                ck.append(re.search(r"CHECK \((.*)\)$", linea).group(1))
            continue
        if not linea:
            continue
        cm = re.match(r"(\w+)\s+(\w+(?:\([\d,]+\))?)\s*(.*)", linea)
        resto = re.sub(r"COMMENT '.*'$", "", cm.group(3))
        cols.append((cm.group(1), cm.group(2), "NOT NULL" not in resto, (re.search(r"DEFAULT (\S+)", resto) or [None, None])[1],
                     "AUTO_INCREMENT" in resto))
    out += [f"## {n}. {t}", "", f"**Finalidad:** {TABLAS[t]}", "",
            "| Atributo | Finalidad | Tipo | Nulo | Único | Check / por defecto | PK | FK (referencia) |",
            "| --- | --- | --- | --- | --- | --- | --- | --- |"]
    for c, tipo, nulo, defecto, auto in cols:
        chk = [k for k in ck if re.search(rf"\b{c}\b", k)]
        extra = []
        if chk:
            extra.append("; ".join(f"`{k}`" for k in chk))
        if tipo.upper().startswith("TINYINT"):
            extra.append("0 o 1")
        if defecto:
            extra.append(f"por defecto {defecto}")
        if auto:
            extra.append("autoincremental")
        out.append(f"| {c} | {C[t][c]} | {tipo.upper()} | {'Sí' if nulo else 'No'} | {uk.get(c, '')} | {'; '.join(extra)} | "
                   f"{'Sí' if c in pk else ''} | {fk.get(c, '')} |")
    out.append("")
(raiz / "docs" / "diccionario-datos.md").write_text("\n".join(out), encoding="utf-8")
print(len(secuencia), "tablas", len("\n".join(out)), "caracteres")
