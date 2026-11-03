"""
Genera database/03_datos.sql con datos de ejemplo coherentes para FrutiMarket BD.

Los catalogos (municipios, productos, planes...) estan escritos a mano. Los movimientos
(lotes, pedidos, pagos, facturas, despachos, notificaciones) se simulan dia a dia desde
el 2 de enero hasta el 2 de noviembre de 2026, respetando el inventario: nunca se vende
mas de lo que hay en un lote ni un lote vencido. La semilla es fija, asi que cada
ejecucion produce exactamente el mismo archivo.

Uso:  python3 database/generador/generar_datos.py
"""
import random
import unicodedata
from datetime import date, datetime, timedelta
from pathlib import Path

random.seed(2026)
INICIO = date(2026, 1, 2)
FIN = date(2026, 11, 2)
HOY = date(2026, 11, 3)

# Hashes BCrypt (htpasswd -bnBC 10). Claves en el README.
HASH = {
    "ADMIN": "$2a$10$yJsCoonziC.aEAntDM6dyepVXAGctyhsBObRBheZKMbnJgKMtafsa",      # Admin2026*
    "OPERADOR": "$2a$10$tIEOUT/QmgULorvfSeaine67KwG0tKvztPEGaFCDjppzIWxbITcGi",   # Operador2026*
    "CAMPESINO": "$2a$10$b8Z9/9kawmUrrmRqUcrswenm14mSHxddhlX3Ib1xtA6Y5ZFQse59O",  # Campesino2026*
    "CLIENTE": "$2a$10$Iad0ea7VQpPpWMS1XTgOOe82m39FCiywEMaYsxdskdvRQfcF93yPq",    # Cliente2026*
}

out = []


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, datetime):
        return "'" + v.strftime("%Y-%m-%d %H:%M:%S") + "'"
    if isinstance(v, date):
        return "'" + v.isoformat() + "'"
    return "'" + str(v).replace("'", "''") + "'"


def insert(tabla, columnas, filas, lote=200):
    if not filas:
        return
    out.append(f"\n-- {tabla}: {len(filas)} registros")
    for i in range(0, len(filas), lote):
        parte = filas[i:i + lote]
        valores = ",\n".join("(" + ", ".join(q(v) for v in f) + ")" for f in parte)
        out.append(f"INSERT INTO {tabla} ({', '.join(columnas)}) VALUES\n{valores};")


def sin_tildes(t):
    return "".join(c for c in unicodedata.normalize("NFD", t) if unicodedata.category(c) != "Mn")


def celular():
    return random.choice(["310", "311", "312", "313", "314", "315", "316", "317", "318", "320", "321", "322", "323", "300", "301", "302", "304", "305"]) + \
        f"{random.randint(1000000, 9999999)}"


def fijo_quindio():
    return "606" + f"{random.choice([7, 7, 7, 3])}{random.randint(100000, 999999)}"


def cedula(antigua=False):
    if antigua:
        return str(random.randint(7500000, 9899999)) if random.random() < .5 else str(random.randint(24300000, 41999999))
    return "1094" + f"{random.randint(100000, 999999)}"


usados_doc = set()


def doc_unico(antigua=False):
    while True:
        d = cedula(antigua)
        if d not in usados_doc:
            usados_doc.add(d)
            return d


def nit():
    while True:
        base = random.randint(900100000, 901899999)
        if base not in usados_doc:
            usados_doc.add(base)
            pesos = [41, 37, 29, 23, 19, 17, 13, 7, 3]
            digitos = [int(c) for c in str(base)]
            s = sum(d * p for d, p in zip(digitos, pesos)) % 11
            dv = s if s < 2 else 11 - s
            return f"{base}-{dv}"


# ---------------------------------------------------------------------------
# Catalogos escritos a mano
# ---------------------------------------------------------------------------
MUNICIPIOS = [  # nombre, departamento, DANE
    ("Armenia", "Quindío", "63001"), ("Buenavista", "Quindío", "63111"), ("Calarcá", "Quindío", "63130"),
    ("Circasia", "Quindío", "63190"), ("Córdoba", "Quindío", "63212"), ("Filandia", "Quindío", "63272"),
    ("Génova", "Quindío", "63302"), ("La Tebaida", "Quindío", "63401"), ("Montenegro", "Quindío", "63470"),
    ("Pijao", "Quindío", "63548"), ("Quimbaya", "Quindío", "63594"), ("Salento", "Quindío", "63690"),
    ("Pereira", "Risaralda", "66001"), ("Manizales", "Caldas", "17001"),
]
MUN_ID = {m[0]: i + 1 for i, m in enumerate(MUNICIPIOS)}
insert("municipio", ["id_municipio", "nombre", "departamento", "codigo_dane"],
       [(i + 1, *m) for i, m in enumerate(MUNICIPIOS)])

VEREDAS = {
    "Armenia": ["El Caimo", "Puerto Espejo", "La Revancha", "Murillo"],
    "Buenavista": ["Río Verde Alto", "La Mina", "Los Sauces"],
    "Calarcá": ["Quebradanegra", "La Bella", "Planadas", "La Paloma"],
    "Circasia": ["La Cabaña", "Villarazo", "Membrillal", "Barcelona Alta"],
    "Córdoba": ["Río Verde", "Media Cara", "Bellavista"],
    "Filandia": ["La Lotería", "Cruces", "El Vigilante", "Fachadas"],
    "Génova": ["El Cairo", "Pedregales", "San Juan"],
    "La Tebaida": ["La Silvia", "El Edén", "Padilla"],
    "Montenegro": ["Pueblo Tapao", "Baraya", "La Montaña", "El Gigante"],
    "Pijao": ["Río Lejos", "La Mariela", "Los Balsos"],
    "Quimbaya": ["El Laurel", "Kerman", "La Unión", "Palermo"],
    "Salento": ["Boquía", "Palestina", "Llano Grande", "Canaán"],
}
ALTITUD = {"Armenia": 1480, "Buenavista": 1700, "Calarcá": 1550, "Circasia": 1750, "Córdoba": 1500,
           "Filandia": 1900, "Génova": 1700, "La Tebaida": 1200, "Montenegro": 1300, "Pijao": 1650,
           "Quimbaya": 1350, "Salento": 2050}

ROLES = [
    (1, "ADMIN", "Administrador general: usuarios, catálogo, reportes y configuración"),
    (2, "OPERADOR", "Empleado de punto de acopio: recibe lotes, gestiona pedidos y despachos"),
    (3, "CAMPESINO", "Productor vinculado: consulta sus fincas, lotes y liquidaciones"),
    (4, "CLIENTE", "Comprador registrado: hace pedidos, se suscribe a planes y paga en línea"),
]
insert("rol", ["id_rol", "nombre", "descripcion"], ROLES)

