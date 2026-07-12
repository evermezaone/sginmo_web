# REQ-0068 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0068
- Tipo de cambio: UI (login.xhtml) + backend liviano (LoginBean, FiltroAutenticacion) del modulo onesystem-security
- Riesgo: medio (toca el flujo de login/redireccion; return-url tiene implicancia de seguridad -open redirect-)
- Archivos clave:
  - `onesystem-security/.../META-INF/resources/login.xhtml`: CSS para igualar campos (el `.ui-password` con toggle-mask y su input interno a width 100% + box-sizing); titulo/subtitulo bindeados a `#{loginBean.appTitulo/appSubtitulo}`.
  - `onesystem-security/.../web/LoginBean.java`: getAppTitulo/getAppSubtitulo (config por env `SGINMO_APP_TITULO`/`SGINMO_APP_SUBTITULO` o `-Dsginmo.app.titulo`/`.subtitulo`, con defaults); return-url en entrar() -> destinoGuardado() valida ruta interna .xhtml (anti open-redirect) y redirige; PORTAL y debeCambiar mantienen prioridad.
  - `onesystem-security/.../web/FiltroAutenticacion.java`: al redirigir a login por falta de sesion, guarda el destino GET pedido en la sesion (`LoginBean.ATTR_DESTINO`).
- Comandos probados:
  - `mvn -q clean package` (multi-modulo): BUILD OK.
  - Deploy + redeploy: login HTTP 200.
  - `curl dashboard-gerencial.xhtml` sin sesion -> HTTP 302 a login.xhtml (el filtro guarda el destino).
  - `python tools/smoke-test-vps.py`: 31/31 (login sigue OK).
- Cambios de datos: no.
- Cambios de entorno: OPCIONALES -> `SGINMO_APP_TITULO` / `SGINMO_APP_SUBTITULO` (o system properties) para rebrandear; sin setearlas, defaults actuales.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar; revisar la validacion anti open-redirect del return-url.
- Notas para auditor:
  - Return-url: solo se acepta un destino que empieza con el contextPath, contiene .xhtml, sin "://", y no es login/cambiar-password. Se guarda SOLO en navegaciones GET.
  - changeSessionId (anti session-fixation) se mantiene; el atributo de destino sobrevive al cambio de id (misma sesion).
  - El branding es global (login pre-tenant); no expone datos por empresa.

## Resumen Funcional

El login luce prolijo (campos parejos), se puede rebrandear (titulo/subtitulo) por variable de
entorno sin recompilar, y si el usuario pidio una pagina y se le exigio login, vuelve a esa pagina
tras autenticarse.

## Resumen Tecnico

CSS de igualado en login.xhtml; LoginBean expone branding configurable y hace el redirect de retorno
validado; FiltroAutenticacion persiste el destino GET en la sesion.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| onesystem-security .../login.xhtml | CSS igualar campos + binding de titulo/subtitulo |
| onesystem-security .../LoginBean.java | branding configurable + return-url validado |
| onesystem-security .../FiltroAutenticacion.java | guarda destino GET pedido |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Opcionales: `SGINMO_APP_TITULO`, `SGINMO_APP_SUBTITULO` (o `-Dsginmo.app.titulo` / `-Dsginmo.app.subtitulo`).

## Pruebas Ejecutadas

Build OK; deploy; 302 dashboard->login sin sesion; smoke 31/31. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Sin sesion, abrir /dashboard-gerencial.xhtml -> login -> loguearse -> debe caer en dashboard-gerencial.
2. Setear `SGINMO_APP_TITULO=Mi Inmobiliaria` en el arranque de WildFly -> el login muestra ese titulo.
3. Ver que ambos campos (usuario/clave) queden del mismo ancho.

## Riesgos Conocidos

- Return-url mal validado podria ser open-redirect: mitigado con validacion estricta de ruta interna.
