#!/usr/bin/env python3
"""
gestor_inversores.py
Gestión de Trazabilidad y Bitácora de Hitos Granulares por Inversor String (1 a 13) por Planta.
"""

import os
import sys
import json

# Asegurar codificación UTF-8 en consola de Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INVERSORES_FILE = os.path.join(BASE_DIR, "datos", "inversores_detalle.json")

def cargar_inversores():
    if not os.path.exists(INVERSORES_FILE):
        return []
    with open(INVERSORES_FILE, mode='r', encoding='utf-8') as f:
        data = json.load(f)
        return data.get("plantas", [])

def reportar_bitacora_consola():
    plantas = cargar_inversores()
    print("=========================================================")
    print(" TERRACON ENERGY - BITÁCORA DE HITOS POR INVERSOR STRING ")
    print("=========================================================\n")
    
    for p in plantas:
        print(f"⚡ PLANTA SOLAR: {p['proyecto']} ({p['centro_costo']}) - {p['mvs_id']}")
        print(f"   Total Inversores a Instalar: {p['total_inversores']} Unidades")
        print("-" * 60)
        
        for inv in p["inversores"]:
            h = inv["hitos"]
            dc_info = h.get("cables_solares_dc", {})
            mc4_info = h.get("conectorizacion_mc4", {})
            bt_info = h.get("triadas_bt_mvs", {})
            qaqc_info = h.get("protocolo_qaqc", {})
            
            print(f" 🔌 [{inv['id']}] {inv['nombre']} ({inv['posicion']})")
            print(f"    • Estado General: {inv['estado_general']} ({inv['pct_avance']}%)")
            print(f"    • Montaje Mecánico: {h['montaje_mecanico']['estado']} ({h['montaje_mecanico']['fecha'] or 'S/F'})")
            print(f"    • Cables Solares DC (Rojo + / Negro -): {dc_info.get('strings_listos', 0)}/{dc_info.get('total_strings', 12)} Strings ({dc_info.get('estado')})")
            print(f"    • Conectorización MC4 DC: {mc4_info.get('conectores_listos', 0)}/{mc4_info.get('total_conectores', 24)} Conectores ({mc4_info.get('estado')})")
            print(f"    • Tríadas BT a MVS: {bt_info.get('estado')}")
            print(f"    • Pruebas Megado/Polaridad: {h['pruebas_megado_polaridad']['estado']}")
            print(f"    • Protocolo QA/QC: {qaqc_info.get('estado')}")
            print(" " * 4)

if __name__ == "__main__":
    reportar_bitacora_consola()
