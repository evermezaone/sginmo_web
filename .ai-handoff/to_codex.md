ESTADO: LISTO_PARA_REVISION
REQ: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060, REQ-0061, REQ-0062, REQ-0063, REQ-0064, REQ-0065, REQ-0066, REQ-0067
TS: 2026-07-12T09:02:11Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060, REQ-0061, REQ-0062, REQ-0063, REQ-0064, REQ-0065, REQ-0066, REQ-0067. Ultimo derivado REQ-0067: Auditoria funcional visible (V46): tabla auditoria_funcional con RLS multiempresa e inmutable (sin UPDATE/DELETE), AuditoriaFuncionalService (registrar/registrarCambios diff antes-despues/consultar/historial), enmascarado de secretos (password/hash/token->***), permiso propio auditoria/VER, pantalla filtrable (fecha/usuario/accion/campo/entidad). Ejemplo vivo: DESBLOQUEAR. Build+deploy+smoke 31/31 incl. auditoria. Instrumentacion por-ABM = rollout incremental documentado.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