CATEGORIAS = [
    ("Musáceas", "Plátano y banano cultivados en asocio con café en la zona cafetera"),
    ("Frutas tropicales", "Frutas de clima medio y cálido: piña, papaya, maracuyá, mango y similares"),
    ("Frutas de clima frío", "Frutas de ladera alta: mora, lulo, tomate de árbol, fresa y uchuva"),
    ("Cítricos", "Naranja, mandarina y limón de los valles bajos del Quindío"),
    ("Aguacates", "Aguacate Hass de exportación y variedades criollas"),
    ("Café", "Café de origen Quindío en pergamino y tostado"),
    ("Procesados", "Productos transformados por asociaciones campesinas: pulpas y panela"),
]
insert("categoria", ["id_categoria", "nombre", "descripcion"], [(i + 1, *c) for i, c in enumerate(CATEGORIAS)])

# nombre, variedad, categoria, precio venta kg, iva, vida util, refrigeracion, precio finca kg, municipios donde se cultiva
PRODUCTOS = [
    ("Plátano", "Dominico hartón", 1, 3200, 0, 12, False, 1900, ["Montenegro", "Quimbaya", "La Tebaida", "Armenia", "Calarcá"]),
    ("Banano", "Gros Michel", 1, 2800, 0, 9, False, 1500, ["Montenegro", "Quimbaya", "Calarcá"]),
    ("Piña", "Oro Miel MD2", 2, 3800, 0, 14, False, 2200, ["La Tebaida", "Quimbaya", "Montenegro"]),
    ("Papaya", "Tainung", 2, 3500, 0, 8, False, 2000, ["La Tebaida", "Montenegro", "Quimbaya"]),
    ("Maracuyá", "Amarillo", 2, 5200, 0, 14, False, 3000, ["La Tebaida", "Quimbaya", "Montenegro", "Armenia"]),
    ("Guayaba", "Pera", 2, 3600, 0, 8, False, 2000, ["Calarcá", "Córdoba", "Armenia"]),
    ("Granadilla", "Ruiz", 2, 8600, 0, 20, False, 5200, ["Pijao", "Génova", "Córdoba", "Buenavista"]),
    ("Gulupa", "Morada", 2, 9800, 0, 20, False, 6000, ["Génova", "Pijao", "Buenavista"]),
    ("Mora", "De Castilla sin espinas", 3, 7800, 0, 5, True, 4600, ["Salento", "Filandia", "Circasia", "Génova"]),
    ("Lulo", "La Selva", 3, 6500, 0, 10, False, 3900, ["Pijao", "Génova", "Córdoba", "Buenavista", "Calarcá"]),
    ("Tomate de árbol", "Rojo común", 3, 4800, 0, 15, False, 2800, ["Salento", "Filandia", "Circasia", "Génova"]),
    ("Fresa", "Albión", 3, 12000, 0, 5, True, 7200, ["Salento", "Filandia"]),
    ("Uchuva", "Colombiana", 3, 14000, 0, 20, True, 8500, ["Salento", "Filandia", "Génova"]),
    ("Naranja", "Valencia", 4, 2600, 0, 25, False, 1400, ["La Tebaida", "Montenegro", "Quimbaya", "Armenia"]),
    ("Mandarina", "Arrayana", 4, 4200, 0, 20, False, 2400, ["La Tebaida", "Montenegro", "Quimbaya"]),
    ("Limón", "Tahití", 4, 3800, 0, 25, False, 2100, ["La Tebaida", "Montenegro", "Armenia"]),
    ("Aguacate", "Hass", 5, 9800, 0, 12, False, 6200, ["Pijao", "Génova", "Salento", "Córdoba", "Filandia"]),
    ("Aguacate", "Papelillo Lorena", 5, 6500, 0, 8, False, 3800, ["Calarcá", "Montenegro", "Quimbaya"]),
    ("Café pergamino seco", "Castillo", 6, 24000, 0, 365, False, 18500, ["Pijao", "Génova", "Buenavista", "Córdoba", "Calarcá", "Salento"]),
    ("Café tostado molido", "Castillo origen Quindío", 6, 52000, 5, 180, False, 36000, ["Pijao", "Génova", "Buenavista"]),
    ("Pulpa de fruta", "Mora", 7, 14000, 19, 90, True, 8800, ["Salento", "Filandia"]),
    ("Pulpa de fruta", "Lulo", 7, 15000, 19, 90, True, 9200, ["Pijao", "Génova"]),
    ("Panela", "Pulverizada", 7, 6500, 0, 365, False, 4200, ["Buenavista", "Pijao", "Córdoba"]),
]
insert("producto", ["id_producto", "id_categoria", "nombre", "variedad", "unidad_medida", "precio_venta_kg", "iva_pct",
                    "vida_util_dias", "requiere_refrigeracion", "activo"],
       [(i + 1, p[2], p[0], p[1], "KG", p[3], p[4], p[5], p[6], True) for i, p in enumerate(PRODUCTOS)])

PUNTOS = [  # nombre, municipio, direccion, capacidad, refrigerado
    ("Acopio Central Armenia", "Armenia", "Calle 50 # 19-30, Bodega 4, sector Mercar", 60000, True),
    ("Acopio Calarcá", "Calarcá", "Carrera 25 # 38-20, barrio La Huerta", 25000, True),
    ("Acopio Quimbaya", "Quimbaya", "Calle 15 # 4-52, salida a Filandia", 20000, False),
    ("Acopio Filandia", "Filandia", "Carrera 6 # 8-14, vía a Quimbaya", 12000, True),
    ("Acopio Montenegro", "Montenegro", "Calle 17 # 6-35, sector galería", 18000, False),
    ("Acopio La Tebaida", "La Tebaida", "Carrera 7 # 10-81, vía al aeropuerto El Edén", 22000, True),
]
insert("punto_acopio", ["id_punto", "id_municipio", "nombre", "direccion", "telefono", "capacidad_kg", "refrigerado", "activo"],
       [(i + 1, MUN_ID[p[1]], p[0], p[2], fijo_quindio(), p[3], p[4], True) for i, p in enumerate(PUNTOS)])
PUNTO_DE_MUNICIPIO = {"Armenia": 1, "Calarcá": 2, "Quimbaya": 3, "Filandia": 4, "Montenegro": 5, "La Tebaida": 6,
                      "Circasia": 1, "Salento": 4, "Córdoba": 2, "Pijao": 2, "Génova": 2, "Buenavista": 2,
                      "Pereira": 3, "Manizales": 4}

