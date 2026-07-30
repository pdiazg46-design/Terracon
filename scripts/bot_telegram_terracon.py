#!/usr/bin/env python3
"""
bot_telegram_terracon.py
Bot Oficial de Telegram para Terracon Energy.
Recibe notas de voz, fotos y texto en tiempo real.
Permite ingresar Y GESTIONAR (completar, reasignar) tareas desde Telegram o la Web.
"""

import os
import sys
import json
import time
import csv
import datetime
import urllib.request
import urllib.parse
import soundfile as sf
import speech_recognition as sr

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

def transcribir_audio_ogg(path_ogg):
    temp_wav = path_ogg + ".temp.wav"
    try:
        data, samplerate = sf.read(path_ogg)
        sf.write(temp_wav, data, samplerate)
        
        r = sr.Recognizer()
        with sr.AudioFile(temp_wav) as source:
            audio_data = r.record(source)
            texto = r.recognize_google(audio_data, language='es-CL')
            
        if os.path.exists(temp_wav):
            os.remove(temp_wav)
        return texto
    except Exception as e:
        print(f"⚠️ Error en transcripción de audio: {e}", flush=True)
        if os.path.exists(temp_wav):
            os.remove(temp_wav)
        return "Audio grabado recibido."

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
    
    with open(BITACORA_JS, "w", encoding="utf-8") as f:
        f.write("window.TERRACON_BITACORA_DATA = " + json.dumps(data, ensure_ascii=False, indent=2) + ";\n")

def cambiar_estado_tarea(event_id, nuevo_estado="COMPLETADO"):
    bitacora = cargar_bitacora()
    encontrado = False
    for item in bitacora:
        if event_id.lower() in item.get("id", "").lower() or event_id in str(item.get("id", "")):
            item["estado"] = nuevo_estado
            encontrado = item
            break
    if encontrado:
        guardar_bitacora(bitacora)
    return encontrado

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
        print("❌ No se encontró TOKEN de Telegram.", flush=True)
        return

    print("=========================================================", flush=True)
    print(" 🤖 BOT DE TELEGRAM TERRACON ENERGY - BITÁCORA EN LÍNEA  ", flush=True)
    print("=========================================================", flush=True)

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
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "parse_mode": "Markdown",
                                "text": f"✅ *FOTO REGISTRADA EN BITÁCORA [{evento['id']}]*\n📍 *Proyecto*: {evento['proyecto']}\n📝 *Detalle*: {caption}"
                            })

                    # 🎙️ Procesar Audios
                    elif "voice" in msg or "audio" in msg:
                        audio_obj = msg.get("voice") or msg.get("audio")
                        f_id = audio_obj["file_id"]
                        fname = f"audio_terreno_{int(time.time())}.ogg"
                        out_path = os.path.join(DEST_REUNIONES, fname)
                        if descargar_archivo(token, f_id, out_path):
                            texto_transcrito = transcribir_audio_ogg(out_path)
                            proy, resp = inferir_proyecto_y_responsable(texto_transcrito)
                            
                            resumen_final = f"Audio transcrito: \"{texto_transcrito}\""
                            evento = registrar_evento_bitacora(
                                usuario=full_user,
                                tipo="TAREA / AUDIO",
                                resumen=resumen_final,
                                proyecto=proy,
                                responsable=resp,
                                archivo=f"reuniones/{fname}"
                            )
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "parse_mode": "Markdown",
                                "text": (
                                    f"🎙️ *AUDIO TRANSCRIBIDO Y REGISTRADO [{evento['id']}]*\n\n"
                                    f"🗣️ *Voz de*: {full_user}\n"
                                    f"📝 *Texto Transcrito*: \"{texto_transcrito}\"\n"
                                    f"📍 *Proyecto*: {evento['proyecto']}\n"
                                    f"👤 *Responsable*: {evento['responsable']}"
                                )
                            })

                    # 📝 Procesar Texto & Comandos de Gestión
                    elif "text" in msg:
                        txt = msg["text"].strip()
                        
                        # Comandos de gestión: /completar BIT-005 o /resuelto 5
                        if txt.lower().startswith("/completar") or txt.lower().startswith("/resuelto"):
                            partes = txt.split()
                            if len(partes) > 1:
                                target_id = partes[1]
                                item = cambiar_estado_tarea(target_id, "COMPLETADO")
                                if item:
                                    make_request(token, "sendMessage", {
                                        "chat_id": chat_id,
                                        "parse_mode": "Markdown",
                                        "text": f"🟢 *TAREA MARCADA COMO RESUELTA [{item['id']}]*\n📝 *Detalle*: {item['resumen']}\n👤 *Responsable*: {item['responsable']}"
                                    })
                                else:
                                    make_request(token, "sendMessage", {"chat_id": chat_id, "text": f"⚠️ No se encontró la tarea '{target_id}'."})
                        elif txt.startswith("/start"):
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "parse_mode": "Markdown",
                                "text": (
                                    f"👋 *¡Bienvenido al Bot de Bitácora Terracon Energy!*\n\n"
                                    f"📌 **Crear tareas**: Envía voz, foto o texto.\n"
                                    f"✅ **Resolver tareas**: Escribe `/completar 5` o `/resuelto BIT-2026-005`."
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
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "parse_mode": "Markdown",
                                "text": f"📌 *EVENTO REGISTRADO [{evento['id']}]*\n📝 *Detalle*: \"{txt}\"\n👤 *Responsable*: {evento['responsable']}"
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
