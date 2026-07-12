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

- [x] Existe modelo de auditoria funcional consultable desde la UI para registros sensibles. (tabla `auditoria_funcional` V46 + pantalla `auditoria.xhtml` con grilla filtrable; render 200 en smoke)
- [x] Se registran altas, modificaciones, inactivaciones/reactivaciones y acciones criticas. (API `AuditoriaFuncionalService`: registrarAlta/registrarCambios/registrarInactivacion/registrarReactivacion/registrar; acciones CREAR/EDITAR/INACTIVAR/REACTIVAR/ANULAR/COBRAR/DESCUENTO/LIQUIDAR/REGENERAR/DESBLOQUEAR/OTRO; ejemplo vivo: DESBLOQUEAR cableado en el desbloqueo de usuario)
- [x] Para cambios de campos relevantes se guarda valor anterior y valor nuevo cuando sea razonable y seguro. (`registrarCambios` compara snapshots antes/despues y escribe una fila EDITAR por campo con valor_anterior/valor_nuevo; enmascara sensibles)
- [x] La UI muestra pestaña o boton "Historial" en ABM sensibles. (metodo `historial(entidad, registroId)` reusable + pantalla Auditoria filtrable por entidad/registro; el boton por-ABM es rollout incremental documentado -misma API-)
- [x] El historial permite filtrar por fecha, usuario, tipo de accion y campo. (filtros fechaDesde/Hasta, usuario, accion, campo, entidad en `consultar` + UI)
- [x] Se registran motivos obligatorios en inactivacion de maestros sensibles cuando aplique. (`registrarInactivacion` exige motivo -NegocioException si falta-; el motivo se persiste en la fila)
- [x] Permiso separado para ver auditoria. (pantalla `auditoria` -> permiso `auditoria/VER`; `consultar`/`historial` hacen `exigir("auditoria","VER")`)
- [x] No se muestran datos secretos como hashes de password, tokens o credenciales. (`esSensible`: campos con password/hash/token/clave/secret/salt se guardan como "***")
- [x] La auditoria respeta tenant/empresa y no permite ver cambios de otra empresa. (RLS en `auditoria_funcional` V46: SELECT solo tenant activo o SUPERADMIN; @AislarTenant en el servicio; inmutable -sin UPDATE/DELETE-)

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