PLANES = [
    ("Canasta Familiar Semanal", "Canasta de 8 kg de frutas de temporada entregada cada semana en casa. Incluye 5 % de descuento en compras adicionales.", "SEMANAL", 69900, 8, 5),
    ("Canasta Familiar Quincenal", "Canasta de 15 kg de frutas y plátano cada quince días, ideal para familias de 4 personas.", "QUINCENAL", 119900, 15, 5),
    ("Canasta Saludable Mensual", "30 kg al mes de frutas de clima frío, cítricos y aguacate con asesoría nutricional básica.", "MENSUAL", 219900, 30, 8),
    ("Plan Tienda de Barrio", "Surtido quincenal de 90 kg con precio preferencial para tiendas y fruterías.", "QUINCENAL", 450000, 90, 10),
    ("Plan Restaurante", "150 kg mensuales con entregas programadas dos veces por semana y prioridad en producto extra.", "MENSUAL", 890000, 150, 12),
    ("Plan Hotel Premium", "300 kg mensuales de fruta seleccionada para desayunos y bar, con cadena de frío garantizada.", "MENSUAL", 1650000, 300, 15),
]
insert("plan", ["id_plan", "nombre", "descripcion", "periodicidad", "precio", "kg_incluidos", "descuento_pct", "activo"],
       [(i + 1, *p, True) for i, p in enumerate(PLANES)])
DIAS_PERIODO = {"SEMANAL": 7, "QUINCENAL": 15, "MENSUAL": 30}

METODOS = [  # nombre, tipo, comision, en_linea, peso de uso
    ("Tarjeta de crédito", "TARJETA", 2.99, True, 18),
    ("Tarjeta débito", "TARJETA", 1.50, True, 14),
    ("PSE", "PSE", 1.20, True, 22),
    ("Nequi", "BILLETERA", 1.00, True, 20),
    ("Daviplata", "BILLETERA", 1.00, True, 8),
    ("Transferencia bancaria", "TRANSFERENCIA", 0.00, True, 8),
    ("Efectivo contra entrega", "EFECTIVO", 0.00, False, 10),
]
insert("metodo_pago", ["id_metodo", "nombre", "tipo", "comision_pct", "en_linea", "activo"],
       [(i + 1, m[0], m[1], m[2], m[3], True) for i, m in enumerate(METODOS)])

# ---------------------------------------------------------------------------
# Personas
# ---------------------------------------------------------------------------
NOMBRES_H = ["José", "Luis", "Carlos", "Jorge", "Juan Carlos", "Andrés", "Jhon Fredy", "Diego", "Hernán", "Gustavo", "Fabio",
             "Óscar", "Alberto", "Jairo", "Rubén", "Wilson", "Édgar", "Julián", "Mauricio", "Sebastián", "Alejandro",
             "Fernando", "Héctor", "Libardo", "Arnulfo", "Gildardo", "Albeiro", "Orlando", "Néstor", "Camilo"]
NOMBRES_M = ["María", "Luz Marina", "Ana Lucía", "Gloria", "Martha", "Claudia", "Sandra", "Diana", "Paola", "Liliana",
             "Beatriz", "Rosa Elena", "Carolina", "Natalia", "Valentina", "Adriana", "Yaneth", "Luz Dary", "Olga",
             "Marleny", "Gladys", "Nubia", "Esperanza", "Daniela", "Laura", "Juliana", "Alba Lucía", "Mónica"]
APELLIDOS = ["Gómez", "Ramírez", "Giraldo", "Osorio", "Arango", "Valencia", "Henao", "Londoño", "Cardona", "Restrepo",
             "Marín", "Ospina", "García", "López", "Hernández", "Salazar", "Quintero", "Castaño", "Zuluaga", "Gutiérrez",
             "Montoya", "Agudelo", "Rendón", "Jaramillo", "Correa", "Ríos", "Toro", "Escobar", "Duque", "Mejía",
             "Bedoya", "Patiño", "Arias", "Muñoz", "Loaiza", "Grajales", "Aristizábal", "Betancur", "Sánchez", "Velásquez"]
usados_nombre = set()


def persona(mujer=None):
    while True:
        m = random.random() < .5 if mujer is None else mujer
        n = random.choice(NOMBRES_M if m else NOMBRES_H)
        a = f"{random.choice(APELLIDOS)} {random.choice(APELLIDOS)}"
        if a.split()[0] != a.split()[1] and (n, a) not in usados_nombre:
            usados_nombre.add((n, a))
            return n, a


usados_email = set()
usados_user = set()


def email_de(n, a, dominio=None):
    base = sin_tildes(n.split()[0].lower()) + "." + sin_tildes(a.split()[0].lower())
    dominio = dominio or random.choice(["gmail.com", "gmail.com", "gmail.com", "hotmail.com", "outlook.com", "yahoo.es"])
    e, k = f"{base}@{dominio}", 2
    while e in usados_email:
        e, k = f"{base}{k}@{dominio}", k + 1
    usados_email.add(e)
    return e


def username_de(n, a):
    base = sin_tildes(n.split()[0][0].lower() + a.split()[0].lower()).replace(" ", "")
    u, k = base, 2
    while u in usados_user:
        u, k = f"{base}{k}", k + 1
    usados_user.add(u)
    return u


usuarios = []      # id, rol, username, hash, nombre, email, tel, activo, fecha_registro, ultimo_acceso


def nuevo_usuario(rol, username, nombre, email, tel, registro):
    uid = len(usuarios) + 1
    acceso = datetime.combine(HOY - timedelta(days=random.randint(0, 20)), datetime.min.time()) + timedelta(hours=random.randint(6, 21), minutes=random.randint(0, 59))
    usuarios.append([uid, rol, username, HASH[ROLES[rol - 1][1]], nombre, email, tel, True, registro, acceso])
    usados_user.add(username)
    usados_email.add(email)
    return uid


nuevo_usuario(1, "admin", "Administrador del Sistema", "admin@frutimarket.com.co", "3104567890", datetime(2025, 12, 1, 8, 0))

# --- Empleados (un coordinador por punto con usuario OPERADOR) ---
empleados = []
CARGOS = [("Auxiliar de bodega", 1750905, 1950000), ("Operario de cargue", 1800000, 2000000),
          ("Analista de calidad", 2700000, 3100000), ("Auxiliar administrativo", 2100000, 2400000)]
SEDE = [("Gerente general", 8500000), ("Contador", 5200000), ("Ejecutiva comercial", 3400000), ("Ejecutivo comercial", 3300000),
        ("Coordinadora de logística", 4100000)]
for pid in range(1, len(PUNTOS) + 1):
    n, a = persona()
    em = sin_tildes(n.split()[0].lower()) + "." + sin_tildes(a.split()[0].lower()) + "@frutimarket.com.co"
    tel = celular()
    uid = nuevo_usuario(2, username_de(n, a), f"{n} {a}", em, tel, datetime(2025, 12, 10, 9, 0))
    empleados.append([len(empleados) + 1, pid, uid, doc_unico(True), n, a, "Coordinador de acopio", random.choice([3800000, 3900000, 4000000]),
                      date(random.randint(2019, 2025), random.randint(1, 12), random.randint(1, 28)), tel, em, True])
    for _ in range(random.randint(2, 4) if pid > 1 else 5):
        cargo, smin, smax = random.choice(CARGOS)
        n, a = persona()
        empleados.append([len(empleados) + 1, pid, None, doc_unico(random.random() < .4), n, a, cargo,
                          round(random.randint(smin, smax), -4) if smin > 1750905 else smin,
                          date(random.randint(2020, 2026), random.randint(1, 12), random.randint(1, 28)) if True else None,
                          celular(), email_de(n, a), random.random() > .06])
