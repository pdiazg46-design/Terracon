# GUÍA DE ACCESO Y ACTUALIZACIÓN DESDE DISPOSITIVOS MÓVILES (FAENA / TERRENO)

**ESTRATEGIA SELECCIONADA**: **Sincronización en la Nube Offline/Online (Google Drive / OneDrive / Dropbox)**

Esta guía detalla la metodología aprobada para que **Patricio Díaz**, **Marcos Font** y los **Administradores de Sitio** puedan enviar datos de avance diario, comprobantes de gastos en fotos y notas de voz desde su teléfono celular directamente al sistema **Terracon**, sin importar si hay o no cobertura celular en la planta solar.

---

## 📱 ¿POR QUÉ LA OPCIÓN 2 ES LA MEJOR PARA FAENAS SOLARES?

1. **Trabajo 100% Offline (Sin Señal en Terreno)**:
   En plantas solares como Carrera Pinto o Diego de Almagro (Región de Atacama), es muy común tener zonas con señal débil o nula. Con la Opción 2, puedes tomar fotos de boletas, grabar notas de voz o dictar avances **sin necesidad de tener internet en ese instante**.

2. **Sincronización Automática en Segundo Plano**:
   Tu teléfono guarda los archivos de forma local. En cuanto retomas cobertura 4G/5G o te conectas al Wi-Fi del hotel o caseta de faena, el celular **sincroniza en segundo plano automáticamente**.

3. **Sin Consumo Constante de Batería ni Streaming**:
   A diferencia de las conexiones remotas en vivo que agotan la batería del teléfono y exigen señal constante, esta opción consume mínimos recursos del dispositivo.

---

## 🛠️ ESTRUCTURA DE CARPETAS SINCRONIZADAS EN TU CELULAR

En tu teléfono (a través de la app de Google Drive, OneDrive o Dropbox) tendrás la carpeta compartida `Terracon_Terreno` organizada así:

```text
Terracon_Terreno/  (Carpeta en tu Celular)
├── audios_reuniones/      <-- Guardas grabaciones de audio (.mp3, .m4a, .wav)
├── fotos_respaldos/       <-- Fotografías de boletas, facturas y comprobantes
└── notas_avance/          <-- Notas de texto o dictados de voz rápidos
```

---

## 🤖 PROCESAMIENTO AUTOMÁTICO EN LA COMPUTADORA

En la computadora del proyecto ejecutaremos el programa automatizado:

```bash
python scripts/procesar_sincronizacion_nube.py
```

### ¿Qué hace el sistema?
1. Detecta todos los audios nuevos subidos desde el celular y los mueve a `reuniones/` para procesar la **Minuta y Matriz de Compromisos**.
2. Detecta las fotos de boletas y facturas y las organiza por proyecto en `respaldos_gastos/`.
3. Actualiza las métricas financieras y el estado de la Carta Gantt.
