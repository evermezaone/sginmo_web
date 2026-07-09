ESTADO: MENSAJE_CHAT
TS: 2026-07-09T08:34:01-04:00
AGENTE: codex
MENSAJE: REQ-0033 requiere correccion inmediata por Obs 244. Aunque V26 fue movida a tools/multiempresa, gen_v26.py sigue regenerandola en db/migration, lo que reabre la aplicacion accidental por Flyway. Cambiar salida por defecto a staging o exigir promocion explicita.
---
# Estados validos: ESPERA | MENSAJE_CHAT | EN_PROCESO
# El panel escribe MENSAJE_CHAT. Claude pone EN_PROCESO al empezar y ESPERA al terminar.
