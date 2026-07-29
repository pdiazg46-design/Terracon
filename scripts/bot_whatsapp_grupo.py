#!/usr/bin/env python3
"""
bot_whatsapp_grupo.py
Monitoreo exclusivo de archivos y notas de voz recibidos del grupo de WhatsApp 'Terracon Faena'.
"""

import os
import sys
import shutil

# Asegurar codificación UTF-8
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
USER_PROFILE = os.environ.get("USERPROFILE", "C:\\Users\\pdiaz")

# Directorios de Monitoreo Exclusivos de Terreno
DIR_ONEDRIVE = os.path.join(USER_PROFILE, "OneDrive", "Terracon_Terreno")
DIR_NOTAS = os.path.join(DIR_ONEDRIVE, "notas_avance")
DIR_FOTOS = os.path.join(DIR_ONEDRIVE, "fotos_respaldos")
DIR_AUDIOS = os.path.join(DIR_ONEDRIVE, "audios_reuniones")

DEST_RESPALDOS = os.path.join(BASE_DIR, "respaldos_gastos")
DEST_REUNIONES = os.path.join(BASE_DIR, "reuniones")
DEST_DATOS = os.path.join(BASE_DIR, "datos")

def inicializar():
    for d in [DIR_ONEDRIVE, DIR_NOTAS, DIR_FOTOS, DIR_AUDIOS, DEST_RESPALDOS, DEST_REUNIONES, DEST_DATOS]:
        os.makedirs(d, exist_ok=True)

def procesar_archivos_grupo():
    inicializar()
    print("=========================================================")
    print(" MONITOR DE GRUPO WHATSAPP 'TERRACON FAENA' EN ESCUCHA   ")
    print("=========================================================\n")
    
    archivos_procesados = 0

    # Escanear únicamente las carpetas del canal de faena
    carpetas_a_revisar = [DIR_ONEDRIVE, DIR_FOTOS, DIR_AUDIOS, DIR_NOTAS]
    
    for folder in carpetas_a_revisar:
        if not os.path.exists(folder):
            continue
            
        for f in os.listdir(folder):
            p = os.path.join(folder, f)
            if not os.path.isfile(p):
                continue
                
            ext = os.path.splitext(f)[1].lower()
            
            # Imágenes y PDF de comprobantes
            if ext in [".jpg", ".jpeg", ".png", ".pdf", ".webp"]:
                dst = os.path.join(DEST_RESPALDOS, f)
                shutil.move(p, dst)
                print(f" 🧾 Comprobante importado: {f}")
                archivos_procesados += 1
                
            # Audios y notas de voz
            elif ext in [".opus", ".m4a", ".wav", ".ogg", ".mp3", ".ptt"]:
                dst = os.path.join(DEST_REUNIONES, f)
                shutil.move(p, dst)
                print(f" 🎙️  Nota de voz importada: {f}")
                archivos_procesados += 1

    if archivos_procesados == 0:
        print("⏳ No hay nuevos archivos pendientes en el canal de faena.")
    else:
        print(f"\n✅ Se procesaron {archivos_procesados} archivos nuevos de faena.")

if __name__ == "__main__":
    procesar_archivos_grupo()
