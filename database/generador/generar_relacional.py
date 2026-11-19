"""Diagramas del modelo relacional a partir de database/01_esquema.sql (Graphviz).

Genera en docs/relacional/:
  relacional-general.svg     solo tablas y relaciones
  relacional-completo.svg    todas las tablas con columnas, llaves y relaciones
  subvista-<area>.svg        una subvista por area del negocio (tablas del area completas y
                             las tablas de otras areas que referencian, en gris)
Uso: python3 generar_relacional.py   (requiere el comando dot de Graphviz)
"""
import re
import subprocess
from pathlib import Path

raiz = Path(__file__).resolve().parent.parent.parent
sql = (raiz / "database" / "01_esquema.sql").read_text(encoding="utf-8")
salida = raiz / "docs" / "relacional"
salida.mkdir(parents=True, exist_ok=True)

AREAS = {
    "seguridad": ("Seguridad y notificaciones", "#e3f2fd", "#1565c0",
                  ["rol", "usuario", "notificacion", "tipo_notificacion"]),
    "produccion": ("Ubicación y producción", "#e8f5e9", "#2e7d32",
                   ["departamento", "municipio", "campesino", "finca", "categoria", "producto", "lote"]),
    "logistica": ("Logística y personal", "#fff3e0", "#e65100",
                  ["punto_acopio", "cargo", "empleado", "transportador", "despacho", "estado_despacho"]),
    "clientes": ("Clientes y suscripciones", "#f3e5f5", "#6a1b9a",
                 ["tipo_cliente", "cliente", "periodicidad", "plan", "estado_suscripcion", "suscripcion"]),
    "ventas": ("Ventas", "#fce4ec", "#ad1457", ["estado_pedido", "pedido", "detalle_pedido", "factura"]),
    "pagos": ("Pagos", "#e0f7fa", "#00838f", ["tipo_metodo_pago", "metodo_pago", "estado_pago", "pago"]),
}
area_de = {t: k for k, (_, _, _, ts) in AREAS.items() for t in ts}

# ------------------------------------------------------------------ lectura del esquema
tablas = {}
for m in re.finditer(r"CREATE TABLE (\w+) \((.*?)\n\) ENGINE", sql, re.S):
    nombre, cuerpo = m.group(1), m.group(2)
    cols, pk, fks, uks = [], [], [], []
    for linea in cuerpo.splitlines():
        linea = re.sub(r"\s+--.*$", "", linea).strip().rstrip(",")
        if not linea:
            continue
        if linea.startswith("CONSTRAINT"):
            if " PRIMARY KEY " in linea:
                pk = [c.strip() for c in re.search(r"PRIMARY KEY \(([^)]*)\)", linea).group(1).split(",")]
            elif " UNIQUE " in linea:
                uks.append([c.strip() for c in re.search(r"UNIQUE \(([^)]*)\)", linea).group(1).split(",")])
            elif " FOREIGN KEY " in linea:
                fm = re.search(r"FOREIGN KEY \((\w+)\) REFERENCES (\w+) \((\w+)\)", linea)
                fks.append((fm.group(1), fm.group(2), fm.group(3)))
            continue
        cm = re.match(r"(\w+)\s+(\w+(?:\([\d,]+\))?)\s*(.*)", linea)
        cols.append((cm.group(1), cm.group(2), "NOT NULL" in cm.group(3)))
    tablas[nombre] = dict(cols=cols, pk=pk, fks=fks, uks=uks)


def esc(t):
    return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def etiqueta(t, completa=True, gris=False, solo=None):
    d = tablas[t]
    _, fondo, borde, _ = AREAS[area_de[t]]
    if gris:
        fondo, borde = "#f2f2f2", "#9e9e9e"
    fkcols = {f[0] for f in d["fks"]}
    ukcols = {c for u in d["uks"] for c in u}
    h = [f'<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="3" COLOR="{borde}" BGCOLOR="white">',
         f'<TR><TD COLSPAN="3" BGCOLOR="{borde}"><FONT COLOR="white"><B>{t.upper()}</B></FONT></TD></TR>']
    if not completa:
        h.append(f'<TR><TD COLSPAN="3" BGCOLOR="{fondo}"><FONT POINT-SIZE="9">{len(d["cols"])} columnas</FONT></TD></TR>')
    else:
        for c, tipo, nn in d["cols"]:
            if solo is not None and c not in solo:
                continue
            marcas = []
            if c in d["pk"]:
                marcas.append("PK")
            if c in fkcols:
                marcas.append("FK")
            if c in ukcols and c not in d["pk"]:
                marcas.append("UK")
            m = ",".join(marcas)
            nombre = f"<B><U>{c}</U></B>" if c in d["pk"] else c
            color = "#b71c1c" if "PK" in marcas else ("#0d47a1" if "FK" in marcas else "#212121")
            h.append(f'<TR><TD ALIGN="LEFT" PORT="{c}_i" BGCOLOR="{fondo}">{f'<FONT POINT-SIZE="8" COLOR="{color}">{m}</FONT>' if m else ' '}</TD>'
                     f'<TD ALIGN="LEFT" BGCOLOR="{fondo}"><FONT COLOR="{color}">{nombre}{" *" if nn else ""}</FONT></TD>'
                     f'<TD ALIGN="LEFT" PORT="{c}_o" BGCOLOR="{fondo}"><FONT POINT-SIZE="8" COLOR="#555555">{tipo.upper()}</FONT></TD></TR>')
    h.append("</TABLE>")
    return "<" + "".join(h) + ">"


