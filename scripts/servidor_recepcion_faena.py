import os
import sys
import json
import time
import cgi
import datetime
import subprocess
from http.server import HTTPServer, SimpleHTTPRequestHandler

PORT = 8080
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

class TerraconUploadHandler(SimpleHTTPRequestHandler):
    def translate_path(self, path):
        rel_path = path.lstrip('/')
        full_path = os.path.join(BASE_DIR, rel_path)
        if os.path.isdir(full_path):
            index_path = os.path.join(full_path, 'index.html')
            if os.path.exists(index_path):
                return index_path
        return full_path

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()

    def do_POST(self):
        if self.path == '/api/subir_captura':
            try:
                ctype, pdict = cgi.parse_header(self.headers.get('content-type'))
                if ctype == 'multipart/form-data':
                    pdict['boundary'] = bytes(pdict['boundary'], "utf-8")
                    fields = cgi.parse_multipart(self.rfile, pdict)
                    
                    reportante = fields.get('reportante', [b'Patricio Diaz'])[0].decode('utf-8')
                    proyecto = fields.get('proyecto', [b'Carrera Pinto'])[0].decode('utf-8')
                    tipo = fields.get('tipo', [b'gasto'])[0].decode('utf-8')
                    
                    timestamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
                    
                    # Guardar foto si existe
                    photo_name = "Sin_foto.jpg"
                    if 'foto' in fields and fields['foto']:
                        photo_data = fields['foto'][0]
                        photo_name = f"boleta_{timestamp}_{reportante.split()[0]}.jpg"
                        photo_path = os.path.join(BASE_DIR, 'rendiciones_caja', photo_name)
                        with open(photo_path, 'wb') as f:
                            f.write(photo_data)
                    
                    # Guardar audio si existe
                    audio_name = "Sin_audio.webm"
                    if 'audio' in fields and fields['audio']:
                        audio_data = fields['audio'][0]
                        target_folder = 'audios_instrucciones' if tipo != 'reunion' else 'reuniones'
                        audio_name = f"audio_{timestamp}_{reportante.split()[0]}.webm"
                        audio_path = os.path.join(BASE_DIR, target_folder, audio_name)
                        with open(audio_path, 'wb') as f:
                            f.write(audio_data)

                    # Registrar en CSV / Dashboard
                    monto_simulado = 45000 if tipo == 'gasto' else 0
                    neto = int(monto_simulado / 1.19)
                    iva = monto_simulado - neto
                    
                    subfolder = 'carrera_pinto' if 'Carrera' in proyecto else 'diego_de_almagro'
                    csv_path = os.path.join(BASE_DIR, 'datos', subfolder, 'rendicion_gastos.csv')
                    
                    row = f"\n{datetime.date.today()},{reportante},Comercial Faena Copiapó,B-{timestamp[-4:]},{cat_by_type(tipo)},{monto_simulado},{neto},{iva},{photo_name},{audio_name},Aprobado"
                    with open(csv_path, 'a', encoding='utf-8') as f:
                        f.write(row)
                    
                    # Git push automático
                    try:
                        subprocess.run(["git", "add", "."], cwd=BASE_DIR, capture_output=True)
                        subprocess.run(["git", "commit", "-m", f"auto: captura móvil por {reportante}"], cwd=BASE_DIR, capture_output=True)
                        subprocess.run(["git", "push", "origin", "main"], cwd=BASE_DIR, capture_output=True)
                    except Exception as e:
                        print("Error en git push auto:", e)

                    self.send_response(200)
                    self.send_header('Content-Type', 'application/json')
                    self.send_header('Access-Control-Allow-Origin', '*')
                    self.end_headers()
                    response = {
                        "status": "success",
                        "message": f"Captura procesada exitosamente para {reportante}",
                        "foto": photo_name,
                        "audio": audio_name
                    }
                    self.wfile.write(json.dumps(response).encode('utf-8'))
                    return
            except Exception as err:
                print("Error procesando POST:", err)
                self.send_response(500)
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(str(err).encode('utf-8'))
                return

        super().do_GET()

def cat_by_type(tipo):
    if tipo == 'gasto': return 'CAT-05'
    if tipo == 'reunion': return 'CAT-06'
    return 'CAT-07'

if __name__ == '__main__':
    server = HTTPServer(('0.0.0.0', PORT), TerraconUploadHandler)
    print(f"Servidor Terracon activo con CORS en puerto {PORT}...")
    server.serve_forever()
