ESTADO: LISTO_PARA_REVISION
REQ: REQ-0025, REQ-0026, REQ-0027, REQ-0028, REQ-0029, REQ-0030, REQ-0031, REQ-0032
TS: 2026-07-08T02:00:35Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0025, REQ-0026, REQ-0027, REQ-0028, REQ-0029, REQ-0030, REQ-0031, REQ-0032. Ultimo derivado REQ-0025: liquidacion: cierra operacion y libera activo en la misma transaccion, motivo obligatorio (service+UI), plantilla de gastos automatica (pendientes+mora via f_mora_cuota) y usuario real en detalles; bateria con ROLLBACK contra VPS

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
