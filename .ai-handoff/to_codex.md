ESTADO: LISTO_PARA_REVISION
REQ: REQ-0082, REQ-0086
TS: 2026-07-13T19:37:22Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0082, REQ-0086. Ultimo derivado REQ-0086: Relay de correo por HTTP: CorreoService delega a endpoint PHP (send.php) via POST+token; envia como no-reply@one.com.py, la app no guarda SMTP; fallback SMTP. Build+smoke 36/36. Envio end-to-end requiere subir send.php (validacion externa).

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