for cargo, sal in SEDE:
    n, a = persona(mujer=cargo.endswith("a") and not cargo.startswith("Gerente") or None)
    em = sin_tildes(n.split()[0].lower()) + "." + sin_tildes(a.split()[0].lower()) + "@frutimarket.com.co"
    empleados.append([len(empleados) + 1, 1, None, doc_unico(True), n, a, cargo, sal,
                      date(random.randint(2018, 2024), random.randint(1, 12), random.randint(1, 28)), celular(), em, True])
for e in empleados:  # una fecha de ingreso de 2026 no puede ser posterior a hoy
    if e[8] > FIN:
        e[8] = date(2026, random.randint(1, 9), random.randint(1, 28))

# --- Campesinos y fincas ---
NOMBRES_FINCA = ["La Esperanza", "El Paraíso", "La Primavera", "Villa Luz", "El Mirador", "La Cabaña", "Buenos Aires",
                 "Santa Elena", "La Palmera", "El Recreo", "San José", "La Floresta", "Los Guaduales", "El Diamante",
                 "La Carolina", "Villa Nueva", "El Jardín", "La Divisa", "Las Brisas", "El Edén", "Monteloro",
                 "La Graciela", "El Porvenir", "La Selva", "Bellavista", "La Aurora", "El Placer", "Los Naranjos",
                 "La Arcadia", "El Retiro", "Altamira", "La Pradera", "Guayacanes", "El Cedral", "La Ilusión", "Brisas del Quindío"]
campesinos, fincas = [], []
municipios_rurales = list(VEREDAS.keys())
fincas_por_mun = {m: [] for m in municipios_rurales}
nombres_finca = NOMBRES_FINCA[:]
random.shuffle(nombres_finca)
for i in range(28):
    mun = municipios_rurales[i % len(municipios_rurales)]
    n, a = persona()
    tel = celular()
    em = email_de(n, a) if random.random() < .7 else None
    uid = None
    if i < 8:
        em = em or email_de(n, a)
        uid = nuevo_usuario(3, username_de(n, a), f"{n} {a}", em, tel, datetime(2026, 1, random.randint(5, 30), 10, 0))
    vinc = date(random.randint(2023, 2025), random.randint(1, 12), random.randint(1, 28)) if i < 24 else date(2026, random.randint(2, 8), random.randint(1, 28))
    campesinos.append([i + 1, uid, MUN_ID[mun], "CC", doc_unico(True), n, a, tel, em, vinc, random.random() < .55, True])
    for k in range(1 if random.random() < .65 else 2):
        fm = mun if k == 0 else random.choice(municipios_rurales)
        nombre = nombres_finca.pop() if nombres_finca else f"{random.choice(NOMBRES_FINCA)} {k + 2}"
        fid = len(fincas) + 1
        fincas.append([fid, i + 1, MUN_ID[fm], nombre, random.choice(VEREDAS[fm]),
                       round(random.uniform(1.5, 22), 2), ALTITUD[fm] + random.randint(-150, 250)])
        fincas_por_mun[fm].append(fid)

# --- Transportadores ---
VEHICULOS = [("Camión NHR", 2500, False), ("Furgón refrigerado", 3500, True), ("Camioneta D-Max", 1000, False),
             ("Turbo", 4500, False), ("Furgón refrigerado", 2000, True), ("Camioneta D-Max", 1100, False),
             ("Camión NPR", 4000, False), ("Furgoneta eléctrica", 700, False)]
LETRAS = "BCDFGHJKLMNPRSTVWXYZ"
transportadores = []
for i, (tipo, cap, refri) in enumerate(VEHICULOS):
    n, a = persona(mujer=False if i != 4 else True)
    placa = random.choice(["T", "W", "S", "U", "K"]) + "".join(random.choice(LETRAS) for _ in range(2)) + f"{random.randint(100, 999)}"
    nombre = f"{n} {a}" if i not in (1, 6) else ["Transportes Refrigerados del Café S.A.S.", "Logística Andina del Quindío S.A.S."][i == 6]
    transportadores.append([i + 1, doc_unico(True) if i not in (1, 6) else nit(), nombre, celular(), placa, tipo, cap, refri, True])

# --- Clientes ---
NEGOCIOS = {
    "RESTAURANTE": ["Restaurante La Fonda del Arriero", "Restaurante Sazón de Montaña", "Asadero El Fogón Quindiano",
                    "Restaurante Trucha y Bosque", "Comedor La Molienda", "Restaurante Raíces del Valle", "Café Bistró La Guadua"],
    "HOTEL": ["Hotel Campestre Las Heliconias", "Hotel Mirador del Valle de Cocora", "Hostal Casa de los Bambúes",
              "Hotel Boutique Calle Real", "Finca Hotel El Bosque de los Yarumos", "Glamping Niebla Andina"],
    "TIENDA": ["Frutería Doña Rosa", "Tienda Mixta El Paisa", "Supertienda La Economía", "Frutas y Verduras El Campesino",
               "Tienda Naturista Vida Verde", "Granero San Antonio"],
    "SUPERMERCADO": ["Supermercado La Cosecha", "Mercados Familia Unida", "Autoservicio Plaza Fresca"],
}
DIRECCIONES = {
    "Armenia": ["Calle 21 # 14-36, Centro", "Carrera 14 # 1N-45, Av. Bolívar", "Carrera 13 # 12N-30, La Castellana",
                "Calle 10 # 18-52, Galán", "Carrera 19 # 37-12, Granada", "Av. Centenario # 4-120, Portal del Quindío",
                "Calle 50 # 23-14, La Clarita", "Carrera 23 # 9-28, Los Profesionales", "Manzana 12 casa 8, La Patria"],
    "Calarcá": ["Carrera 25 # 40-12, Centro", "Calle 39 # 27-15, Las Ferias", "Carrera 23 # 44-08, Llanitos"],
    "Circasia": ["Carrera 14 # 6-22, Centro", "Calle 7 # 11-40, El Bosque"],
    "Filandia": ["Calle 6 # 6-45, Calle del Tiempo Detenido", "Carrera 5 # 8-12, Centro"],
    "Salento": ["Calle Real # 4-30", "Carrera 6 # 5-11, vía Cocora km 1"],
    "Montenegro": ["Calle 16 # 5-40, Centro", "Carrera 6 # 19-15, Villa Juliana"],
    "Quimbaya": ["Carrera 4 # 14-22, Centro", "Calle 18 # 7-30, Kennedy"],
    "La Tebaida": ["Carrera 7 # 12-20, Centro", "Calle 11 # 5-48, Villa Alejandra"],
    "Pereira": ["Carrera 8 # 21-45, Centro", "Av. Circunvalar # 12-80, Pinares"],
    "Manizales": ["Carrera 23 # 62-15, Cable Plaza", "Calle 65 # 23-40, Milán"],
}
clientes = []
MUN_CLIENTE = ["Armenia"] * 9 + ["Calarcá"] * 3 + ["Circasia", "Filandia", "Salento", "Montenegro", "Quimbaya", "La Tebaida"] * 2 + ["Pereira", "Manizales"]
hoteles_mun = {"Hotel Mirador del Valle de Cocora": "Salento", "Glamping Niebla Andina": "Salento", "Hotel Boutique Calle Real": "Filandia",
               "Finca Hotel El Bosque de los Yarumos": "Montenegro", "Hotel Campestre Las Heliconias": "Quimbaya",
               "Hostal Casa de los Bambúes": "Circasia", "Restaurante Trucha y Bosque": "Salento"}


