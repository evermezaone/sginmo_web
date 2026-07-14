# REQ-0098 - Portal socio: quitar subtitulo "Como cliente" de la cabecera

**Numero:** REQ-0098
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** baja (visual)

## Objetivo Funcional

Quitar el subtitulo redundante "Como cliente" del inicio del portal, manteniendo el saludo "Bienvenido, {nombre}".
(En REQ-0095 se agrego el saludo pero se dejo "Como cliente" como divisor; el usuario pide retirarlo.)

## Alcance

- portal/inicio.xhtml: se elimina el h2 "Como cliente" (que se mostraba cuando el socio tambien era propietario).
- Se mantiene "Como propietario" como divisor para socios con ambos roles (criterio 4 del pedido).
- Solo visual; sin cambios de permisos/secciones. Layout responsive intacto (REQ-0096).

## Criterios De Aceptacion

- [x] Al ingresar como cliente, la cabecera muestra "Bienvenido, {nombre}" y ya NO "Como cliente".
- [x] Con rol cliente+propietario tampoco aparece "Como cliente".
- [x] Espaciado correcto en desktop y mobile.
- [x] Build (mvn clean package) OK.

## Dependencias
- Correccion visual de REQ-0095. Base: portal/inicio.xhtml.
