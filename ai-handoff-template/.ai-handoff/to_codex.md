ESTADO: ESPERA
REQ: -
TS: YYYY-MM-DDTHH:mm:ssZ
AGENTE: sistema
MENSAJE: Sin tareas pendientes para auditoria.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
