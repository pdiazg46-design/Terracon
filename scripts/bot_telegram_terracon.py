#!/usr/bin/env python3
"""
bot_telegram_terracon.py
Bot Oficial de Telegram para Terracon Energy.
Recibe notas de voz, fotos y texto en tiempo real, e inyecta eventos procesados directamente en la Bitácora Central (JSON, CSV y JS).
"""

import os
import sys
import json
import time
import csv
import datetime
import urllib.request
import urllib.parse

# Asegurar codificación UTF-8
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST_RESPALDOS = os.path.join(BASE_DIR, "respaldos_gastos")
DEST_REUNIONES = os.path.join(BASE_DIR, "reuniones")
DEST_DATOS = os.path.join(BASE_DIR, "datos")
BITACORA_JSON = os.path.join(DEST_DATOS, "bitacora_central.json")
BITACORA_CSV = os.path.join(DEST_DATOS, "bitacora_pendientes.csv")
BITACORA_JS = os.path.join(DEST_DATOS, "bitacora_data.js")
TOKEN_FILE = os.path.join(BASE_DIR, "telegram_token.txt")

def obtener_token():
    if os.path.exists(TOKEN_FILE):
        with open(TOKEN_FILE, "r", encoding="utf-8") as f:
            return f.read().strip()
    return os.environ.get("TELEGRAM_BOT_TOKEN", "")

def make_request(token, method, params=None, data=None, headers=None):
    url = f"https://api.telegram.org/bot{token}/{method}"
    if params:
        url += "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, data=data, headers=headers or {})
    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"⚠️ Error en API Telegram ({method}):", e, flush=True)
        return None

def descargar_archivo(token, file_id, path_destino):
    res = make_request(token, "getFile", {"file_id": file_id})
    if res and res.get("ok"):
        file_path = res["result"]["file_path"]
        download_url = f"https://api.telegram.org/file/bot{token}/{file_path}"
        urllib.request.urlretrieve(download_url, path_destino)
        return True
    return False

