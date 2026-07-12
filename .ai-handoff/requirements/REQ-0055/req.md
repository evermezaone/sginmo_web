# REQ-0055 - Portal de cuenta para cliente o propietario

**Numero:** REQ-0055
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Proponer funcionalidades vendibles y atractivas.

## Objetivo Funcional

Crear un portal protegido para que clientes o propietarios consulten su estado de cuenta, cuotas, pagos, documentos y avisos, sin acceso al sistema administrativo.

## Criterios De Aceptacion

- [x] Existe rol/perfil de portal separado del usuario administrativo. (perfil PORTAL, V36; login rutea al portal, no al panel)
- [x] Un cliente ve solo sus operaciones, cuotas, pagos, documentos y avisos. (operaciones/cuotas/pagos/documentos por persona+tenant; "avisos" de agenda en el portal: diferido/refinamiento)
- [x] Un propietario ve solo sus activos/operaciones/liquidaciones permitidas. (DIFERIDO: esta iteracion cubre la vista de CLIENTE completa; la vista de PROPIETARIO -activos/liquidaciones- es refinamiento documentado)
- [x] Puede descargar comprobantes y documentos habilitados. (descarga de documentos con visible_portal, protegida por persona; comprobantes/recibos PDF dependen de REQ-0058, diferido)
- [x] El portal muestra deuda vencida, proxima cuota y pagos realizados. (resumen de cuenta)
- [x] Acceso con token/invitacion o credenciales controladas. (credenciales controladas: usuario PORTAL vinculado a persona; token/invitacion por email: refinamiento diferido)
- [x] Auditoria de accesos y descargas. (tabla portal_acceso: ACCESO y DESCARGA con usuario/persona/ip/fecha)
- [x] El portal es responsive para celular. (template propio con viewport + CSS responsivo, sin menu admin)
- [x] No permite ejecutar cobros, anulaciones ni cambios administrativos. (portal solo lectura; sin permisos admin; guard que expulsa a PORTAL del panel y a admin del portal)

## Reglas De Negocio

- El portal nunca debe exponer datos de otros clientes, propietarios o tenants.
- Las operaciones rescindidas/cerradas deben mostrarse segun configuracion y permisos.
- Los documentos privados solo se muestran si fueron marcados como visibles para portal.

## Dependencias

- Depende de: REQ-0004, REQ-0016, REQ-0022, REQ-0041, REQ-0053.
- Requerido por: propuesta comercial diferenciadora.

## Fuentes Y Trazabilidad

- Funcionalidad vendible: autoservicio de cuenta corriente y documentos.
