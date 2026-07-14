# REQ-0095 - Portal socio: encabezado de bienvenida amigable

**Numero:** REQ-0095
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** baja (cosmetico)

## Texto Original

"ya se que el nombre del usuario aparece arriba a la derecha, ese esta bien y queda alli. Pero en vez del
texto del encabezado 'Como cliente', pone algun texto amigable como Bienvenido Fulano de Tal, bien grande y
legible."

## Objetivo Funcional

Que la pantalla de inicio del portal reciba al socio con un saludo amigable, grande y legible
("Bienvenido, {nombre}"), en lugar de arrancar con el rotulo frio "Como cliente". El nombre en la barra
superior derecha se mantiene sin cambios.

## Alcance

- portal/inicio.xhtml: agregar un encabezado h1 al tope del contenido con "Bienvenido, #{portalBean.nombreUsuario}",
  grande (~1.8rem) y legible.
- Se mantienen los rotulos "Como cliente" / "Como propietario" como divisores de seccion (solo relevantes
  para socios con ambos roles); dejan de ser el encabezado principal.
- Solo UI; sin cambios de backend, datos ni permisos.

## Criterios De Aceptacion

- [x] Al entrar al portal, el socio ve "Bienvenido, {su nombre}" grande y legible arriba.
- [x] El nombre de la barra superior derecha se mantiene igual.
- [x] Los divisores de seccion siguen ordenando cliente/propietario para socios con ambos roles.
- [x] Sin cambios de backend/datos.

## Dependencias

- Base: portal/inicio.xhtml, portalBean.nombreUsuario (sesion.getNombre()).
