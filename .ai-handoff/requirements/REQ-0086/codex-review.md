# REQ-0086 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Revision Estatica

- `CorreoService.enviar()` mantiene el comportamiento anterior sin `MAIL_HTTP_URL`: si no hay URL de relay, vuelve al SMTP directo y si tampoco hay `SMTP_HOST` no interrumpe el flujo.
- Con `MAIL_HTTP_URL` configurado, delega el envio por `HttpClient` con POST JSON `{to,subject,body}`, `Content-Type` UTF-8 y header `X-Mailer-Token`.
- El JSON se arma con escape explicito para comillas, backslash y caracteres de control, evitando romper el payload con asunto/cuerpo arbitrario.
- `hostinger-mailer/send.php` acepta solo POST, exige token con `hash_equals`, valida destinatario con `FILTER_VALIDATE_EMAIL`, remueve CR/LF del asunto y envia con PHPMailer desde `no-reply@one.com.py`.
- No se encontro token real ni credencial SMTP real versionada; el archivo PHP queda como artefacto para despliegue manual con placeholder.

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- El endpoint PHP contiene un placeholder funcional (`REEMPLAZAR_POR_EL_TOKEN`); en despliegue debe reemplazarse antes de publicar. Esta advertencia queda cubierta por el README y por la validacion externa pendiente.
- El servicio Java confia en que `MAIL_HTTP_URL` sea HTTPS; el README lo exige explicitamente. No bloquea este REQ porque la URL operativa definida es `https://one.com.py/mailer/send.php`.

## Riesgos

- La prueba end-to-end de entrega real depende de subir `send.php`, instalar PHPMailer y cargar `MAIL_HTTP_URL`/`MAIL_HTTP_TOKEN` en produccion. Queda como validacion externa no bloqueante, tal como define el requerimiento.

## Pruebas Revisadas

- [x] Revision estatica de `CorreoService.java`.
- [x] Revision estatica de `hostinger-mailer/send.php`.
- [x] Revision de `hostinger-mailer/README.md`.
- [x] `mvn -q -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual con `curl` contra `https://one.com.py/mailer/send.php` luego del despliegue externo.
- [ ] Prueba manual de OTP del portal usando `MAIL_HTTP_URL` y `MAIL_HTTP_TOKEN` reales.
