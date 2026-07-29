#!/usr/bin/env python3
"""
procesar_sincronizacion_nube.py
Procesador de archivos sincronizados desde la nube móvil para Terracon Energy.
Vincula automáticamente fotos de comprobantes con notas de voz según timestamp o nombre.
"""

import os
import sys
import shutil
from datetime import datetime

# Asegurar codificación UTF-8 en consola de Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
USER_PROFILE = os.environ.get("USERPROFILE", "C:\\Users\\pdiaz")

# Carpeta sincronizada de OneDrive
ONEDRIVE_DIR = os.path.join(USER_PROFILE, "OneDrive", "Terracon_Terreno")
ONEDRIVE_AUDIOS = os.path.join(ONEDRIVE_DIR, "audios_reuniones")
ONEDRIVE_FOTOS = os.path.join(ONEDRIVE_DIR, "fotos_respaldos")
ONEDRIVE_NOTAS = os.path.join(ONEDRIVE_DIR, "notas_avance")

# Carpeta local alternativa
LOCAL_DIR = os.path.join(BASE_DIR, "datos_terreno_sincronizados")
LOCAL_AUDIOS = os.path.join(LOCAL_DIR, "audios_reuniones")
LOCAL_FOTOS = os.path.join(LOCAL_DIR, "fotos_respaldos")
LOCAL_NOTAS = os.path.join(LOCAL_DIR, "notas_avance")

# Destinos finales en el proyecto
DEST_REUNIONES = os.path.join(BASE_DIR, "reuniones")
DEST_RESPALDOS = os.path.join(BASE_DIR, "respaldos_gastos")
DEST_DATOS = os.path.join(BASE_DIR, "datos")

def inicializar():
    for d in [ONEDRIVE_AUDIOS, ONEDRIVE_FOTOS, ONEDRIVE_NOTAS, LOCAL_AUDIOS, LOCAL_FOTOS, LOCAL_NOTAS, DEST_REUNIONES, DEST_RESPALDOS, DEST_DATOS]:
        os.makedirs(d, exist_ok=True)

def obtener_archivos_con_mtime(directorio):
    archivos = []
    if os.path.exists(directorio):
        for f in os.listdir(directorio):
            p = os.path.join(directorio, f)
            if os.path.isfile(p):
                archivos.append({
                    "nombre": f,
                    "path": p,
                    "mtime": os.path.getmtime(p)
                })
    return archivos

def procesar_y_vincular():
    inicializar()
    print("=========================================================")
    print(" TERRACON ENERGY - PROCESADOR Y VINCULADOR MÓVIL         ")
    print("=========================================================\n")
    
    # Recolectar de OneDrive y Local
    audios = obtener_archivos_con_mtime(ONEDRIVE_AUDIOS) + obtener_archivos_con_mtime(LOCAL_AUDIOS)
    fotos = obtener_archivos_con_mtime(ONEDRIVE_FOTOS) + obtener_archivos_con_mtime(LOCAL_FOTOS)
    notas = obtener_archivos_con_mtime(ONEDRIVE_NOTAS) + obtener_archivos_con_mtime(LOCAL_NOTAS)

    # 1. Vincular fotos con audios/notas por cercanía de tiempo (menos de 10 minutos de diferencia)
    for foto in fotos:
        foto_base = os.path.splitext(foto["nombre"])[0]
        audio_asociado = None
        
        # Buscar por mismo nombre base o tiempo cercano
        for audio in audios:
            audio_base = os.path.splitext(audio["nombre"])[0]
            diferencia_seg = abs(foto["mtime"] - audio["mtime"])
            if foto_base == audio_base or diferencia_seg < 600: # 10 min
                audio_asociado = audio
                break

        if audio_asociado:
            print(f" 🔗 VÍNCULO DETECTADO:")
            print(f"    - Foto: {foto['nombre']}")
            print(f"    - Audio Indicación: {audio_asociado['nombre']}")
        else:
            print(f" 🧾 Foto de respaldo procesada (sin audio asociado): {foto['nombre']}")

        # Mover foto a respaldos_gastos
        dst_foto = os.path.join(DEST_RESPALDOS, foto["nombre"])
        shutil.move(foto["path"], dst_foto)

    # Mover audios a reuniones
    for audio in audios:
        dst_audio = os.path.join(DEST_REUNIONES, audio["nombre"])
        if os.path.exists(audio["path"]):
            shutil.move(audio["path"], dst_audio)
            print(f" 🎙️  Audio guardado en reuniones: {audio['nombre']}")

    print("\n✅ Proceso completado con éxito.")

if __name__ == "__main__":
    procesar_y_vincular()
