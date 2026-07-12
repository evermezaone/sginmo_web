# REQ-0068 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` (multi-modulo) | Build OK | OK |
| T02 | Deploy + redeploy | login HTTP 200 | OK |
| T03 | `curl dashboard-gerencial.xhtml` sin sesion | HTTP 302 -> login.xhtml (filtro guarda destino) | OK |
| T04 | HTML del login (grep h1/sub) | render con defaults SGInmo / Gestion inmobiliaria... | OK |
| T05 | `python tools/smoke-test-vps.py` | 31/31 (login sigue OK) | OK |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Sin sesion, abrir /dashboard-gerencial.xhtml -> loguearse | cae en dashboard-gerencial, no en /index | pendiente (verificacion visual del usuario) |
| M02 | Arrancar WildFly con `-Dsginmo.app.titulo="Mi Inmobiliaria"` | el login muestra ese titulo | pendiente (requiere set de la variable en ops) |
| M03 | Comparar ancho de campos usuario vs contrasena | identico | pendiente (visual) |
| M04 | Return-url con URL externa forjada (`//evil` o `http://`) | se ignora, cae a /index | pendiente (cubierto por validacion en codigo) |

## Datos De Prueba

Un usuario valido para loguear. Opcional: setear SGINMO_APP_TITULO en el arranque para probar branding.

## Nota De Seguridad

El return-url solo acepta rutas internas (empiezan con el contextPath, con .xhtml, sin "://", ni
login/cambiar-password) para evitar open-redirect. PORTAL y cambio-de-clave forzado mantienen prioridad.