def arista(t, fk, completa):
    col, ref, refcol = fk
    nn = next(n for c, _, n in tablas[t]["cols"] if c == col)
    # muchos (pata de gallo) del lado hijo; uno (|) del lado padre; o = opcional
    unico = any(u == [col] for u in tablas[t]["uks"])   # FK unica: relacion 1 a 1
    cola = ("teetee" if nn else "teeodot") if unico else ("crow" if nn else "crowodot")
    if completa:
        return f'"{t}":"{col}_o":e -> "{ref}":"{refcol}_i":w [arrowtail={cola}, arrowhead=teetee, dir=both]'
    return f'"{t}" -> "{ref}" [arrowtail={cola}, arrowhead=teetee, dir=both]'


def dibujar(nombre, titulo, incluidas, completa, externas=(), rankdir="LR", clusters=True):
    g = [f'digraph "{nombre}" {{', f'graph [rankdir={rankdir}, fontname="DejaVu Sans", fontsize=20, labelloc=t, '
         f'label=<<B>{esc(titulo)}</B>>, splines=true, nodesep=0.35, ranksep={1.1 if completa else 0.8}, pad=0.3, bgcolor="white"];',
         'node [shape=plaintext, fontname="DejaVu Sans", fontsize=10];',
         'edge [color="#546e7a", penwidth=1.1, arrowsize=0.8];']
    if clusters:
        for k, (tit, fondo, borde, ts) in AREAS.items():
            sel = [t for t in ts if t in incluidas]
            if not sel:
                continue
            g.append(f'subgraph "cluster_{k}" {{ label=<<B>{esc(tit)}</B>>; fontsize=14; style="rounded,dashed"; color="{borde}"; fontcolor="{borde}";')
            for t in sel:
                g.append(f'"{t}" [label={etiqueta(t, completa)}];')
            g.append("}")
    else:
        for t in incluidas:
            g.append(f'"{t}" [label={etiqueta(t, completa)}];')
    for t in externas:
        usadas = set(tablas[t]["pk"]) | {fk[0] for fk in tablas[t]["fks"] if fk[1] in incluidas}
        g.append(f'"{t}" [label={etiqueta(t, completa, gris=True, solo=usadas)}];')
    todas = set(incluidas) | set(externas)
    for t in incluidas:
        for fk in tablas[t]["fks"]:
            if fk[1] in todas:
                g.append(arista(t, fk, completa))
    for t in externas:
        for fk in tablas[t]["fks"]:
            if fk[1] in incluidas:
                g.append(arista(t, fk, completa))
    g.append("}")
    dot = salida / f"{nombre}.dot"
    dot.write_text("\n".join(g), encoding="utf-8")
    subprocess.run(["dot", "-Tsvg", str(dot), "-o", str(salida / f"{nombre}.svg")], check=True, stderr=subprocess.DEVNULL)
    subprocess.run(["dot", "-Tpng", "-Gdpi=110", str(dot), "-o", str(salida / f"{nombre}.png")], check=True, stderr=subprocess.DEVNULL)
    dot.unlink()
    print(nombre)


todas = list(tablas)
dibujar("relacional-general", "FrutiMarket BD · Modelo relacional (tablas y relaciones)", todas, False)
dibujar("relacional-completo", "FrutiMarket BD · Modelo relacional completo", todas, True)
for k, (tit, _, _, ts) in AREAS.items():
    ext = sorted({fk[1] for t in ts for fk in tablas[t]["fks"] if fk[1] not in ts}
                 | {t2 for t2 in tablas if t2 not in ts and any(fk[1] in ts for fk in tablas[t2]["fks"])})
    dibujar(f"subvista-{k}", f"Subvista: {tit}", ts, True, externas=ext, clusters=False)
