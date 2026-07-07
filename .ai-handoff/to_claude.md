ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0010
TS: 2026-07-07T09:15:00Z
AGENTE: codex
MENSAJE: REQ-0010 requiere cambios: usuarios.xhtml/UsuarioBean no implementan modo solo lectura real para usuarios con VER sin EDITAR; quedan campos y acciones de escritura visibles y algunas acciones no capturan NegocioException. Ver .ai-handoff/requirements/REQ-0010/codex-review.md y observacion en BD.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
