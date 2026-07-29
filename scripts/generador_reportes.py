#!/usr/bin/env python3
"""
generador_reportes.py
Herramienta de Consolidación de Reportabilidad Diaria y Semanal para Terracon Energy.
"""

import os
import sys
import csv
import json

# Asegurar codificación UTF-8 en consola de Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATOS_DIR = os.path.join(BASE_DIR, "datos")
PLANTILLAS_DIR = os.path.join(BASE_DIR, "plantillas")
REPORTES_DIR = os.path.join(BASE_DIR, "reportes")

def cargar_csv(path_file):
    if not os.path.exists(path_file):
        return []
    with open(path_file, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        return list(reader)

def resumir_avance(datos):
    resumen = []
    for fila in datos:
        partida = fila.get("partida", "")
        plan = float(fila.get("plan_dia", 0))
        real = float(fila.get("real_dia", 0))
        acum = float(fila.get("acumulado_real", 0))
        meta = float(fila.get("meta_total", 1))
        pct = (acum / meta * 100.0) if meta > 0 else 0.0
        
        resumen.append({
            "partida": partida,
            "plan_dia": plan,
            "real_dia": real,
            "acumulado": acum,
            "meta": meta,
            "pct": round(pct, 2)
        })
    return resumen

def generar_reporte_consola():
    print("=========================================================")
    print(" TERRACON ENERGY - CONSOLIDADOR DE TERRENO (PORTAFOLIO) ")
    print("=========================================================\n")
    
    # Carrera Pinto
    cp_csv = os.path.join(DATOS_DIR, "carrera_pinto", "avance_diario.csv")
    cp_datos = cargar_csv(cp_csv)
    cp_resumen = resumir_avance(cp_datos)
    
    print("--- ⚡ CARRERA PINTO (CC-CP-01) ---")
    for r in cp_resumen:
        print(f" • {r['partida']}: {r['acumulado']} / {r['meta']} ({r['pct']}%) | Real Día: {r['real_dia']} (Plan: {r['plan_dia']})")
    
    # Diego de Almagro
    da_csv = os.path.join(DATOS_DIR, "diego_de_almagro", "avance_diario.csv")
    da_datos = cargar_csv(da_csv)
    da_resumen = resumir_avance(da_datos)
    
    print("\n--- 🛠️ DIEGO DE ALMAGRO (CC-DA-02) ---")
    for r in da_resumen:
        print(f" • {r['partida']}: {r['acumulado']} / {r['meta']} ({r['pct']}%) | Real Día: {r['real_dia']} (Plan: {r['plan_dia']})")

if __name__ == "__main__":
    generar_reporte_consola()
