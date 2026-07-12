# REQ-0057 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0057
- Tipo de cambio: BD (2 tablas + RLS + pantalla) + backend + UI
- Riesgo: medio-alto (mora/cobranza; toca reglas de dinero -pero no las modifica-)
- Archivos clave:
  - `V38__mora_cobranza.sql`: tablas `gestion_cobranza` y `promesa_pago` (por-tenant, RLS inline) + pantalla `cobranza`.
  - `dominio/cobranza/GestionCobranza.java`, `PromesaPago.java`: entidades (Auditable).
  - `servicio/MoraService.java`: @AislarTenant; carteraVencida (usa `f_mora_cuota`, cap 1000), registrarGestion, registrarPromesa, cambiarEstadoPromesa. No modifica cronograma_cuota.
  - `servicio/AgendaService.java`: generarAutomaticos ahora crea evento PROMESA para promesas PENDIENTE vencidas (dedup).
  - `web/MoraBean.java` + `webapp/cobranza.xhtml`: cartera con filtros + export CSV + dialogo (gestiones/promesas).
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V38 en `BEGIN...ROLLBACK`: 2 tablas + 8 politicas RLS + pantalla + insert de promesa OK.
  - Deploy + Flyway V38 `success=t`; `python tools/smoke-test-vps.py`: 24/24 RENDER OK incl. `cobranza`.
- Cambios de datos: si, V38 (2 tablas nuevas + pantalla). Sin tocar cronograma_cuota/cobro.
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: revisar reglas de mora (usa f_mora_cuota) y aislamiento.
- Notas para auditor:
  - "Misma logica de mora que cobros, sin duplicar": la mora en dinero sale de `f_mora_cuota(cuota, current_date)`; los dias = current_date - fecha_vencimiento.
  - "No modificar el monto de la cuota desde cobranza": la cartera es solo lectura sobre cronograma_cuota; gestiones/promesas son tablas aparte.
  - "La promesa no es un pago ni cambia el estado de la cuota": promesa_pago no toca cronograma_cuota.
  - Aislamiento por tenant (RLS) en ambas tablas; escrituras exigen permiso EDITAR.

## Resumen Funcional

Nueva pantalla "Mora y cobranza": cartera vencida (dias de mora, saldo, mora) con filtros y export CSV;
por cada fila, un dialogo para registrar gestiones de cobranza y promesas de pago, y cerrar promesas
(Cumplida/Incumplida). Las promesas incumplidas aparecen automaticamente en la Agenda.

## Resumen Tecnico

MoraService @AislarTenant: cartera via native query sobre cronograma_cuota + f_mora_cuota; gestiones y
promesas como entidades JPA. Estados varchar+CHECK. Integracion con la agenda (promesa vencida -> evento).

## Limitaciones Conocidas (transparencia)

- Cierre automatico de la promesa al cobrar la cuota: DIFERIDO (se permite el cierre manual Cumplida/Incumplida);
  el enganche automatico al flujo de cobro es refinamiento.
- Filtros de cliente/operacion: soportados en el servicio; combos UI acotados (dias/monto/moneda en la vista).
- Reasignar: se maneja con el campo responsable de la gestion (permiso EDITAR); no hay reasignacion masiva.

## Archivos Modificados

Ver Manifiesto. V38 nueva; AgendaService editado (promesas vencidas).

## Cambios De Datos

V38: crea `gestion_cobranza` y `promesa_pago` (vacias) con RLS per-tenant + pantalla `cobranza`.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V38 rollback OK (incl. insert de promesa); deploy + Flyway success; smoke 24/24. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Con cuotas vencidas en una empresa: abrir Mora y cobranza, filtrar por dias, registrar una gestion y una promesa.
2. Con una promesa vencida: abrir la Agenda -> aparece el evento PROMESA.

## Riesgos Conocidos

- Toca el dominio de mora (pero reutiliza f_mora_cuota y no modifica cuotas). Ver "Limitaciones".
