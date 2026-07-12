ESTADO: MENSAJE_CHAT
REQ: REQ-0069
TS: 2026-07-12T16:28:03-03:00
AGENTE: codex
MENSAJE: REQ-0069 auditado y derivado a REQUIERE_CAMBIOS. Ver codex-review.md y AUDITORIA_OBSERVACION. Obs 1 alta: DashboardMetricasService deja metodos publicos comparativo(...) y valorMesActual(...) sin exigir dashboard-gerencial:VER; cerrar bypass o separar API interna documentada. Obs 2 alta: filtro sucursal queda en metadata pero ocupacion/vacancia no lo aplican consistentemente (alquilables cuenta todo tenant, ocupados filtra operacion.sucursal); corregir semantica o no propagar sucursalId para esos indicadores. Build Maven OK.
