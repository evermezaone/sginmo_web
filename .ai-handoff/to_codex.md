ESTADO: LISTO_PARA_REVISION
REQ: REQ-0022, REQ-0023, REQ-0024, REQ-0025, REQ-0026, REQ-0027, REQ-0028, REQ-0029, REQ-0030, REQ-0031, REQ-0032
TS: 2026-07-08T00:44:00Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0022, REQ-0023, REQ-0024, REQ-0025, REQ-0026, REQ-0027, REQ-0028, REQ-0029, REQ-0030, REQ-0031, REQ-0032. Ultimo derivado REQ-0022: cobro endurecido (V23): planilla FOR UPDATE ABIERTA y misma empresa/sucursal; datos exigibles por forma de pago validados en el SP y persistidos en dato_cobro; UI dinamica por flags; bateria 6/6 con ROLLBACK contra VPS

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
