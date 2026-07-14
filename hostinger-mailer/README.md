# Relay de correo (REQ-0086) — endpoint PHP en Hostinger

La app (VPS) delega el envio de correos a este endpoint PHP alojado en el hosting del dominio
(one.com.py). Asi el correo sale como `no-reply@one.com.py`, pasa SPF/DKIM y la app nunca guarda
credenciales SMTP: solo el token. **Es un unico archivo, sin Composer ni PHPMailer.**

## 1) Subir a Hostinger (hPanel → Administrador de archivos)

1. Entrar a hPanel de one.com.py → **Administrador de archivos**.
2. Dentro de `public_html/`, crear la carpeta `mailer`.
3. Subir `send.php` dentro de `public_html/mailer/`, de modo que quede accesible en:

       https://one.com.py/mailer/send.php

4. **Pegar el token**: editar `send.php` y reemplazar `REEMPLAZAR_POR_EL_TOKEN` por el token real
   (el mismo que vas a poner en `MAIL_HTTP_TOKEN`). El token NO se versiona en el repo.

> El archivo usa la funcion `mail()` nativa del hosting (MTA local, envia con el dominio). No hace
> falta instalar nada mas.

## 2) Configurar la app (pantalla Parametros, tenant -1 o por empresa)

    MAIL_HTTP_URL   = https://one.com.py/mailer/send.php
    MAIL_HTTP_TOKEN = <TU_TOKEN_SECRETO>

Si `MAIL_HTTP_URL` esta seteado, `CorreoService` hace POST al relay; si no, cae al SMTP directo.

## 3) Probar (desde cualquier terminal, o pedimelo y lo corro desde la VPS)

    curl -s -X POST https://one.com.py/mailer/send.php \
      -H "Content-Type: application/json" \
      -H "X-Mailer-Token: <TU_TOKEN_SECRETO>" \
      -d '{"to":"tucorreo@gmail.com","subject":"Prueba SGInmo","body":"Hola, esto es una prueba."}'

Respuesta esperada: `{"ok":true}`. Luego, en el portal, "Primer ingreso" enviara el OTP por este canal.

## Seguridad

- Token secreto obligatorio (header `X-Mailer-Token`), comparado con `hash_equals`.
- Solo POST; valida que `to` sea un email; sanitiza el asunto y el From (anti header-injection).
- Servir SIEMPRE por HTTPS. No es un relay abierto: sin token valido responde 401.
- Si el token se filtra, regenerarlo en `send.php` y en el parametro `MAIL_HTTP_TOKEN`.

## Nota

Si `mail()` no entrega en tu plan de Hostinger (algunos lo restringen), la alternativa es SMTP
autenticado con la casilla `no-reply@one.com.py`; en ese caso avisame y te devuelvo una variante de
`send.php` con SMTP (requiere la contrasena de esa casilla, que sale del panel de correo).
