ESTADO: LISTO_PARA_REVISION
REQ: REQ-0103, REQ-0104
TS: 2026-07-16T23:32:52Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0103, REQ-0104. Ultimo derivado REQ-0103: Ronda 2 corregida (obs 323/324/325): estado/saldo/fecha_cancelacion EXACTOS por cuota + un cobro por cuota con fecha real; verificacion reproducible tools/verifica_0103.py (TODO CUADRA, 459 cuotas 0 mismatches, cobros por mes legado==web). Evidencia en verificacion-cuadre.txt.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
