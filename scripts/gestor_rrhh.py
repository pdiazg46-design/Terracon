#!/usr/bin/env python3
"""
gestor_rrhh.py
Gestión del Maestro de Recursos Humanos, Remuneraciones y Acreditaciones Legales (Sonedix) para Terracon.
"""

import os
import sys
import json

# Asegurar codificación UTF-8 en consola de Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RRHH_FILE = os.path.join(BASE_DIR, "datos", "rrhh.json")

def cargar_personal():
    if not os.path.exists(RRHH_FILE):
        return []
    with open(RRHH_FILE, mode='r', encoding='utf-8') as f:
        data = json.load(f)
        return data.get("personal", [])

def listar_rrhh_consola():
    personal = cargar_personal()
    print("=========================================================")
    print(" TERRACON ENERGY - MAESTRO DE RRHH Y ACREDITACIÓN FAENA  ")
    print("=========================================================\n")
    
    if not personal:
        print("No hay personal registrado.")
        return
        
    total_sueldos = 0.0
    for p in personal:
        acred = p.get("acreditacion_legal_sonedix", {})
        estado_acred = acred.get("estado_acreditacion", "Sin Registro")
        sueldo = float(p.get("remuneracion_base_mensual", 0))
        total_sueldos += sueldo
        
        print(f"👤 [{p['id']}] {p['nombre_completo']} ({p['rut']})")
        print(f"   Cargo: {p['cargo']} | Proyecto: {p['proyecto_asignado']}")
        print(f"   Empresa: {p['entidad_empresa']} | Contrato: {p['tipo_contrato']} | Remuneración: ${sueldo:,.0f} CLP")
        print(f"   Formulario Asignado: {p.get('formulario_rendicion_id', 'N/A')}")
        print(f"   Acreditación Sonedix: {estado_acred} (Examen Altura: {'OK' if acred.get('examen_ocupacional_altura') else 'PENDIENTE'})")
        print("-" * 60)

    print(f"\n📊 Total Costo Mensual Planilla Dotación: ${total_sueldos:,.0f} CLP")

if __name__ == "__main__":
    listar_rrhh_consola()