def agregar_cliente(tipo, nombre, mun, con_usuario, registro, doc=None, email=None, tel=None, n_a=None):
    cid = len(clientes) + 1
    if tipo == "HOGAR":
        tdoc = "CC"
        doc = doc or doc_unico(random.random() < .5)
        email = email or email_de(*n_a)
    else:
        tdoc = "NIT"
        doc = doc or nit()
        base = sin_tildes(nombre.lower()).replace("restaurante ", "").replace("hotel ", "").replace("s.a.s.", "")
        base = "".join(ch for ch in base if ch.isalnum())[:22]
        email = email or f"{random.choice(['compras', 'gerencia', 'pedidos', 'administracion'])}@{base}.com.co"
        usados_email.add(email)
    tel = tel or (celular() if random.random() < .75 else fijo_quindio())
    uid = None
    if con_usuario:
        user = username_de(*n_a) if n_a else sin_tildes(nombre.split()[-1].lower()) + str(cid)
        uid = nuevo_usuario(4, user, nombre, email, tel, datetime.combine(registro, datetime.min.time()) + timedelta(hours=random.randint(7, 20)))
    clientes.append([cid, uid, MUN_ID[mun], tipo, tdoc, doc, nombre, tel, email, random.choice(DIRECCIONES[mun]), registro, True])
    return cid


# Cliente de demostracion con usuario 'cliente'
n_demo = ("Laura", "Restrepo Giraldo")
usados_nombre.add(n_demo)
cid_demo = len(clientes) + 1
uid_demo = nuevo_usuario(4, "cliente", "Laura Restrepo Giraldo", "laura.restrepo@gmail.com", "3157894512", datetime(2026, 1, 8, 18, 20))
clientes.append([cid_demo, uid_demo, MUN_ID["Armenia"], "HOGAR", "CC", doc_unico(), "Laura Restrepo Giraldo", "3157894512",
                 "laura.restrepo@gmail.com", "Carrera 13 # 12N-30, La Castellana", date(2026, 1, 8), True])

for tipo, nombres in NEGOCIOS.items():
    for nom in nombres:
        mun = hoteles_mun.get(nom, random.choice(MUN_CLIENTE))
        agregar_cliente(tipo, nom, mun, random.random() < .5, date(2025, 6, 1) + timedelta(days=random.randint(0, 330)))
for i in range(30):
    n, a = persona()
    mun = random.choice(MUN_CLIENTE)
    reg = date(2025, 9, 1) + timedelta(days=int(random.random() ** 1.3 * 400))
    agregar_cliente("HOGAR", f"{n} {a}", mun, random.random() < .45, reg, n_a=(n, a))
# fecha de registro del cliente <= fecha de su usuario
TIPO_PESO = {"HOGAR": 1.0, "RESTAURANTE": 2.6, "HOTEL": 2.4, "TIENDA": 2.0, "SUPERMERCADO": 2.2}
KG_PEDIDO = {"HOGAR": (3, 14), "RESTAURANTE": (18, 70), "HOTEL": (25, 110), "TIENDA": (25, 90), "SUPERMERCADO": (70, 260)}

# ---------------------------------------------------------------------------
# Simulacion diaria: lotes, suscripciones, pedidos, pagos, facturas, despachos
# ---------------------------------------------------------------------------
lotes = []          # id, codigo, finca, producto, punto, cosecha, vence, kg, disp, precio_finca, calidad, estado
disp = {}           # id_lote -> kg disponibles
pedidos, detalles, pagos, facturas, despachos, notifs, suscripciones = [], [], [], [], [], [], []
ref_usadas = set()

# productos ofrecidos por cada punto segun municipios de cultivo cercanos
PUNTO_PRODUCTOS = {}
# el acopio central maneja todo el catalogo; los demas, lo que se cultiva cerca
PUNTO_PRODUCTOS[1] = list(range(1, len(PRODUCTOS) + 1))
_cerca = {2: ["Calarcá", "Córdoba", "Pijao", "Génova", "Buenavista"], 3: ["Quimbaya", "Montenegro", "Filandia"],
          4: ["Filandia", "Salento", "Circasia"], 5: ["Montenegro", "Quimbaya", "La Tebaida"], 6: ["La Tebaida", "Montenegro", "Armenia"]}
for pid, muns in _cerca.items():
    PUNTO_PRODUCTOS[pid] = [i + 1 for i, p in enumerate(PRODUCTOS) if set(p[8]) & set(muns)]


def temporada(prod_idx, d):
    """Factor de oferta: picos de cosecha tipicos (mitaca abr-jun y principal oct-dic)."""
    m = d.month
    nombre = PRODUCTOS[prod_idx][0]
    if nombre.startswith("Café pergamino"):
        return 1.6 if m in (4, 5, 10, 11) else .5
    if nombre in ("Mora", "Fresa", "Uchuva"):
        return 1.3 if m in (3, 4, 5, 9, 10) else .9
    if nombre in ("Mango", "Mandarina", "Naranja"):
        return 1.3 if m in (6, 7, 11, 12) else .9
    if nombre == "Aguacate":
        return 1.4 if m in (2, 3, 8, 9, 10) else .8
    return 1.0


def demanda(d):
    """Pedidos esperados en el dia: crecimiento del negocio + temporada turistica (vacaciones de mitad de anio, Semana Santa)."""
    base = 2.6 + (d - INICIO).days / 300 * 2.4
    if d.month in (6, 7):
        base *= 1.35
    if date(2026, 3, 29) <= d <= date(2026, 4, 5):
        base *= 1.5
    if d.month == 10:
        base *= 1.1
    if d.weekday() == 6:
        base *= .55
    return base


def ref_pago(metodo):
    pref = {"TARJETA": "TC", "PSE": "PSE", "BILLETERA": "BIL", "TRANSFERENCIA": "TRF", "EFECTIVO": "EFE"}[METODOS[metodo - 1][1]]
    while True:
        r = f"{pref}{random.randint(10000000, 99999999)}"
        if r not in ref_usadas:
            ref_usadas.add(r)
            return r


def metodo_aleatorio(solo_linea=False):
    pesos = [m[4] if (m[3] or not solo_linea) else 0 for m in METODOS]
    return random.choices(range(1, len(METODOS) + 1), weights=pesos)[0]


