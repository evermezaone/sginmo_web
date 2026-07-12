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

- [ ] Existe rol/perfil de portal separado del usuario administrativo.
- [ ] Un cliente ve solo sus operaciones, cuotas, pagos, documentos y avisos.
- [ ] Un propietario ve solo sus activos/operaciones/liquidaciones permitidas.
- [ ] Puede descargar comprobantes y documentos habilitados.
- [ ] El portal muestra deuda vencida, proxima cuota y pagos realizados.
- [ ] Acceso con token/invitacion o credenciales controladas.
- [ ] Auditoria de accesos y descargas.
- [ ] El portal es responsive para celular.
- [ ] No permite ejecutar cobros, anulaciones ni cambios administrativos.

## Reglas De Negocio

- El portal nunca debe exponer datos de otros clientes, propietarios o tenants.
- Las operaciones rescindidas/cerradas deben mostrarse segun configuracion y permisos.
- Los documentos privados solo se muestran si fueron marcados como visibles para portal.

## Dependencias

- Depende de: REQ-0004, REQ-0016, REQ-0022, REQ-0041, REQ-0053.
- Requerido por: propuesta comercial diferenciadora.

## Fuentes Y Trazabilidad

- Funcionalidad vendible: autoservicio de cuenta corriente y documentos.
