import os
import sys
import datetime
import subprocess

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def agregar_rendicion(reportante, proyecto, proveedor, concepto, monto_total, nro_doc="B-9981"):
    monto_neto = int(monto_total / 1.19)
    iva = monto_total - monto_neto
    fecha = datetime.date.today().strftime('%Y-%m-%d')
    
    subfolder = 'carrera_pinto' if 'Carrera' in proyecto else 'diego_de_almagro'
    csv_path = os.path.join(BASE_DIR, 'datos', subfolder, 'rendicion_gastos.csv')
    
    item_id = f"CP-GASTO-{datetime.datetime.now().strftime('%M%S')}" if 'Carrera' in proyecto else f"DA-GASTO-{datetime.datetime.now().strftime('%M%S')}"
    
    row = f"\n{item_id},{fecha},CAT-05,Combustible y Faena,{proveedor},{nro_doc},{concepto},{monto_neto},{iva},{monto_total},SI,boleta_patricio.jpg"
    
    with open(csv_path, 'a', encoding='utf-8') as f:
        f.write(row)
        
    print(f"Rendicion de ${monto_total} agregada a {proyecto} por {reportante}")
    
    # Git sync
    try:
        subprocess.run(["git", "add", "."], cwd=BASE_DIR, capture_output=True)
        subprocess.run(["git", "commit", "-m", f"update: rendicion agregada a {proyecto}"], cwd=BASE_DIR, capture_output=True)
        subprocess.run(["git", "push", "origin", "main"], cwd=BASE_DIR, capture_output=True)
        print("Datos subidos a GitHub Pages exitosamente.")
    except Exception as e:
        print("Error en git push:", e)

if __name__ == '__main__':
    agregar_rendicion("Patricio Diaz", "Carrera Pinto (CC-CP-01)", "Copec Copiapo", "Carga Combustible Camioneta Faena", 45000)
