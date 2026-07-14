# REQ-0091 - Portal socio: panel lateral de pagos e historial unificado (caja + transferencia)

**Numero:** REQ-0091
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

"...al costado derecho de forma estetica y profesional, ver mis pagos, si hice en caja, o por transferencia..."

## Objetivo Funcional

En la vista de cuenta del socio (portal/inicio.xhtml) mostrar, en un panel lateral derecho estetico y
profesional, el historial de pagos del socio indicando el canal de cada pago (CAJA o TRANSFERENCIA) y su
estado. Hoy PortalService.pagos() ya lista cobros pero NO expone el canal ni tiene un layout dedicado.

## Alcance

- Extender PortalService.pagos() para devolver por pago: fecha, monto, moneda, canal (CAJA / TRANSFERENCIA),
  estado y referencia/numero de comprobante si existe. Canal derivable de la forma de pago (TRANSFERENCIA = 'TRF').
- Rediseñar inicio.xhtml con un panel lateral derecho responsive (colapsa en mobile) con badges de canal/estado;
  el resumen de cuenta queda como contenido principal.
- Solo lectura; respeta aislamiento por persona + tenant (RLS) como el resto del portal.

## Criterios De Aceptacion

- [x] El socio ve un panel lateral derecho con sus pagos, ordenados por fecha desc.
- [x] Cada pago indica claramente el canal (CAJA / TRANSFERENCIA) y su estado.
- [x] Diseño responsive y consistente con el estilo del portal (profesional, sin romper mobile).
- [x] Un socio nunca ve pagos de otro socio ni de otra empresa.

## Dependencias

- Relacionado con REQ-0092 (las transferencias, al aplicarse como cobro, aparecen tambien aca).
- Base existente: PortalService.pagos(), portal/inicio.xhtml.
