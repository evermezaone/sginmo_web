ESTADO: LISTO_PARA_REVISION
REQ: REQ-0083, REQ-0086, REQ-0087, REQ-0088, REQ-0089
TS: 2026-07-13T21:51:01Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0083, REQ-0086, REQ-0087, REQ-0088, REQ-0089. Ultimo derivado REQ-0083: Fase 1: informar transferencia + adjuntar comprobante en el portal + bandeja operativa interna + aplicacion (aprobar aplica el cobro con el motor de caja, forma TRANSFERENCIA). RLS + anti-doble-aplicacion. Deploy VPS + Flyway V56 + smoke 37/37. OCR y banco en 0084/0085.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
