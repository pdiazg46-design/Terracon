#!/usr/bin/env python3
"""
gestor_proveedores.py
Gestión del Maestro de Proveedores y Contratistas para Terracon Energy.
"""

import os
import sys
import json

# Asegurar codificación UTF-8 en consola de Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROVEEDORES_FILE = os.path.join(BASE_DIR, "datos", "proveedores.json")

def cargar_proveedores():
    if not os.path.exists(PROVEEDORES_FILE):
        return []
    with open(PROVEEDORES_FILE, mode='r', encoding='utf-8') as f:
        data = json.load(f)
        return data.get("proveedores", [])

def guardar_proveedores(proveedores):
    with open(PROVEEDORES_FILE, mode='w', encoding='utf-8') as f:
        json.dump({"proveedores": proveedores}, f, ensure_ascii=False, indent=2)

def listar_proveedores_consola():
    proveedores = cargar_proveedores()
    print("=========================================================")
    print(" TERRACON ENERGY - MAESTRO DE PROVEEDORES Y CONTRATISTAS ")
    print("=========================================================\n")
    
    if not proveedores:
        print("No hay proveedores registrados.")
        return
        
    for p in proveedores:
        print(f"🏭 [{p['id']}] {p['nombre_fantasia']} ({p['razon_social']})")
        print(f"   RUT: {p['rut_empresa']} | Estado: {p['estado_acreditacion']}")
        print(f"   Rubro: {p['rubro_servicio']}")
        print(f"   Contacto: {p['contacto_principal']['nombre']} ({p['contacto_principal']['telefono']} | {p['contacto_principal']['email']})")
        print(f"   Datos de Pago: {p['datos_legales_pago']['banco']} ({p['datos_legales_pago']['tipo_cuenta']} N° {p['datos_legales_pago']['numero_cuenta']})")
        print(f"   Proyectos: {', '.join(p['proyectos_asociados'])}")
        print("-" * 60)

if __name__ == "__main__":
    listar_proveedores_consola()
