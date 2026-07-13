# REQ-0086 - Relay de correo por HTTP (endpoint PHP en Hostinger) para envio transaccional

**Numero:** REQ-0086
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Para el envio de correos (OTP del portal, alertas, etc.), en vez de que la VPS pegue al SMTP de Hostinger (con lios de From/SPF), hacer un endpoint PHP en Hostinger que envie el mail, y que la VPS le mande los datos por HTTP POST.

## Objetivo Funcional

CorreoService puede delegar el envio a un endpoint PHP alojado en el hosting del dominio (one.com.py). Asi el correo sale como no-reply@one.com.py, pasa SPF/DKIM y la app nunca guarda credenciales SMTP (solo un token). Si no hay relay configurado, cae al SMTP directo (comportamiento anterior).

## Alcance

- `CorreoService.enviar`: si `MAIL_HTTP_URL` esta seteado, hace POST JSON `{to,subject,body}` con header `X-Mailer-Token` al endpoint; si no, usa SMTP.
- Parametros nuevos: `MAIL_HTTP_URL`, `MAIL_HTTP_TOKEN`.
- Endpoint PHP `hostinger-mailer/send.php` (PHPMailer, token con hash_equals, From no-reply@one.com.py, valida destinatario, sanitiza asunto). Se despliega en Hostinger (lo sube el usuario; a Hostinger no se puede empujar desde la VPS).

## Criterios De Aceptacion

- [x] Con `MAIL_HTTP_URL`+`MAIL_HTTP_TOKEN`, CorreoService hace POST al relay y envia el correo.
- [x] Sin `MAIL_HTTP_URL`, sigue el SMTP directo (fallback).
- [x] El endpoint exige token (401 si falta/incorrecto), solo POST, valida email y sanitiza asunto (anti header-injection).
- [x] La app no almacena credenciales SMTP cuando usa el relay (solo el token).
- [x] Build `mvn -q clean package` EXIT 0; smoke 36/36.
- [x] El token NO queda en el repo (placeholder en send.php/README; valor real fuera del control de versiones).

## Validacion Pendiente (externa, no bloquea la implementacion)

- Subir `send.php` a https://one.com.py/mailer/send.php + PHPMailer + cargar `MAIL_HTTP_URL`/`MAIL_HTTP_TOKEN`,
  y probar que el OTP del portal llega al email del cliente (prueba manual M03 del test-plan).

## Dependencias

- Relacionado: REQ-0078 (OTP del portal), REQ-0075 (alertas por correo).
