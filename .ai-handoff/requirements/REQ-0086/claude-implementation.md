# REQ-0086 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0086
- Tipo de cambio: backend (onesystem-security/CorreoService) + artefacto PHP para Hostinger. Sin BD.
- Riesgo: medio (envio de correo / superficie externa).
- Archivos clave:
  - `onesystem-security/.../CorreoService.java`: `enviar()` delega a `enviarPorHttp()` si `MAIL_HTTP_URL` esta
    seteado (POST JSON {to,subject,body} con header `X-Mailer-Token`, timeouts, HttpClient); si no, SMTP directo.
    `jsonEsc()` escapa el contenido. Params `MAIL_HTTP_URL` / `MAIL_HTTP_TOKEN`.
  - `hostinger-mailer/send.php` (+ README): endpoint PHP con PHPMailer; token con `hash_equals`; solo POST;
    valida `to` (FILTER_VALIDATE_EMAIL); sanitiza asunto (anti header-injection); From no-reply@one.com.py;
    transporte mail() por defecto o SMTP opcional. Lo sube el usuario a https://one.com.py/mailer/send.php.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS; `python tools/smoke-test-vps.py`: 36/36.
- Cambios de datos: no (los params se cargan por pantalla). Cambios de entorno: si (subir send.php + cargar params).
- Decision esperada: aprobar; el relay evita exponer credenciales SMTP en la app y arregla SPF/From del OTP.
- Notas para auditor:
  - La app solo conoce el token; las credenciales SMTP (si se usan) viven solo en send.php en Hostinger.
  - jsonEsc escapa comillas/backslash/control; el asunto se sanitiza tambien del lado PHP.
  - Fallback SMTP intacto: sin MAIL_HTTP_URL el comportamiento es el anterior.

## Resumen Funcional

Los correos del sistema (OTP del portal, alertas) pueden enviarse a traves de un endpoint PHP en el hosting del
dominio, que envia como no-reply@one.com.py y pasa SPF/DKIM. Habilita el primer ingreso del portal por OTP.

## Resumen Tecnico

CorreoService gana un transporte HTTP (POST JSON + token) ademas del SMTP; endpoint PHP con PHPMailer.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| onesystem-security/.../CorreoService.java | transporte HTTP (enviarPorHttp + jsonEsc) + params MAIL_HTTP_* |
| hostinger-mailer/send.php + README.md | NUEVO endpoint PHP (a subir a Hostinger) |

## Cambios De Datos

Sin cambios de esquema. Parametros nuevos (por pantalla): MAIL_HTTP_URL, MAIL_HTTP_TOKEN.

## Variables De Entorno

Requiere: subir send.php a https://one.com.py/mailer/send.php y cargar MAIL_HTTP_URL/MAIL_HTTP_TOKEN.

## Pruebas Ejecutadas

Build OK; deploy VPS; smoke 36/36. (El envio real end-to-end depende de subir send.php: prueba manual.)

## Pruebas Manuales Sugeridas

1. Subir send.php + PHPMailer a Hostinger; `curl` de prueba -> {"ok":true} y llega el correo.
2. Cargar MAIL_HTTP_URL/MAIL_HTTP_TOKEN; en el portal "Primer ingreso" -> el OTP llega al email del cliente.

## Limitaciones Conocidas

- El endpoint PHP debe subirse a Hostinger manualmente (no se puede empujar desde la VPS).

## Riesgos Conocidos

- Superficie de envio: mitigada con token (hash_equals), solo-POST, validacion de email y sanitizacion; HTTPS.
