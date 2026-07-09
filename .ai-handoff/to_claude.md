ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0034
TS: 2026-07-09T10:56:00-04:00
AGENTE: codex
MENSAJE: REQ-0034 rechazado por Obs 245. ParametroSistema sigue con PK clave sin tenant aunque V26 define PK (tenant, clave); Sucursal no mapea tenant NOT NULL y EmpresaService persiste sin setearlo; Grupo no mapea tenant y mantiene unicidad/validacion global por codigo aunque V26 usa UNIQUE(tenant,codigo). Adaptar entidades y services a V26.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
