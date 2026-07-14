# REQ-0095 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0095
- Tipo de cambio: UI
- Riesgo: bajo (un encabezado en una pagina del portal; sin backend)
- Archivos clave:
  - `webapp/portal/inicio.xhtml`: nuevo h1 "Bienvenido, #{portalBean.nombreUsuario}" al tope del contenido.
- Comandos probados:
  - `powershell -File tools/deploy-vps.ps1` (mvn clean package + deploy): Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK === (37 pantallas 200).
- Cambios de datos: no
- Cambios de entorno: no
- Impacto LLM/tokens: no
- Decision esperada: aprobar
- Notas para auditor: se conservan los rotulos "Como cliente"/"Como propietario" como divisores de seccion.

## Resumen Funcional

El portal saluda al socio con "Bienvenido, {nombre}" grande y legible en la pagina de inicio.

## Resumen Tecnico

Se agrego un `<h1>` al inicio de `ui:define name="contenido"` en `portal/inicio.xhtml` con el nombre del
socio (`portalBean.nombreUsuario` = `sesion.getNombre()`). No se toco backend.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `webapp/portal/inicio.xhtml` | h1 de bienvenida al tope del contenido |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

- Build OK (mvn clean package, EXIT 0).
- Deploy a la VPS: Redeploy OK; login.xhtml HTTP 200.
- smoke-test-vps.py: 37 pantallas HTTP 200 (TODAS OK).

## Pruebas Manuales Sugeridas

1. Entrar al portal del socio -> se ve "Bienvenido, {nombre}" grande arriba; el nombre de la barra superior se mantiene.

## Riesgos Conocidos

Ninguno.
