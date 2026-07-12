# REQ-0068 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Tres ajustes cohesivos en el login (modulo onesystem-security): CSS para igualar campos, branding
configurable por entorno via LoginBean, y return-url (filtro guarda destino GET; LoginBean redirige
validado). Se agrupan en un REQ por ser la misma pantalla y un unico deploy.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| login.xhtml | CSS igualar campos + binding titulo/subtitulo |
| LoginBean.java | getAppTitulo/getAppSubtitulo + return-url validado |
| FiltroAutenticacion.java | guardar destino GET |

## Pruebas Previstas

- [ ] Build OK
- [ ] Deploy + login 200 + smoke
- [ ] dashboard sin sesion -> 302 login; branding default; anti open-redirect

## Riesgos

- Return-url: riesgo de open-redirect -> validacion estricta de ruta interna.

## Cambios De Datos

Sin cambios.
