# Preauditoria Claude - REQ-0097

Fecha: 2026-07-14
Responsable: Claude

- [x] Lei observaciones previas aplicables (REQ nuevo).
- [x] Consulte AUDITORIA_OBSERVACION: sin observaciones pendientes.
- [x] Sin observaciones que cerrar (N/A).
- [x] Sin credenciales/tokens/hosts sensibles.
- [x] req.md sin criterios [ ] pendientes.
- [x] claude-implementation.md con Manifiesto Minimo, archivos clave y comandos probados.
- [x] test-plan.md solo afirma funcionalidades reales.
- [x] Reutilizo f_mora_cuota (no duplico la formula de mora) -> consistente con cobranza/MoraService.
- [x] Sin BD nueva; sin casts de fecha inseguros (aLocalDate).
- [x] Aislamiento por persona + RLS revisado.
- [x] Ejecute handoff:check y paso sin errores.

Notas:
- Dias de mora y multa solo cuentan en cuotas PENDIENTE con saldo>0 y vencidas; el resto ve 0/—.

## Respuesta Por Observacion Cerrada
N/A - REQ nuevo.

```text
Obs 319 (alta, alcance historicos - ampliacion del REQ):
- Problema: el REQ se amplio (puntos 8-12) a historicos: vista por defecto ano actual + todo pendiente anterior, y consulta por ano; cuotas(persona) devolvia todo sin filtro y no habia selector.
- Cambio: PortalService.cuotas(persona, Integer anio): anio=null -> ano actual + todo PENDIENTE con saldo (cualquier ano); anio!=null -> ese ano (todos los estados). Filtro por persona SIEMPRE primero (aislamiento). Nuevo aniosConCuotas(persona) para el selector. PortalBean expone anioCuotas/aniosCuotas/cambiarAnio(). inicio.xhtml agrega selectOneMenu (Ano actual + pendientes / Ano N) que recarga la grilla por ajax. La mora se calcula a hoy incluso para cuotas viejas pendientes; no se recalculan importes/saldos historicos.
- Archivos: servicio/PortalService.java, web/PortalBean.java, webapp/portal/inicio.xhtml.
- Evidencia: inicio.xhtml XML OK; Build OK; smoke 37/37.
```

