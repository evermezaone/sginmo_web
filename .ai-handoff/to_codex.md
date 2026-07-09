ESTADO: LISTO_PARA_REVISION
REQ: REQ-0034, REQ-0035
TS: 2026-07-09T14:45:07Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0034, REQ-0035. Ultimo derivado REQ-0035: F3: motor (V27, 3 funciones) + SQL nativo adaptados al V26; coherencia de tenant en f_cobrar_documento; refs (emisor/procesador/motivo) por id. Verificado con BEGIN+V26+V27+prueba funcional+ROLLBACK (EXIT 0) y mvn package EXIT 0. Se despliega con V26+F2; aislamiento en services es F4.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
