# Terracon Energy - Sistema de Control y Orquestación de Portafolio Revamping Solar

Este repositorio contiene la arquitectura de control, seguimiento técnico-financiero, administración de Carta Gantt y **gestión de reuniones/compromisos** para los dos proyectos de revamping solar desarrollados por **Terracon Energy** (mandante) para **Sonnedix** (cliente final):

1. **Carrera Pinto (CC-CP-01)**: Recambio eléctrico puro (MVS 4480, String Inverters, reconfiguración de strings, recableado DC/AC).
2. **Diego de Almagro (CC-DA-02)**: Revamping integral (desmontaje/montaje de módulos solares, logística inversa de paneles viejos, MVS 4480, String Inverters, recableado DC/AC).

---

## 🏆 REGLA DE ORO DE GOBERNANZA E INTERRELACIÓN

1. **Un Solo Equipo (Frente Unido)**: Patricio Díaz (Director Portafolio / Administrador), Marcos Font (PM Senior / Liderazgo Operativo) y Víctor Escanilla (Site Manager Carrera Pinto) conforman **UN SOLO BLOQUE COHESIONADO**.
2. **Resolución Interna Previa**: Todo problema, contingencia de terreno, imprevisto de transporte o desvío de cronograma se analiza y resuelve **INTERNAMENTE** entre Patricio, Marcos y Víctor antes de emitir cualquier comunicación externa o informe a Terracon Energy o Sonnedix.
3. **Liderazgo Operativo de Marcos**: Marcos Font define la hoja de ruta y los pasos a seguir; el equipo completo rema alineado en esa dirección con el respaldo administrativo de Patricio.

---

## 👥 Arquitectura de los 3 Agentes

| Agente | Perfil / Rol | Archivo de Prompt | Responsabilidad Principal |
| :--- | :--- | :--- | :--- |
| **Agente 1** | **Orquestador PM** *(Marcos Font Directivo)* | [.agentes/01_orquestador_pm.md](file:///c:/Users/pdiaz/Desarrollos/Terracon/.agentes/01_orquestador_pm.md) | Consolidado ejecutivo de portafolio, control presupuestario global, *outages*, RDI, minutas de reunión y comunicaciones formales. |
| **Agente 2** | **Sitio Carrera Pinto** *(Víctor Escanilla)* | [.agentes/02_sitio_carrera_pinto.md](file:///c:/Users/pdiaz/Desarrollos/Terracon/.agentes/02_sitio_carrera_pinto.md) | Avance físico electromecánico, control LOTO, MVS 4480, comisionamiento y rendición CC-CP-01. |
| **Agente 3** | **Sitio Diego de Almagro** *(Marcos Font Operativo)* | [.agentes/03_sitio_diego_de_almagro.md](file:///c:/Users/pdiaz/Desarrollos/Terracon/.agentes/03_sitio_diego_de_almagro.md) | Desmontaje/montaje de módulos, logística inversa (acopio/despacho), avance eléctrico, outages y rendición CC-DA-02. |

---

## 🎙️ Gestión de Reuniones y Compromisos

Puedes subir transcripciones, notas o audios de reuniones con Sonnedix, Terracon o subcontratistas a la carpeta `reuniones/`. El sistema extraerá automáticamente:
- **Minuta de Reunión**: Puntos clave discutidos.
- **Matriz de Compromisos**: Asignación de tareas, responsable, entidad, fecha límite e impacto directo en la Carta Gantt o Presupuesto.

---

## 📂 Estructura del Repositorio

- `.agentes/`: Prompts y perfiles operativos de los 3 agentes.
- `plantillas/`: Formatos estandarizados en Markdown para reportes diarios, informes de portafolio, RDI, rendiciones de gastos, Carta Gantt y minutas de reunión.
- `datos/`: Almacenamiento en CSV y JSON por proyecto y seguimiento de compromisos (`datos/compromisos.json`).
- `reuniones/`: Registro de transcripciones y notas de reuniones grabadas.
- `respaldos_gastos/`: Directorios para almacenar las boletas, facturas y comprobantes digitales por proyecto.
- `scripts/`: Herramientas en Python para automatizar el cálculo de avances, gastos y ruta crítica de la Gantt (`bot_telegram_terracon.py`).

---

## 🚀 Hoja de Ruta de Infraestructura & Evolución (Fase 2)

> [!NOTE]
> **Tarea Pendiente Registrada**: Una vez definidos y firmados los contratos oficiales con Sonnedix para el inicio de faena, se procederá a evolucionar la arquitectura actual a **Vercel + Supabase**:
> 1. **Vercel (Serverless Functions + Webhook Telegram)**: Para procesamiento cloud 24/7 sin dependencia de PC encendido.
> 2. **Supabase (PostgreSQL Realtime)**: Para almacenamiento de base de datos corporativa con sincronización en tiempo real (WebSockets) y roles de acceso segregados (Director, Site Manager, Cliente Sonnedix).

