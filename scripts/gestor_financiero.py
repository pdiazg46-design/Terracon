#!/usr/bin/env python3
"""
gestor_financiero.py
Gestión Operativa de Rendiciones de Gastos y Control de OPEX por Centro de Costos para Terracon.
"""

import os
import sys
import csv

# Asegurar codificación UTF-8 en consola de Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATOS_DIR = os.path.join(BASE_DIR, "datos")

def procesar_gastos_proyecto(nombre_proyecto, dir_nombre):
    csv_path = os.path.join(DATOS_DIR, dir_nombre, "rendicion_gastos.csv")
    if not os.path.exists(csv_path):
        return None
    
    total_neto = 0.0
    total_iva = 0.0
    total_general = 0.0
    con_respaldo = 0
    sin_respaldo = 0
    
    categorias = {}
    
    with open(csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for fila in reader:
            cat_nom = fila.get("categoria_nombre", "Otros")
            monto_t = float(fila.get("monto_total", 0))
            monto_n = float(fila.get("monto_neto", 0))
            monto_i = float(fila.get("iva", 0))
            resp = fila.get("respaldado", "No").lower() in ["si", "sí", "yes", "true"]
            
            total_neto += monto_n
            total_iva += monto_i
            total_general += monto_t
            
            if resp:
                con_respaldo += 1
            else:
                sin_respaldo += 1
                
            categorias[cat_nom] = categorias.get(cat_nom, 0.0) + monto_t

    return {
        "proyecto": nombre_proyecto,
        "total_neto": total_neto,
        "total_iva": total_iva,
        "total_general": total_general,
        "con_respaldo": con_respaldo,
        "sin_respaldo": sin_respaldo,
        "categorias": categorias
    }

def reporte_financiero_portafolio():
    print("=========================================================")
    print(" TERRACON ENERGY - CONTROL FINANCIERO Y RENDICION OPEX   ")
    print("=========================================================\n")
    
    cp = procesar_gastos_proyecto("Carrera Pinto (CC-CP-01)", "carrera_pinto")
    da = procesar_gastos_proyecto("Diego de Almagro (CC-DA-02)", "diego_de_almagro")
    
    for proj in [cp, da]:
        if not proj:
            continue
        print(f"💰 {proj['proyecto']}")
        print(f" • Total Acumulado: ${proj['total_general']:,.0f} CLP (Neto: ${proj['total_neto']:,.0f} | IVA: ${proj['total_iva']:,.0f})")
        print(f" • Respaldos: {proj['con_respaldo']} validados | {proj['sin_respaldo']} pendientes")
        print(" • Desglose por Categoría:")
        for cat, monto in proj['categorias'].items():
            print(f"    - {cat}: ${monto:,.0f} CLP")
        print("-" * 55)

if __name__ == "__main__":
    reporte_financiero_portafolio()
