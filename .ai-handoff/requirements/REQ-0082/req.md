# REQ-0082 - BUG portal: entrar por /portal/ da "index.xhtml Not Found"; falta redirect al login

**Numero:** REQ-0082
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Al abrir http://77.237.235.69:8080/sginmo-web/portal/ (URL de carpeta) aparece: "/portal/index.xhtml Not Found in ExternalContext as a Resource".

## Causa Raiz

El welcome-file de la app es `index.xhtml`. Al pedir la carpeta `/portal/`, el contenedor resuelve `/portal/index.xhtml`, que no existia: el entrypoint del portal es `portal/login.xhtml`. Sin ese archivo, FacesServlet no encuentra el recurso y muestra el error.

## Objetivo Funcional

Entrar por `/portal/` debe llevar al login del portal (o a la cuenta si ya hay sesion de portal), sin errores.

## Criterios De Aceptacion

- [x] `GET /sginmo-web/portal/` responde 302 a `portal/login.xhtml` (no el error de recurso no encontrado).
- [x] Si ya hay sesion de portal activa, redirige a `portal/inicio.xhtml`.
- [x] No afecta el resto del portal ni el login administrativo.

## Dependencias

- Depende de: REQ-0078 (portal externo).
