# REQ-0068 - Pulido del login: campos, branding configurable y return-url

**Numero:** REQ-0068
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Del usuario (2026-07-12): "esteticamente el login falta mejorar, igualar los campos. No se si el
titulo es configurable desde algun .env puesto que ese login es un jar externo. Ademas, si yo intento
entrar dashboard-gerencial.xhtml y por cuestiones de sesion me pide login, al entrar en el login,
despues de logearme, deberia de ir a la pagina que llamo el login."

## Objetivo Funcional

Pulir la pantalla de login (modulo onesystem-security, que se compila desde este repo): campos de
igual ancho, branding (titulo/subtitulo) configurable por entorno, y retorno a la URL originalmente
solicitada tras autenticarse.

## Criterios De Aceptacion

- [x] Los campos Usuario y Contrasena tienen el MISMO ancho (el toggle-mask del password ya no lo angosta). (`login.xhtml`: `.ui-password` y su input interno a width 100% + box-sizing)
- [x] El titulo y el subtitulo del login son configurables sin tocar codigo, por variable de entorno o -D. (`SGINMO_APP_TITULO`/`SGINMO_APP_SUBTITULO` o `-Dsginmo.app.titulo`/`-Dsginmo.app.subtitulo`; defaults "SGInmo" y "Gestion inmobiliaria - ingrese con su usuario"; expuestos por LoginBean.getAppTitulo/getAppSubtitulo y bindeados en login.xhtml)
- [x] Al pedir una pagina protegida sin sesion, tras loguearse el usuario vuelve a esa pagina (no a /index). (FiltroAutenticacion guarda el destino GET en sesion; LoginBean.entrar redirige alli si es interno .xhtml valido)
- [x] El retorno no permite open-redirect ni saltar el cambio de contrasena/portal. (validacion: solo rutas que empiezan con el contextPath, con .xhtml, sin ://, distintas de login/cambiar-password; PORTAL y debeCambiar tienen prioridad)
- [x] El login sigue funcionando (autenticacion, changeSessionId anti-fijacion, mensajes). (smoke 31/31, login OK)

## Reglas De Negocio

- El branding del login es GLOBAL (pre-tenant): el login corre antes del contexto de empresa.
- La seguridad no se baja: el return-url se valida para evitar redirecciones externas.

## Dependencias

- Depende de: REQ-0004 (login/sesion).

## Fuentes Y Trazabilidad

- Pedido directo del usuario 2026-07-12 (captura del login).
- El login vive en `onesystem-security/src/main/resources/META-INF/resources/login.xhtml` (se compila aca; no es un jar opaco).
