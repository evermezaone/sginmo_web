# REQ-0067 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0067
- Tipo de cambio: BD (tabla + RLS + pantalla) + backend + UI
- Riesgo: medio (agrega auditoria a un servicio de seguridad -desbloqueo-; el resto es aditivo/nuevo)
- Archivos clave:
  - `V46__auditoria_funcional.sql`: tabla `auditoria_funcional` (tenant, entidad, registro_id, accion CHECK, campo, valor_anterior/nuevo, modulo, motivo, usuario_codigo, fecha) con RLS multiempresa e INMUTABLE (solo SELECT/INSERT; sin UPDATE/DELETE). Registra la pantalla `auditoria`.
  - `servicio/AuditoriaFuncionalService.java` (@AislarTenant): registrar/registrarAlta/registrarInactivacion(motivo obligatorio)/registrarReactivacion/registrarCambios(diff antes-despues, una fila EDITAR por campo) + consultar(filtros)/historial(entidad,registro)/entidadesConAuditoria; `exigir("auditoria","VER")` en las lecturas; enmascarado de campos sensibles (password/hash/token/clave/secret/salt -> "***").
  - `web/AuditoriaBean.java` + `webapp/auditoria.xhtml`: consulta filtrable (fecha desde/hasta, usuario, accion, campo, entidad).
  - `servicio/SeguridadPoliticaService.java`: ejemplo vivo -> el desbloqueo de usuario registra la accion DESBLOQUEAR.
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura (31 pantallas).
- Comandos probados:
  - `mvn -q clean package` (multi-modulo): BUILD OK (V46 y clases Auditoria* en el WAR).
  - Deploy + redeploy: HTTP 200 login.
  - `python tools/smoke-test-vps.py`: 31/31 RENDER OK incl. `auditoria` (su render ejecuta un SELECT sobre auditoria_funcional -> la tabla existe y la RLS no rompe el render).
- Cambios de datos: si, V46 (tabla + RLS + pantalla).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar; revisar la RLS/inmutabilidad y el enmascarado de secretos.
- Notas para auditor:
  - La auditoria es inmutable a nivel BD (sin policies de UPDATE/DELETE): nadie la altera desde la app.
  - Nunca se guardan secretos: `esSensible()` enmascara por nombre de campo.
  - Aislamiento por tenant garantizado por RLS (V28 pattern) + @AislarTenant.
  - Alcance de instrumentacion: ver "Limitaciones Conocidas".

## Resumen Funcional

Nueva pantalla "Auditoria funcional": consulta filtrable (fecha, usuario, accion, campo, entidad) del
historial de cambios de registros sensibles, mostrando quien cambio que, valor anterior/nuevo, modulo
y motivo. Solo muestra la empresa activa. No exhibe secretos.

## Resumen Tecnico

Tabla de negocio con RLS e inmutable. Servicio de registro (diff antes/despues + acciones criticas)
y consulta con permiso propio. Un ejemplo vivo (DESBLOQUEAR) demuestra el registro end-to-end.

## Archivos Modificados

Ver Manifiesto. V46 nueva; AuditoriaFuncionalService/AuditoriaBean/auditoria.xhtml nuevos;
SeguridadPoliticaService, plantilla.xhtml y smoke tocados.

## Cambios De Datos

V46: tabla `auditoria_funcional` (RLS, inmutable) + pantalla `auditoria`.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy + smoke 31/31 (auditoria renderiza). Ver test-plan.

## Pruebas Manuales Sugeridas

1. Desbloquear un usuario -> ver la fila DESBLOQUEAR en Auditoria (filtrando por accion/usuario).
2. Filtrar por fecha/usuario/accion/campo/entidad y confirmar el acotado.
3. Con dos empresas: confirmar que una no ve la auditoria de la otra (RLS).

## Limitaciones Conocidas (transparencia)

- Instrumentacion campo-a-campo de CADA maestro sensible (boton "Historial" por ABM + captura
  antes/despues + motivo obligatorio en inactivacion desde la UI): rollout INCREMENTAL. La API
  (`registrarCambios`/`registrarInactivacion`/`historial`) y el patron estan listos; se cablea un
  ejemplo vivo (DESBLOQUEAR) y la pantalla Auditoria ya hace todo consultable/filtrable. Se prioriza
  no desestabilizar los ABM ya auditados (cambiar firmas para exigir motivo tocaria sus UIs).

## Riesgos Conocidos

- Se agrego auditoria a `SeguridadPoliticaService.desbloquear`: aditivo, dentro de la transaccion;
  el smoke confirma que el login/seguridad siguen OK.
