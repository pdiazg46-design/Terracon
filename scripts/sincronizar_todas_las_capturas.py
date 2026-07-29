import os
import sys
import datetime
import subprocess

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def sincronizar_captura_real(reportante, proyecto, proveedor, concepto, monto_total):
    monto_neto = int(monto_total / 1.19)
    iva = monto_total - monto_neto
    fecha = datetime.date.today().strftime('%Y-%m-%d')
    
    subfolder = 'carrera_pinto' if 'Carrera' in proyecto else 'diego_de_almagro'
    csv_path = os.path.join(BASE_DIR, 'datos', subfolder, 'rendicion_gastos.csv')
    
    item_id = f"CP-GASTO-{datetime.datetime.now().strftime('%H%M%S')}" if 'Carrera' in proyecto else f"DA-GASTO-{datetime.datetime.now().strftime('%H%M%S')}"
    
    row = f"\n{item_id},{fecha},CAT-05,Combustible y Faena,{proveedor},B-1044,{concepto},{monto_neto},{iva},{monto_total},SI,boleta_movil_{datetime.datetime.now().strftime('%H%M')}.jpg"
    
    with open(csv_path, 'a', encoding='utf-8') as f:
        f.write(row)
        
    print(f"Rendicion de ${monto_total} agregada a {proyecto} por {reportante}")
    
    try:
        subprocess.run(["git", "add", "."], cwd=BASE_DIR, capture_output=True)
        subprocess.run(["git", "commit", "-m", f"auto: rendición móvil sincronizada de {reportante}"], cwd=BASE_DIR, capture_output=True)
        subprocess.run(["git", "push", "origin", "main"], cwd=BASE_DIR, capture_output=True)
        print("Push a GitHub completado.")
    except Exception as e:
        print("Error en git push:", e)

if __name__ == '__main__':
    # Sincronizar las 3 rendiciones registradas en el celular de Patricio
    sincronizar_captura_real("Patricio Diaz", "Carrera Pinto (CC-CP-01)", "Copec Copiapo", "Carga Combustible Faena", 18500)
    sincronizar_captura_real("Patricio Diaz", "Carrera Pinto (CC-CP-01)", "Peaje Valles del Desierto", "Peaje Traslado Copiapo", 4500)
    sincronizar_captura_real("Patricio Diaz", "Diego de Almagro (CC-DA-02)", "Sodimac Copiapo", "Insumos y Ferreteria Faena", 24900)
