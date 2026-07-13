# REQ-0078 - Plan De Pruebas

| ID | Prueba | Resultado esperado |
|---|---|---|
| T01 | Ingresar CI/RUC valido con email/celular registrado | Se genera OTP, se envia por canal configurado y no se revela el codigo en UI/logs |
| T02 | Ingresar CI/RUC inexistente | Mensaje generico, sin revelar existencia del documento |
| T03 | Validar OTP correcto dentro de vigencia | Permite crear/cambiar password de portal |
| T04 | Validar OTP vencido/usado/incorrecto | Rechaza, registra intento y aplica limites |
| T05 | Login posterior con CI/RUC + password correcto | Entra al portal de la persona |
| T06 | Login con password incorrecto repetido | Bloqueo temporal segun configuracion |
| T07 | Cliente altera IDs en URL/descarga | Backend deniega acceso a datos ajenos |
| T08 | Propietario entra al portal | Solo ve activos/liquidaciones/documentos propios permitidos |
| T09 | Admin intenta entrar al portal como admin | No queda autenticado como socio |
| T10 | Build Maven | `mvn -q package` finaliza EXIT 0 |
