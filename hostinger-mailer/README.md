# Relay de correo (REQ-0086) — endpoint PHP en Hostinger

La app (VPS) delega el envio de correos a este endpoint PHP alojado en el hosting del dominio
(one.com.py). Asi el correo sale como `no-reply@one.com.py`, pasa SPF/DKIM y la app nunca guarda
credenciales SMTP: solo el token.

## 1) Subir a Hostinger

Crear la carpeta `mailer/` en la raiz publica del sitio one.com.py y subir `send.php` alli, de modo
que quede accesible en:

    https://one.com.py/mailer/send.php

## 2) Instalar PHPMailer (una de las dos)

- **Composer** (recomendado, via SSH o Terminal de hPanel, dentro de la carpeta `mailer/`):

      composer require phpmailer/phpmailer

  (crea `vendor/` — send.php lo detecta solo).

- **Manual**: descargar PHPMailer (https://github.com/PHPMailer/PHPMailer/releases) y subir estos
  tres archivos a `mailer/PHPMailer/src/`:
  `PHPMailer.php`, `SMTP.php`, `Exception.php`.

## 3) Transporte de envio (en send.php)

- Por defecto usa `mail()` (el MTA local del hosting; envia como el dominio, sin credenciales).
- Si preferis SMTP autenticado con la casilla no-reply, completa en `send.php`:
  `SMTP_HOST` (ej. `smtp.hostinger.com`), `SMTP_USER = no-reply@one.com.py`, `SMTP_PASS`.

## 4) Configurar la app (pantalla Parametros, tenant -1 o por empresa)

    MAIL_HTTP_URL   = https://one.com.py/mailer/send.php
    MAIL_HTTP_TOKEN = <TU_TOKEN_SECRETO>

Si `MAIL_HTTP_URL` esta seteado, `CorreoService` hace POST al relay; si no, cae al SMTP directo.

## 5) Probar

    curl -s -X POST https://one.com.py/mailer/send.php \
      -H "Content-Type: application/json" \
      -H "X-Mailer-Token: <TU_TOKEN_SECRETO>" \
      -d '{"to":"tucorreo@gmail.com","subject":"Prueba SGInmo","body":"Hola, esto es una prueba."}'

Respuesta esperada: `{"ok":true}`. Luego, en el portal, "Primer ingreso" enviara el OTP por este canal.

## Seguridad

- Token secreto obligatorio (header `X-Mailer-Token`), comparado con `hash_equals`.
- Solo POST; valida que `to` sea un email; sanitiza el asunto (anti header-injection).
- Servir SIEMPRE por HTTPS. No es un relay abierto: sin token valido responde 401.
- Si el token se filtra, regenerarlo en `send.php` y en el parametro `MAIL_HTTP_TOKEN`.