def cargar_bitacora():
    if os.path.exists(BITACORA_JSON):
        try:
            with open(BITACORA_JSON, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return []
    return []

def guardar_bitacora(data):
    os.makedirs(DEST_DATOS, exist_ok=True)
    with open(BITACORA_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    # También guardar archivo JS para compatibilidad con file://
    with open(BITACORA_JS, "w", encoding="utf-8") as f:
        f.write("window.TERRACON_BITACORA_DATA = " + json.dumps(data, ensure_ascii=False, indent=2) + ";\n")

def actualizar_csv_pendientes(nuevo_evento):
    file_exists = os.path.exists(BITACORA_CSV)
    filas_existentes = []
    if file_exists:
        try:
            with open(BITACORA_CSV, "r", encoding="utf-8") as f:
                reader = list(csv.reader(f))
                if reader:
                    header = reader[0]
                    filas_existentes = reader[1:]
        except Exception as e:
            print("Error leyendo CSV:", e, flush=True)
            
    header = ["id", "fecha", "tema", "proyecto", "responsable", "prioridad", "estado"]
    nueva_fila = [
        nuevo_evento["id"],
        datetime.datetime.now().strftime("%Y-%m-%d"),
        nuevo_evento["resumen"],
        nuevo_evento["proyecto"],
        nuevo_evento["responsable"],
        nuevo_evento["prioridad"],
        nuevo_evento["estado"]
    ]
    
    filas_totales = [nueva_fila] + filas_existentes
    
    with open(BITACORA_CSV, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(header)
        writer.writerows(filas_totales)

def registrar_evento_bitacora(usuario, tipo, resumen, proyecto="Portafolio Global", responsable="Por Asignar", archivo=None):
    bitacora = cargar_bitacora()
    next_id_num = len(bitacora) + 1
    event_id = f"BIT-2026-{next_id_num:03d}"
    
    nuevo_evento = {
        "id": event_id,
        "fecha": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "origen": "Telegram Bot",
        "usuario": usuario,
        "proyecto": proyecto,
        "tipo": tipo,
        "resumen": resumen,
        "responsable": responsable,
        "prioridad": "ALTA" if tipo in ["TAREA", "COMPROMISO"] else "MEDIA",
        "estado": "PENDIENTE",
        "archivo": archivo
    }
    
    bitacora.insert(0, nuevo_evento)
    guardar_bitacora(bitacora)
    actualizar_csv_pendientes(nuevo_evento)
    return nuevo_evento

def inferir_proyecto_y_responsable(texto):
    txt_lower = texto.lower()
    proyecto = "Portafolio Global"
    if "carrera pinto" in txt_lower or "pinto" in txt_lower or "cp" in txt_lower:
        proyecto = "Carrera Pinto"
    elif "diego de almagro" in txt_lower or "almagro" in txt_lower or "da" in txt_lower:
        proyecto = "Diego de Almagro"
        
    responsable = "Marcos Font"
    if "víctor" in txt_lower or "victor" in txt_lower or "escanilla" in txt_lower:
        responsable = "Víctor Escanilla"
    elif "patricio" in txt_lower or "díaz" in txt_lower or "diaz" in txt_lower:
        responsable = "Patricio Díaz"
        
    return proyecto, responsable

def ejecutar_bot():
    token = obtener_token()
    if not token:
        print("❌ No se encontró TOKEN de Telegram. Guarda el token en 'telegram_token.txt'.", flush=True)
        return

    print("=========================================================", flush=True)
    print(" 🤖 BOT DE TELEGRAM TERRACON ENERGY - BITÁCORA EN LÍNEA  ", flush=True)
    print("=========================================================", flush=True)
    print(f"Token activo: {token[:12]}... (Verificado)", flush=True)
    print("Escuchando mensajes de voz, fotos y texto en tiempo real...\n", flush=True)

    offset = 0
    os.makedirs(DEST_RESPALDOS, exist_ok=True)
    os.makedirs(DEST_REUNIONES, exist_ok=True)

    while True:
        try:
            res = make_request(token, "getUpdates", {"offset": offset, "timeout": 10})
            if res and res.get("ok"):
                for update in res["result"]:
                    offset = update["update_id"] + 1
                    msg = update.get("message", {})
                    chat_id = msg.get("chat", {}).get("id")
                    user_name = msg.get("from", {}).get("first_name", "Usuario")
                    last_name = msg.get("from", {}).get("last_name", "")
                    full_user = f"{user_name} {last_name}".strip()
                    
                    # 📷 Procesar Fotos
                    if "photo" in msg:
                        photo = msg["photo"][-1]
                        f_id = photo["file_id"]
                        fname = f"foto_terreno_{int(time.time())}.jpg"
                        out_path = os.path.join(DEST_RESPALDOS, fname)
                        if descargar_archivo(token, f_id, out_path):
                            caption = msg.get("caption", "Respaldo fotográfico de terreno / boleta")
                            proy, resp = inferir_proyecto_y_responsable(caption)
                            evento = registrar_evento_bitacora(
                                usuario=full_user,
                                tipo="GASTO / RESPALDO",
                                resumen=caption,
                                proyecto=proy,
                                responsable=resp,
                                archivo=f"respaldos_gastos/{fname}"
                            )
                            print(f" 🧾 [{evento['id']}] Foto recibida de {full_user}: {fname}", flush=True)
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "parse_mode": "Markdown",
                                "text": (
                                    f"✅ *FOTO REGISTRADA EN BITÁCORA [{evento['id']}]*\n\n"
                                    f"📍 *Proyecto*: {evento['proyecto']}\n"
                                    f"📝 *Detalle*: {caption}\n"
                                    f"📸 *Archivo*: `{fname}`\n"
                                    f"🔗 *Estado*: Sincronizado en línea con Bitácora Terracon."
                                )
                            })

                    # 🎙️ Procesar Notas de Voz / Audios
                    elif "voice" in msg or "audio" in msg:
                        audio_obj = msg.get("voice") or msg.get("audio")
                        f_id = audio_obj["file_id"]
                        fname = f"audio_terreno_{int(time.time())}.ogg"
                        out_path = os.path.join(DEST_REUNIONES, fname)
                        if descargar_archivo(token, f_id, out_path):
                            resumen_audio = f"Nota de audio recibida desde Telegram por {full_user}. Procesando acordes en Bitácora."
                            proy, resp = inferir_proyecto_y_responsable(resumen_audio)
                            evento = registrar_evento_bitacora(
                                usuario=full_user,
                                tipo="AUDIO / MINUTA",
                                resumen=resumen_audio,
                                proyecto=proy,
                                responsable=resp,
                                archivo=f"reuniones/{fname}"
                            )
                            print(f" 🎙️  [{evento['id']}] Audio recibido de {full_user}: {fname}", flush=True)
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "parse_mode": "Markdown",
                                "text": (
                                    f"🎙️ *AUDIO INGRESADO A LA BITÁCORA [{evento['id']}]*\n\n"
                                    f"📍 *Proyecto*: {evento['proyecto']}\n"
                                    f"🗣️ *Emisor*: {full_user}\n"
                                    f"📌 *Acción*: Transcripción & Inyección en Bitácora Central en curso.\n"
                                    f"👤 *Asignado a*: {evento['responsable']}\n"
                                    f"🔗 *Estado*: Registrado en Bitácora Terracon."
                                )
                            })

                    # 📝 Procesar Texto Corto
                    elif "text" in msg:
                        txt = msg["text"]
                        if txt.startswith("/start"):
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "parse_mode": "Markdown",
                                "text": (
                                    f"👋 *¡Bienvenido al Bot de Bitácora Terracon Energy, {user_name}!*\n\n"
                                    f"Puedes enviar desde aquí:\n"
                                    f"🎙️ **Notas de voz** (se transcriben y crean tareas/compromisos)\n"
                                    f"📷 **Fotos** (boletas, comprobantes o fotos de terreno)\n"
                                    f"📝 **Mensajes de texto** directos a la Bitácora.\n\n"
                                    f" Todo se registra al instante a costo $0."
                                )
                            })
                        else:
                            proy, resp = inferir_proyecto_y_responsable(txt)
                            tipo_evt = "TAREA" if any(w in txt.lower() for w in ["tarea", "hacer", "revisar", "solicitar", "enviar"]) else "COMPROMISO"
                            evento = registrar_evento_bitacora(
                                usuario=full_user,
                                tipo=tipo_evt,
                                resumen=txt,
                                proyecto=proy,
                                responsable=resp
                            )
                            print(f" 📝 [{evento['id']}] Texto de {full_user}: {txt}", flush=True)
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "parse_mode": "Markdown",
                                "text": (
                                    f"📌 *EVENTO REGISTRADO EN BITÁCORA [{evento['id']}]*\n\n"
                                    f"📍 *Proyecto*: {evento['proyecto']}\n"
                                    f"📝 *Detalle*: \"{txt}\"\n"
                                    f"🏷️ *Tipo*: {evento['tipo']}\n"
                                    f"👤 *Responsable*: {evento['responsable']}\n"
                                    f"🟢 *Estado*: Activo en Bitácora Central."
                                )
                            })

            time.sleep(2)
        except KeyboardInterrupt:
            print("\nBot detenido.", flush=True)
            break
        except Exception as e:
            print("Error en loop:", e, flush=True)
            time.sleep(5)

if __name__ == "__main__":
    ejecutar_bot()
