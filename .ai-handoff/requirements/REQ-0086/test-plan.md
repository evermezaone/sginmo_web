# REQ-0086 - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | BUILD OK | OK |
| T02 | Deploy VPS + smoke | 36/36 | OK |
| T03 | Sin MAIL_HTTP_URL | usa SMTP directo (fallback) | OK (sin cambios) |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Endpoint | curl POST con token al send.php | ok:true y llega el correo | pendiente (subir send.php) |
| M02 | Token invalido | curl POST sin token o incorrecto | HTTP 401 unauthorized | pendiente |
| M03 | OTP portal | Cargar params + "Primer ingreso" en el portal | El OTP llega al email del cliente | pendiente |

## Datos De Prueba

send.php subido a https://one.com.py/mailer/send.php + PHPMailer instalado + params cargados.