def dt(d, h1=7, h2=20):
    return datetime.combine(d, datetime.min.time()) + timedelta(hours=random.randint(h1, h2), minutes=random.randint(0, 59), seconds=random.randint(0, 59))


def notificar(uid, tipo, titulo, mensaje, cuando, leida=None):
    if uid is None:
        return
    if leida is None:
        leida = cuando.date() < HOY - timedelta(days=4) or random.random() < .3
    notifs.append([len(notifs) + 1, uid, tipo, titulo, mensaje, cuando, leida if cuando.date() <= HOY else False,
                   cuando - timedelta(minutes=random.randint(0, 30)) if tipo != "RECORDATORIO" else cuando - timedelta(days=random.randint(1, 5))])


consecutivo_lote = {}
codigo_finca_punto = {}


def recibir_lote(d, pid, prod, kg):
    pr = PRODUCTOS[prod - 1]
    # finca de un municipio donde se cultiva el producto
    candidatos = [f for m in pr[8] for f in fincas_por_mun.get(m, [])]
    if not candidatos:
        return
    fid = random.choice(candidatos)
    cosecha = d - timedelta(days=random.randint(0, 2 if pr[5] < 30 else 20))
    calidad = random.choices(["EXTRA", "PRIMERA", "SEGUNDA"], weights=[25, 55, 20])[0]
    precio = round(pr[7] * {"EXTRA": 1.12, "PRIMERA": 1.0, "SEGUNDA": .82}[calidad] * random.uniform(.94, 1.06), -1)
    lid = len(lotes) + 1
    consecutivo_lote[d.year] = consecutivo_lote.get(d.year, 0) + 1
    codigo = f"LT-{d.strftime('%y%m')}-{consecutivo_lote[d.year]:04d}"
    lotes.append([lid, codigo, fid, prod, pid, cosecha, cosecha + timedelta(days=pr[5]), kg, kg, precio, calidad, "DISPONIBLE"])
    disp[lid] = kg
    lotes_abiertos[(pid, prod)].append(lid)


from collections import defaultdict
lotes_abiertos = defaultdict(list)
# venta diaria esperada (kg) por punto y producto; se ajusta con lo que realmente se vende
ventas_media = defaultdict(float)
for pid in range(1, len(PUNTOS) + 1):
    for prod in range(1, len(PRODUCTOS) + 1):
        ventas_media[(pid, prod)] = (3.0 if pid == 1 else 1.0) * (.3 if PRODUCTOS[prod - 1][0].startswith("Café") else 1)
vendido_hoy = defaultdict(float)
cli_ids = [c[0] for c in clientes]
cli_pesos = [TIPO_PESO[c[3]] for c in clientes]
suscripcion_activa = {}   # id_cliente -> (id_suscripcion, plan, fin)

# Suscripciones: algunos clientes se suscriben y renuevan periodo a periodo
PLAN_POR_TIPO = {"HOGAR": [1, 2, 3], "RESTAURANTE": [5], "HOTEL": [6, 5], "TIENDA": [4], "SUPERMERCADO": [4]}
candidatos_sus = [c for c in clientes if c[1] is not None or c[3] != "HOGAR"]
random.shuffle(candidatos_sus)
plan_cliente = {}
for c in candidatos_sus[:24]:
    plan_cliente[c[0]] = (random.choice(PLAN_POR_TIPO[c[3]]), max(c[10], INICIO) + timedelta(days=random.randint(3, 120)))
plan_cliente[cid_demo] = (1, date(2026, 2, 2))

