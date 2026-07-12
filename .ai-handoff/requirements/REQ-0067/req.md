# REQ-0067 - Auditoria funcional visible por registro sensible

**Numero:** REQ-0067
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer funcionalidades utiles, vendibles y atractivas para elevar el nivel del sistema.

## Objetivo Funcional

Hacer visible para administradores el historial de cambios de registros sensibles: quien cambio que, valor anterior/nuevo, fecha, modulo y motivo cuando corresponda.

## Criterios De Aceptacion

- [ ] Existe modelo de auditoria funcional consultable desde la UI para registros sensibles.
- [ ] Se registran altas, modificaciones, inactivaciones/reactivaciones y acciones criticas.
- [ ] Para cambios de campos relevantes se guarda valor anterior y valor nuevo cuando sea razonable y seguro.
- [ ] La UI muestra pestaña o boton "Historial" en ABM sensibles.
- [ ] El historial permite filtrar por fecha, usuario, tipo de accion y campo.
- [ ] Se registran motivos obligatorios en inactivacion de maestros sensibles cuando aplique.
- [ ] Permiso separado para ver auditoria.
- [ ] No se muestran datos secretos como hashes de password, tokens o credenciales.
- [ ] La auditoria respeta tenant/empresa y no permite ver cambios de otra empresa.

## Reglas De Negocio

- Maestros sensibles iniciales: persona, articulo, forma_pago, moneda, parametro_sistema, usuario, activo, operacion, cuota, cobro y plantilla_documento.
- Las acciones de cobro, anulacion, descuento, liquidacion y regeneracion de cuotas deben tener trazabilidad fuerte.
- La auditoria tecnica automatica no reemplaza la auditoria funcional visible.

## Dependencias

- Depende de: REQ-0002, REQ-0004, REQ-0006.
- Requerido por: controles internos, soporte, cumplimiento y confianza comercial.

## Fuentes Y Trazabilidad

- Mejora discutida previamente: historial de cambios visible para registros sensibles.
- Estandares SGI: auditoria automatica y capas estrictas.
