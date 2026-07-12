ESTADO: LISTO_PARA_REVISION
REQ: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057
TS: 2026-07-12T04:58:01Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057. Ultimo derivado REQ-0057: Mora y cobranza (V38): cartera vencida (usa f_mora_cuota, misma logica que cobros, cap 1000) con filtros y export CSV; gestiones de cobranza y promesas de pago (tablas nuevas por-tenant, RLS); promesas vencidas -> Agenda; cierre manual de promesa. No modifica cuotas; promesa no es pago. Permisos VER/EDITAR/EXPORTAR. Build+deploy+smoke 24/24. Diferido: cierre auto de promesa al cobrar.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