d = INICIO
while d <= FIN:
    # 1. recepcion de lotes en cada punto
    #    Punto de reorden: si las existencias vigentes no cubren ~5 dias de la venta
    #    promedio reciente, el coordinador recibe un lote nuevo de alguna finca.
    for pid in range(1, len(PUNTOS) + 1):
        for prod in PUNTO_PRODUCTOS[pid]:
            pr = PRODUCTOS[prod - 1]
            media = ventas_media[(pid, prod)]
            cobertura = min(pr[5] - 2, 4)
            existencias = sum(disp[l] for l in lotes_abiertos[(pid, prod)] if lotes[l - 1][6] > d + timedelta(days=1))
            if existencias < media * cobertura and random.random() < .75 * temporada(prod - 1, d) ** .5:
                kg = media * min(pr[5] - 2, 8) * random.uniform(0.9, 1.3)
                kg = max(12 if pr[5] < 60 else 30, round(kg / 5) * 5)
                recibir_lote(d, pid, prod, kg)

    # 2. suscripciones que inician o se renuevan hoy
    for cid, (plan, inicio) in plan_cliente.items():
        actual = suscripcion_activa.get(cid)
        if (actual is None and d == inicio) or (actual is not None and actual[2] == d):
            if actual is not None:
                suscripciones[actual[0] - 1][5] = "VENCIDA"
                if random.random() < .08:           # algunos no renuevan
                    suscripcion_activa.pop(cid)
                    plan_cliente[cid] = (plan, FIN + timedelta(days=99))
                    continue
            pl = PLANES[plan - 1]
            fin = d + timedelta(days=DIAS_PERIODO[pl[2]])
            sid = len(suscripciones) + 1
            suscripciones.append([sid, cid, plan, d, fin, "ACTIVA", random.random() < .8])
            suscripcion_activa[cid] = (sid, plan, fin)
            met = metodo_aleatorio(solo_linea=True)
            cuando = dt(d, 6, 9)
            pagos.append([len(pagos) + 1, met, None, sid, cuando, pl[3], ref_pago(met), "APROBADO"])
            uid = clientes[cid - 1][1]
            notificar(uid, "SUSCRIPCION", f"{pl[0]} activa", f"Tu plan quedó activo hasta el {fin.strftime('%d/%m/%Y')}. Pagaste ${pl[3]:,.0f}.".replace(",", "."), cuando)
            notificar(uid, "RECORDATORIO", "Tu plan se renueva pronto",
                      f"El {fin.strftime('%d/%m/%Y')} se renueva tu {pl[0]}. Revisa tu medio de pago.", dt(fin - timedelta(days=2), 8, 9))

    # 3. pedidos del dia
    n_ped = sum(1 for _ in range(12) if random.random() < demanda(d) / 12 * 2.2)
    for _ in range(n_ped):
        cid = random.choices(cli_ids, weights=cli_pesos)[0]
        cli = clientes[cid - 1]
        if cli[10] > d:
            continue
        mun_cli = MUNICIPIOS[cli[2] - 1][0]
        pid = PUNTO_DE_MUNICIPIO[mun_cli] if random.random() < .8 else 1
        lo, hi = KG_PEDIDO[cli[3]]
        kg_objetivo = random.uniform(lo, hi)
        # lotes vendibles en el punto: ya recibidos, no vencidos, con existencias
        vendibles = [lotes[i - 1] for prod in PUNTO_PRODUCTOS[pid] for i in lotes_abiertos[(pid, prod)]
                     if lotes[i - 1][5] <= d < lotes[i - 1][6] - timedelta(days=1) and disp[i] >= 1]
        if not vendibles:
            continue
        por_producto = {}
        for l in sorted(vendibles, key=lambda l: l[6]):      # primero el que vence antes
            por_producto.setdefault(l[3], []).append(l)
        elegidos = random.sample(list(por_producto), k=min(len(por_producto), random.randint(1, 5 if cli[3] != "HOGAR" else 4)))
        lineas, linea = [], 0
        for prod in elegidos:
            kg_prod = max(1.0, round(kg_objetivo / len(elegidos) * random.uniform(.6, 1.4), 0 if cli[3] != "HOGAR" else 1))
            if PRODUCTOS[prod - 1][0].startswith("Café"):
                kg_prod = max(1.0, round(kg_prod / 4))
            for l in por_producto[prod]:
                if kg_prod <= 0:
                    break
                toma = min(kg_prod, disp[l[0]])
                toma = round(toma, 1)
                if toma <= 0:
                    continue
                linea += 1
                precio = PRODUCTOS[prod - 1][3]
                if l[10] == "EXTRA":
                    precio = round(precio * 1.1, -1)
                elif l[10] == "SEGUNDA":
                    precio = round(precio * .85, -1)
                lineas.append([linea, l[0], toma, precio, PRODUCTOS[prod - 1][4], round(toma * precio)])
                kg_prod = round(kg_prod - toma, 1)
        if not lineas:
            continue
        fecha = dt(d, 6, 21)
        edad = (HOY - d).days
        r = random.random()
        if edad > 6:
            estado = "CANCELADO" if r < .055 else "ENTREGADO"
        elif edad > 2:
            estado = "CANCELADO" if r < .04 else "ENTREGADO" if r < .65 else "DESPACHADO" if r < .85 else "PAGADO"
        else:
            estado = "PENDIENTE" if r < .35 else "PAGADO" if r < .75 else "DESPACHADO"
        sus = suscripcion_activa.get(cid)
        sid = sus[0] if sus and suscripciones[sus[0] - 1][3] <= d else None
        subtotal = sum(x[5] for x in lineas)
        descuento = round(subtotal * PLANES[sus[1] - 1][5] / 100, -1) if sid else 0
        iva = round(sum(x[5] * x[4] / 100 for x in lineas) * (1 - (descuento / subtotal if subtotal else 0)))
        total = subtotal - descuento + iva
        oid = len(pedidos) + 1
        entrega = d + timedelta(days=random.choice([0, 1, 1, 1, 2]) if cli[3] != "HOGAR" else random.choice([1, 1, 2, 3]))
        obs = random.choice([None, None, None, "Entregar en portería", "Llamar antes de llegar", "Fruta para la semana, no muy madura",
                             "Recibe el chef de 7 a 10 a. m.", "Factura a nombre de la empresa", "Dejar en la bodega de atrás"])
        if cli[3] == "HOGAR" and obs in ("Recibe el chef de 7 a 10 a. m.", "Factura a nombre de la empresa", "Dejar en la bodega de atrás"):
            obs = None
        pedidos.append([oid, cid, pid, sid, fecha, entrega, cli[9], estado, subtotal, descuento, iva, total, obs])
        for x in lineas:
            detalles.append([oid, *x])
            if estado != "CANCELADO":
                disp[x[1]] = round(disp[x[1]] - x[2], 1)
                vendido_hoy[(pid, lotes[x[1] - 1][3])] += x[2]
        uid = cli[1]
        notificar(uid, "PEDIDO", f"Pedido #{oid} recibido", f"Recibimos tu pedido por ${total:,.0f}. Entrega programada el {entrega.strftime('%d/%m/%Y')}.".replace(",", "."), fecha)

        # pago en linea (y a veces un intento rechazado antes)
        pagado = estado in ("PAGADO", "DESPACHADO", "ENTREGADO") or (estado == "CANCELADO" and random.random() < .4)
        if pagado:
            met = metodo_aleatorio()
            cuando = fecha + timedelta(minutes=random.randint(2, 50)) if METODOS[met - 1][3] else datetime.combine(entrega, datetime.min.time()) + timedelta(hours=random.randint(8, 17))
            if METODOS[met - 1][1] in ("TARJETA", "PSE") and random.random() < .07:
                pagos.append([len(pagos) + 1, met, oid, None, cuando - timedelta(minutes=1), total, ref_pago(met), "RECHAZADO"])
                notificar(uid, "PAGO", "Pago rechazado", f"Tu banco rechazó el pago del pedido #{oid}. Intenta con otro medio.", cuando - timedelta(minutes=1))
            pagos.append([len(pagos) + 1, met, oid, None, cuando, total, ref_pago(met), "REEMBOLSADO" if estado == "CANCELADO" else "APROBADO"])
            if estado != "CANCELADO":
                notificar(uid, "PAGO", "Pago aprobado", f"Recibimos ${total:,.0f} del pedido #{oid} con {METODOS[met - 1][0]}.".replace(",", "."), cuando)
                facturas.append([len(facturas) + 1, oid, f"FM-{len(facturas) + 1:06d}", cuando + timedelta(seconds=30), subtotal, descuento, iva, total])
        elif estado == "PENDIENTE":
            notificar(uid, "RECORDATORIO", f"Pedido #{oid} pendiente de pago", "Completa el pago para que podamos alistar tu pedido.", fecha + timedelta(hours=3))
        if estado in ("DESPACHADO", "ENTREGADO"):
            kg_total = sum(x[2] for x in lineas)
            refri = any(PRODUCTOS[lotes[x[1] - 1][3] - 1][6] for x in lineas)
            opciones = [t for t in transportadores if t[6] >= kg_total and (t[7] or not refri)] or transportadores[:2]
            t = random.choice(opciones)
            salida = datetime.combine(entrega, datetime.min.time()) + timedelta(hours=random.randint(5, 9), minutes=random.randint(0, 59))
            llegada = salida + timedelta(minutes=random.randint(35, 240)) if estado == "ENTREGADO" else None
            despachos.append([len(despachos) + 1, oid, t[0], f"GD-{len(despachos) + 1:06d}", salida, llegada,
                              "ENTREGADO" if estado == "ENTREGADO" else "EN_RUTA", round(random.uniform(3, 7), 1) if t[7] else None,
                              random.choice([None, None, "Entregado completo", "Recibió administrador", "Sin novedad", "Cliente revisó calidad al recibir"]) if llegada else None])
            notificar(uid, "PEDIDO", f"Pedido #{oid} en camino", f"Tu pedido salió con {t[2].split()[0]} en el vehículo {t[4]}.", salida)
            if llegada:
                notificar(uid, "PEDIDO", f"Pedido #{oid} entregado", "Gracias por comprarle al campo quindiano.", llegada)
    # actualizar la venta media (promedio movil exponencial) y cerrar lotes agotados o vencidos
    for clave in ventas_media:
        ventas_media[clave] = ventas_media[clave] * .93 + vendido_hoy[clave] * .07
        lotes_abiertos[clave] = [i for i in lotes_abiertos[clave] if disp[i] > 0 and lotes[i - 1][6] > d]
    vendido_hoy.clear()
    d += timedelta(days=1)

