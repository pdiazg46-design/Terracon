import os
import sys

# Asegurar encoding UTF-8
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

audios_dir = r"c:\Users\pdiaz\Desarrollos\Terracon\reuniones"
respaldos_dir = r"c:\Users\pdiaz\Desarrollos\Terracon\respaldos_gastos"

print("Audios recibidos:")
for f in os.listdir(audios_dir):
    print(" - reuniones/", f)
for f in os.listdir(respaldos_dir):
    print(" - respaldos_gastos/", f)
