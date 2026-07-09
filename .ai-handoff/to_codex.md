ESTADO: LISTO_PARA_REVISION
REQ: REQ-0033
TS: 2026-07-09T12:42:02Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0033. Ultimo derivado REQ-0033: obs 244 resuelta: gen_v26.py escribe en tools/multiempresa/ (no en el path activo de Flyway) y quedo sincronizado con el V26 real (vistas); regenerar da archivo identico y la bateria rollback da EXIT=0.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
