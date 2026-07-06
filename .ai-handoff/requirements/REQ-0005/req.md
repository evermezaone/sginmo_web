# REQ-0005 - Layout PrimeFaces, menu por rol y contexto empresa-sucursal

**Numero:** REQ-0005 · **Estado:** implementado y validado por el usuario (2026-07-06)

## Objetivo Funcional
Layout corporativo unico para todo el sistema: menu lateral con secciones, items visibles
segun permiso VER de cada pantalla, barra superior con usuario/cambiar clave/salir,
tablero de inicio con tarjetas por permiso, responsive (menu arriba en pantallas angostas).

## Criterios De Aceptacion
- [x] Plantilla Facelets unica (/WEB-INF/plantilla.xhtml) aplicada a TODAS las pantallas,
      incluidas las del modulo ONEsystem-security (convencion: el JAR usa la plantilla del anfitrion).
- [x] Menu gateado por permisos (sesionUsuario.puede(pantalla,'VER')).
- [x] Barra superior: usuario logueado, perfil, cambiar contrasena, cerrar sesion.
- [x] index.xhtml = tablero de tarjetas filtradas por permiso.
- [x] Responsive validado por el usuario desde el celular.

## Bloqueo Formal Documentado
El "contexto empresa-sucursal en sesion" (selector de empresa/sucursal activa) queda
DIFERIDO a REQ-0009 (Empresas y sucursales): hoy no existen datos de sucursal ni pantallas
que dependan del contexto. Decision registrada en docs-migracion/11 (regla 2 del estudio).
