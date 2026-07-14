# REQ-0096 - Portal socio: layout full responsive para celular

**Numero:** REQ-0096
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

"que tambien esa pantalla sea full responsive para ver en el celular preferentemente."

## Objetivo Funcional

Que el portal del socio se vea bien en celular (mobile-first): sin desbordes horizontales, header
ordenado, tarjetas y tablas legibles y adaptadas al ancho de pantalla.

## Alcance

Solo CSS del template del portal (WEB-INF/portal.xhtml), que cubre inicio.xhtml y transferencia.xhtml:
- box-sizing:border-box global para evitar desbordes por padding.
- Header (.portal-top): flex-wrap; en pantallas chicas la marca ocupa una fila y las acciones (nombre,
  informar, salir) bajan y envuelven; tap targets con padding.
- Tarjetas (.tarjetas-portal): auto-fit; en <=640px pasan a 2 columnas; en <=380px a 1 columna.
- Tablas (table.tp): cada .seccion-p tiene overflow-x propio con celdas nowrap -> una tabla ancha
  scrollea dentro de su tarjeta y NUNCA desborda la pagina.
- h1 de bienvenida escalado en mobile (1.45rem) para no romper el ancho.
- Sin cambios de backend, datos ni logica.

## Criterios De Aceptacion

- [x] En celular no hay scroll horizontal de la pagina; las tablas anchas scrollean dentro de su seccion.
- [x] El header no se solapa: marca y acciones se acomodan/envuelven.
- [x] Las 4 tarjetas de resumen se ven 2x2 en celular (1 columna en pantallas muy angostas).
- [x] El saludo "Bienvenido, {nombre}" se ve completo y legible en mobile.
- [x] En escritorio se mantiene el layout actual (ancho maximo 900px).

## Dependencias

- Base: WEB-INF/portal.xhtml (template), portal/inicio.xhtml, portal/transferencia.xhtml.
- Relacionado con REQ-0095 (encabezado de bienvenida).
