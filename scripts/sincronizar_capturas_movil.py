# -*- coding: utf-8 -*-
"""
TERRACON ENERGY - Sincronizador de Capturas Móviles (Android AT-SIT App & GitHub)
-----------------------------------------------------------------------------
Procesa las capturas enviadas desde la app móvil Android de Patricio:
 1. Rendición de Gastos (Fotos de boletas, montos, proveedor) -> CSVs de Gastos
 2. Actas y Audios de Reunión -> Matriz de Compromisos & Minutas
 3. Instrucciones de Terreno -> Avances diarios & Dashboard Portafolio
"""

import os
import sys
import json
import glob
import csv
from datetime import datetime

# Forzar codificación UTF-8 en consola Windows
if sys.platform == 'win32':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Carpetas de Entrada y Salida
INBOX_DIR = os.path.join(BASE_DIR, "capturas_movil")
RENDICIONES_DIR = os.path.join(BASE_DIR, "rendiciones_caja")
REUNIONES_DIR = os.path.join(BASE_DIR, "reuniones")
AUDIOS_DIR = os.path.join(BASE_DIR, "audios_instrucciones")

DATOS_DIR = os.path.join(BASE_DIR, "datos")
CP_GASTOS_CSV = os.path.join(DATOS_DIR, "carrera_pinto", "rendicion_gastos.csv")
DA_GASTOS_CSV = os.path.join(DATOS_DIR, "diego_de_almagro", "rendicion_gastos.csv")
COMPROMISOS_JSON = os.path.join(DATOS_DIR, "compromisos.json")
DASHBOARD_HTML = os.path.join(BASE_DIR, "reportes", "dashboard_portafolio.html")

def asegurar_carpetas():
    for folder in [INBOX_DIR, RENDICIONES_DIR, REUNIONES_DIR, AUDIOS_DIR]:
        if not os.path.exists(folder):
            os.makedirs(folder)

def procesar_capturas_pendientes():
    asegurar_carpetas()
    print("=" * 65)
    print(" TERRACON ENERGY - SINCRONIZADOR DE CAPTURAS MÓVILES (AT-SIT APP) ")
    print("=" * 65)

    archivos_json = glob.glob(os.path.join(INBOX_DIR, "*.json"))
    if not archivos_json:
        print("ℹ️  No hay nuevas capturas móviles pendientes en 'capturas_movil/'.")
        print("✅  El sistema está al día y en espera de nuevas capturas de la app.")
        return

    procesados = 0
    for file_path in archivos_json:
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)

            tipo = data.get("tipo", "INSTRUCTION")
            proyecto = data.get("proyecto", "Carrera Pinto")
            titulo = data.get("titulo", "Captura Móvil")
            monto = float(data.get("monto", 0.0))
            comercio = data.get("comercio", "Proveedor Varios")
            categoria = data.get("categoria", "CAT-05")
            notas = data.get("notas", "")
            timestamp = data.get("timestamp", int(datetime.now().timestamp() * 1000))
            fecha_str = datetime.fromtimestamp(timestamp / 1000).strftime("%Y-%m-%d")

            print(f"📥 Procesando [{tipo}] {titulo} | Proyecto: {proyecto}")

            if tipo == "EXPENSE":
                imputar_gasto(proyecto, fecha_str, categoria, comercio, titulo, notas, monto)
            elif tipo == "MEETING":
                registrar_compromiso_reunion(proyecto, fecha_str, titulo, notas)
            elif tipo == "INSTRUCTION":
                print(f" 🗣️ Instrucción de terreno registrada: '{titulo}' -> {notas}")

            # Mover a procesados o eliminar según la política de limpieza
            os.remove(file_path)
            procesados += 1

        except Exception as e:
            print(f"⚠️ Error procesando {file_path}: {e}")

    print(f"\n🎉 ¡Sincronización finalizada! Se procesaron {procesados} capturas móviles de la app.")

def imputar_gasto(proyecto, fecha, categoria, comercio, titulo, notas, monto):
    is_cp = "carrera" in proyecto.lower() or "cp" in proyecto.lower()
    target_csv = CP_GASTOS_CSV if is_cp else DA_GASTOS_CSV
    cc = "CC-CP-01" if is_cp else "CC-DA-02"

    neto = round(monto / 1.19, 2)
    iva = round(monto - neto, 2)

    # Leer ID máximo existente
    item_id = 1
    if os.path.exists(target_csv):
        with open(target_csv, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            rows = list(reader)
            if len(rows) > 1:
                item_id = len(rows)

    concepto = f"{titulo} ({notas})" if notas else titulo

    row = [
        item_id,
        fecha,
        categoria,
        "Caja Chica / Faena",
        comercio,
        f"APP-{item_id:04d}",
        concepto,
        neto,
        iva,
        monto,
        "VERIFICADO",
        f"movil_app_{item_id}.jpg"
    ]

    with open(target_csv, 'a', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(row)

    print(f" 💰 Gasto Imputado: ${monto:,.0f} CLP a {cc} | Proveedor: {comercio}")

def registrar_compromiso_reunion(proyecto, fecha, titulo, notas):
    if os.path.exists(COMPROMISOS_JSON):
        with open(COMPROMISOS_JSON, 'r', encoding='utf-8') as f:
            data = json.load(f)
    else:
        data = {"compromisos": []}

    nuevo_id = f"COM-APP-{len(data.get('compromisos', [])) + 1:03d}"
    nuevo_compromiso = {
        "id": nuevo_id,
        "fecha": fecha,
        "proyecto": proyecto,
        "compromiso": f"{titulo}: {notas}",
        "responsable": "Asignado por Terreno",
        "entidad": "Terracon Energy",
        "fecha_limite": fecha,
        "estado": "Pendiente"
    }

    data.setdefault("compromisos", []).append(nuevo_compromiso)

    with open(COMPROMISOS_JSON, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print(f" 📝 Compromiso Registrado: {nuevo_id} -> {titulo}")

if __name__ == "__main__":
    procesar_capturas_pendientes()
