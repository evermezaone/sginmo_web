# REQ-0082 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0082
- Tipo de cambio: UI (nueva pagina de entrada del portal) + un metodo en PortalLoginBean. Sin BD.
- Riesgo: bajo.
- Archivos clave:
  - `webapp/portal/index.xhtml` (NUEVO): welcome-file de la carpeta `/portal/`; en su `f:viewAction` llama a `portalLoginBean.entrada()` que redirige a `portal/login` (o `portal/inicio` si hay sesion de portal).
  - `web/PortalLoginBean.java`: metodo `entrada()` -> outcome de redirect segun `PortalSesion.isAutenticado()`.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS; verificacion: `GET /sginmo-web/portal/` -> 302 a `portal/login.xhtml`; `GET /portal/index.xhtml` -> 302 igual. Smoke 36/36.
- Cambios de datos: no. Cambios de entorno: no.
- Decision esperada: aprobar; es el entrypoint faltante del portal.
- Notas para auditor:
  - `/portal/**` ya es publico en FiltroAutenticacion (REQ-0078); index.xhtml solo redirige, no expone datos.

## Resumen Funcional

Entrar por la URL de carpeta del portal (`/portal/`) ahora lleva al login del portal en vez de mostrar un error de recurso no encontrado.

## Resumen Tecnico

Se agrega `portal/index.xhtml` (welcome-file) con un viewAction que redirige via PortalLoginBean.entrada().

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| webapp/portal/index.xhtml | NUEVO - entrada que redirige al login/cuenta |
| web/PortalLoginBean.java | metodo entrada() |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy VPS; `GET /portal/` y `/portal/index.xhtml` -> 302 a login; smoke 36/36.

## Pruebas Manuales Sugeridas

1. Abrir `http://<host>/sginmo-web/portal/` -> muestra el login del portal (sin error).
2. Con sesion de portal activa, abrir `/portal/` -> va a la cuenta (portal/inicio).

## Limitaciones Conocidas

- Ninguna.

## Riesgos Conocidos

- Ninguno (solo redireccion).
