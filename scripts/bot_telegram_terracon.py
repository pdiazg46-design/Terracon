#!/usr/bin/env python3
"""
bot_telegram_terracon.py
Bot de Telegram para el registro de terreno sin fricción (Terracon Energy).
Recibe fotos, audios de voz y texto desde el celular y los imputa en el proyecto.
"""

import os
import sys
import json
import time
import urllib.request
import urllib.parse

# Asegurar codificación UTF-8
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST_RESPALDOS = os.path.join(BASE_DIR, "respaldos_gastos")
DEST_REUNIONES = os.path.join(BASE_DIR, "reuniones")
DEST_DATOS = os.path.join(BASE_DIR, "datos")

# Cargar Token desde variable o archivo de configuración
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
        print(f"Error en API Telegram ({method}):", e)
        return None

def descargar_archivo(token, file_id, path_destino):
    res = make_request(token, "getFile", {"file_id": file_id})
    if res and res.get("ok"):
        file_path = res["result"]["file_path"]
        download_url = f"https://api.telegram.org/file/bot{token}/{file_path}"
        urllib.request.urlretrieve(download_url, path_destino)
        return True
    return False

def ejecutar_bot():
    token = obtener_token()
    if not token:
        print("⚠️ No se encontró TOKEN de Telegram. Guarda el token en 'telegram_token.txt'.")
        print("Para obtener un Token gratuito de Telegram:")
        print("1. En Telegram busca a @BotFather")
        print("2. Escribe /newbot y ponle nombre (ej: Terracon_Faena_Bot)")
        print("3. Copia el TOKEN que te entregue y pégalo en el archivo 'telegram_token.txt'.")
        return

    print("=========================================================")
    print(" BOT DE TELEGRAM TERRACON ENERGY INICIADO Y EN ESCUCHA ")
    print("=========================================================")
    print("📱 Puedes enviar fotos, audios de voz o texto desde tu celular.")
    print("Presiona Ctrl+C para detener.\n")

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
                    
                    # 📷 Procesar Fotos
                    if "photo" in msg:
                        photo = msg["photo"][-1]
                        f_id = photo["file_id"]
                        fname = f"foto_terreno_{int(time.time())}.jpg"
                        out_path = os.path.join(DEST_RESPALDOS, fname)
                        if descargar_archivo(token, f_id, out_path):
                            caption = msg.get("caption", "Sin descripción")
                            print(f" 🧾 Foto recibida de {user_name}: {fname} | Nota: {caption}")
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "text": f"✅ Foto recibida y guardada en respaldos.\nNota: {caption}"
                            })

                    # 🎙️ Procesar Notas de Voz / Audios
                    elif "voice" in msg or "audio" in msg:
                        audio_obj = msg.get("voice") or msg.get("audio")
                        f_id = audio_obj["file_id"]
                        fname = f"audio_terreno_{int(time.time())}.ogg"
                        out_path = os.path.join(DEST_REUNIONES, fname)
                        if descargar_archivo(token, f_id, out_path):
                            print(f" 🎙️  Audio recibido de {user_name}: {fname}")
                            make_request(token, "sendMessage", {
                                "chat_id": chat_id,
                                "text": f"✅ Audio grabado recibido correctamente. Registrado en el sistema."
                            })

                    # 📝 Procesar Texto Corto
                    elif "text" in msg:
                        txt = msg["text"]
                        print(f" 📝 Mensaje de {user_name}: {txt}")
                        make_request(token, "sendMessage", {
                            "chat_id": chat_id,
                            "text": f"👍 Recibido: \"{txt}\". Procesando en el portafolio Terracon."
                        })

            time.sleep(2)
        except KeyboardInterrupt:
            print("\nBot detenido.")
            break
        except Exception as e:
            print("Error en loop:", e)
            time.sleep(5)

if __name__ == "__main__":
    ejecutar_bot()