# Suscripciones del estado final
for cid, (sid, plan, fin) in suscripcion_activa.items():
    if fin <= HOY:
        suscripciones[sid - 1][5] = "VENCIDA"
for s in random.sample([s for s in suscripciones if s[5] == "ACTIVA"], k=2):
    s[5] = "PAUSADA"

# alertas de inventario para los coordinadores
coordinador = {e[1]: e[2] for e in empleados if e[2]}
for l in lotes:
    if disp[l[0]] > 0 and HOY - timedelta(days=60) <= l[6] <= HOY + timedelta(days=3):
        notificar(coordinador[l[4]], "INVENTARIO", f"Lote {l[1]} por vencer",
                  f"Quedan {disp[l[0]]:.1f} kg de {PRODUCTOS[l[3] - 1][0]} {PRODUCTOS[l[3] - 1][1]}; vence el {l[6].strftime('%d/%m/%Y')}.",
                  dt(l[6] - timedelta(days=2), 6, 7))
# recordatorios personales del administrador
for titulo, msg, dia in [("Cierre contable de octubre", "Revisar facturas y conciliar pagos de PSE y Nequi con el banco.", date(2026, 11, 4)),
                         ("Reunión con asociación de lulo de Pijao", "Negociar precio de finca para la cosecha de diciembre.", date(2026, 11, 6)),
                         ("Renovar póliza del furgón refrigerado", "Vence el SOAT del vehículo de Transportes Refrigerados del Café.", date(2026, 11, 12)),
                         ("Entrega del proyecto final", "Sustentación del sistema FrutiMarket BD ante el docente.", date(2026, 11, 20))]:
    notifs.append([len(notifs) + 1, 1, "RECORDATORIO", titulo, msg, datetime.combine(dia, datetime.min.time()) + timedelta(hours=8), False,
                   datetime(2026, 10, 28, 9, 15)])

# ---------------------------------------------------------------------------
# Escritura
# ---------------------------------------------------------------------------
cab = """-- =====================================================================
-- Datos de ejemplo de FrutiMarket BD (generados por database/generador/generar_datos.py)
-- Periodo simulado: 2 de enero a 2 de noviembre de 2026. Ejecutar despues de 02_objetos.sql.
-- Los triggers descuentan el inventario de cada lote al insertar los detalles de pedido.
-- =====================================================================
USE frutimarket_bd;
SET NAMES utf8mb4;
START TRANSACTION;
"""
body_inicio = out[:]
out.clear()
insert("usuario", ["id_usuario", "id_rol", "username", "password_hash", "nombre_completo", "email", "telefono", "activo", "fecha_registro", "ultimo_acceso"], usuarios)
seccion_usuarios = out[:]
out.clear()
insert("empleado", ["id_empleado", "id_punto", "id_usuario", "numero_documento", "nombres", "apellidos", "cargo", "salario", "fecha_ingreso", "telefono", "email", "activo"], empleados)
insert("campesino", ["id_campesino", "id_usuario", "id_municipio", "tipo_documento", "numero_documento", "nombres", "apellidos", "telefono", "email", "fecha_vinculacion", "certificado_bpa", "activo"], campesinos)
insert("finca", ["id_finca", "id_campesino", "id_municipio", "nombre", "vereda", "area_hectareas", "altitud_msnm"], fincas)
insert("transportador", ["id_transportador", "numero_documento", "nombre", "telefono", "placa", "tipo_vehiculo", "capacidad_kg", "refrigerado", "activo"], transportadores)
insert("cliente", ["id_cliente", "id_usuario", "id_municipio", "tipo_cliente", "tipo_documento", "numero_documento", "nombre", "telefono", "email", "direccion", "fecha_registro", "activo"], clientes)
insert("suscripcion", ["id_suscripcion", "id_cliente", "id_plan", "fecha_inicio", "fecha_fin", "estado", "renovacion_automatica"], suscripciones)
insert("lote", ["id_lote", "codigo", "id_finca", "id_producto", "id_punto", "fecha_cosecha", "fecha_vencimiento", "cantidad_kg", "cantidad_disponible_kg", "precio_finca_kg", "calidad", "estado"], lotes)
insert("pedido", ["id_pedido", "id_cliente", "id_punto", "id_suscripcion", "fecha_pedido", "fecha_entrega_programada", "direccion_entrega", "estado", "subtotal", "descuento", "iva", "total", "observaciones"], pedidos)
insert("detalle_pedido", ["id_pedido", "linea", "id_lote", "cantidad_kg", "precio_kg", "iva_pct", "subtotal"], detalles, lote=400)
insert("pago", ["id_pago", "id_metodo", "id_pedido", "id_suscripcion", "fecha_pago", "valor", "referencia", "estado"], pagos)
insert("factura", ["id_factura", "id_pedido", "numero", "fecha_emision", "subtotal", "descuento", "iva", "total"], facturas)
insert("despacho", ["id_despacho", "id_pedido", "id_transportador", "numero_guia", "fecha_salida", "fecha_entrega", "estado", "temperatura_c", "observaciones"], despachos)
insert("notificacion", ["id_notificacion", "id_usuario", "tipo", "titulo", "mensaje", "fecha_programada", "leida", "fecha_creacion"], notifs)
resto = out[:]

pie = """
-- Lotes que vencieron con existencias quedan marcados como VENCIDO
UPDATE lote SET estado = 'VENCIDO' WHERE estado = 'DISPONIBLE' AND fecha_vencimiento < CURDATE();
COMMIT;
"""
# orden: municipio, rol (en body_inicio estan municipio, rol, categoria, producto, punto, plan, metodo)
destino = Path(__file__).resolve().parent.parent / "03_datos.sql"
destino.write_text(cab + "\n".join(body_inicio + seccion_usuarios + resto) + pie, encoding="utf-8")

# verificacion: lo que calculo el script debe coincidir con lo que dejen los triggers
check = [(l[0], disp[l[0]]) for l in lotes]
(Path(__file__).resolve().parent / "inventario_esperado.txt").write_text("\n".join(f"{a}\t{b:.1f}" for a, b in check))
print(f"usuarios {len(usuarios)}, empleados {len(empleados)}, campesinos {len(campesinos)}, fincas {len(fincas)}, clientes {len(clientes)}")
print(f"lotes {len(lotes)}, pedidos {len(pedidos)}, detalles {len(detalles)}, pagos {len(pagos)}, facturas {len(facturas)}, despachos {len(despachos)}, suscripciones {len(suscripciones)}, notificaciones {len(notifs)}")
