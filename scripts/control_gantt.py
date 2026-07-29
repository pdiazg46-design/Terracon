#!/usr/bin/env python3
"""
control_gantt.py
Calculador de SPI, Estado de Ruta Crítica y Avance de Carta Gantt para Terracon Energy.
"""

import os
import sys
import json

# Asegurar codificación UTF-8 en consola de Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATOS_DIR = os.path.join(BASE_DIR, "datos")

def evaluar_gantt_proyecto(dir_nombre):
    json_path = os.path.join(DATOS_DIR, dir_nombre, "gantt_linea_base.json")
    if not os.path.exists(json_path):
        return None
    
    with open(json_path, mode='r', encoding='utf-8') as f:
        data = json.load(f)
        
    tareas = data.get("tareas", [])
    total_tareas = len(tareas)
    completadas = 0
    suma_avance = 0.0
    alertas_ruta_critica = []
    
    for t in tareas:
        avance = float(t.get("avance_real_pct", 0))
        suma_avance += avance
        if avance == 100:
            completadas += 1
        elif t.get("es_ruta_critica", False) and avance < 100:
            alertas_ruta_critica.append(t["nombre"])
            
    pct_promedio = (suma_avance / total_tareas) if total_tareas > 0 else 0.0
    
    return {
        "proyecto": data.get("proyecto", dir_nombre),
        "centro_costo": data.get("centro_costo", ""),
        "inicio_contrato": data.get("fecha_inicio_contrato", ""),
        "fin_contrato": data.get("fecha_fin_contrato", ""),
        "total_tareas": total_tareas,
        "completadas": completadas,
        "pct_avance_real": round(pct_promedio, 2),
        "alertas_ruta_critica": alertas_ruta_critica
    }

def reporte_gantt_portafolio():
    print("=========================================================")
    print(" TERRACON ENERGY - CONTROL DE CARTA GANTT Y RUTA CRITICA ")
    print("=========================================================\n")
    
    for dir_n in ["carrera_pinto", "diego_de_almagro"]:
        res = evaluar_gantt_proyecto(dir_n)
        if not res:
            continue
            
        print(f"📅 Proyecto: {res['proyecto']} ({res['centro_costo']})")
        print(f" • Periodo Contractual: {res['inicio_contrato']} ➔ {res['fin_contrato']}")
        print(f" • Avance Físico Promedio Real: {res['pct_avance_real']}%")
        print(f" • Tareas Completadas: {res['completadas']} / {res['total_tareas']}")
        if res['alertas_ruta_critica']:
            print(" ⚠️ Tareas Pendientes en RUTA CRÍTICA:")
            for t_name in res['alertas_ruta_critica']:
                print(f"    - [ ] {t_name}")
        else:
            print(" ✅ Ruta Crítica en regla y al día.")
        print("-" * 55)

if __name__ == "__main__":
    reporte_gantt_portafolio()
