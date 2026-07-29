#!/usr/bin/env python3
"""
bot_whatsapp_terracon.py
Receptor Oficial de WhatsApp (Meta Cloud API / Twilio) para Terracon Energy.
Procesa imágenes de respaldos, notas de voz de WhatsApp y mensajes de texto sin costo.
"""

import os
import sys
import json
import time
import requests
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import parse_qs, urlparse

# Asegurar codificación UTF-8
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST_RESPALDOS = os.path.join(BASE_DIR, "respaldos_gastos")
DEST_REUNIONES = os.path.join(BASE_DIR, "reuniones")
DEST_DATOS = os.path.join(BASE_DIR, "datos")

CONFIG_FILE = os.path.join(BASE_DIR, "whatsapp_config.json")
PORT = 5000

def cargar_configuracion():
    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {
        "verify_token": "terracon_secret_token_2026",
        "whatsapp_token": "",
        "phone_number_id": ""
    }

class WhatsAppWebhookHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        # Validación de Webhook Meta / WhatsApp Cloud API
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)
        
        mode = params.get('hub.mode', [''])[0]
        token = params.get('hub.verify_token', [''])[0]
        challenge = params.get('hub.challenge', [''])[0]
        
        config = cargar_configuracion()
        
        if mode == 'subscribe' and token == config.get('verify_token', 'terracon_secret_token_2026'):
            print("✅ Webhook de WhatsApp verificado exitosamente por Meta.")
            self.send_response(200)
            self.send_header('Content-type', 'text/plain')
            self.end_headers()
            self.wfile.write(challenge.encode('utf-8'))
        else:
            self.send_response(200)
            self.send_header('Content-type', 'text/plain')
            self.end_headers()
            self.wfile.write(b"Servidor WhatsApp Terracon Activo y Listo.")

    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        post_data = self.rfile.read(content_length)
        
        try:
            # Twilio o Meta JSON format
            raw_str = post_data.decode('utf-8', errors='ignore')
            
            # Form-encoded (Twilio Sandbox)
            if '=' in raw_str and not raw_str.startswith('{'):
                parsed_data = parse_qs(raw_str)
                sender = parsed_data.get('From', [''])[0]
                body = parsed_data.get('Body', [''])[0]
                media_url = parsed_data.get('MediaUrl0', [''])[0]
                media_type = parsed_data.get('MediaContentType0', [''])[0]
                
                print(f"\n📩 WhatsApp de {sender}:")
                if media_url:
                    ext = ".jpg" if "image" in media_type else ".ogg"
                    dest_folder = DEST_RESPALDOS if "image" in media_type else DEST_REUNIONES
                    fname = f"whatsapp_{int(time.time())}{ext}"
                    out_path = os.path.join(dest_folder, fname)
                    
                    res = requests.get(media_url)
                    with open(out_path, "wb") as f:
                        f.write(res.content)
                    print(f"   • Archivo multimedia guardado: {out_path}")
                elif body:
                    print(f"   • Texto: {body}")
            
            # JSON format (Meta Cloud API Direct)
            else:
                payload = json.loads(raw_str)
                print("\n📩 Evento de WhatsApp Meta recibido:")
                # Procesar mensajes de Meta...
                
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(b'{"status":"received"}')

        except Exception as e:
            print("Error procesando webhook:", e)
            self.send_response(200)
            self.end_headers()

def iniciar_servidor():
    os.makedirs(DEST_RESPALDOS, exist_ok=True)
    os.makedirs(DEST_REUNIONES, exist_ok=True)
    os.makedirs(DEST_DATOS, exist_ok=True)
    
    server_address = ('', PORT)
    httpd = HTTPServer(server_address, WhatsAppWebhookHandler)
    print("=========================================================")
    print(f" SERVIDOR OFICIAL DE WHATSAPP TERRACON (PUERTO {PORT})    ")
    print("=========================================================")
    print(" Listening for incoming WhatsApp messages and media...\n")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nServidor detenido.")

if __name__ == "__main__":
    iniciar_servidor()
