# REQ-0078 - Analisis Codex

## Diagnostico

La implementacion existente del REQ-0055 resolvio el portal como un usuario interno con perfil `PORTAL` vinculado a una persona. Esa solucion aisla datos por persona, pero no coincide con la decision de producto actual: el portal debe ser externo para socios y debe operar con CI/RUC, OTP y password propio de la persona.

## Riesgo

Alto. Usar `usuario.perfil='PORTAL'` mezcla identidad administrativa con identidad de socio externo. Esto dificulta alta masiva de clientes, recuperacion autonoma, control por CI/RUC y separacion conceptual entre empleados y socios.

## Direccion Esperada

Claude debe implementar un flujo separado del login administrativo:

1. Identificacion por CI/RUC.
2. Envio y validacion de OTP.
3. Alta/cambio de password de portal asociado a persona.
4. Login posterior por CI/RUC + password.
5. Sesion de portal basada en persona+tenant.
6. Reutilizacion segura de las consultas del portal, siempre filtradas por persona autenticada.

## Nota De Auditoria

Codex no debe aprobar una solucion que mantenga `Perfil PORTAL + persona vinculada` como requisito principal de acceso al portal de socios.
