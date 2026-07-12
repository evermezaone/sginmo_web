# REQ-0060 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0060
- Tipo de cambio: BD (extiende parametro_sistema + seed) + backend + UI menor
- Riesgo: medio (config usada por servicios; un mal valor no debe romper nada)
- Archivos clave:
  - `V41__parametros_avanzados.sql`: ALTER `parametro_sistema` (tipo/grupo/valor_defecto) + seed de 9 parametros globales documentados.
  - `dominio/catalogo/ParametroSistema.java`: campos tipo/grupo/valorDefecto.
  - `servicio/ParametroConfig.java`: NUEVO — lee el valor efectivo (empresa sobre global -1) con cache; texto/entero/decimal/booleano con default tolerante.
  - `servicio/ParametroService.java`: invalida la cache al guardar.
  - `servicio/AgendaService.java`: lee `AGENDA_DIAS_ALERTA` desde ParametroConfig (ya no constante).
  - `webapp/parametros.xhtml`: columna Grupo (agrupacion).
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V41 en `BEGIN...ROLLBACK`: 3 columnas + 9 seeds; query de override devuelve el valor global (30).
  - Deploy + Flyway V41 `success=t`; `python tools/smoke-test-vps.py`: 26/26 RENDER OK.
- Cambios de datos: si, V41 (ALTER + seed de defaults globales tenant=-1).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (riesgo medio) + revisar tolerancia a mal-tipado.
- Notas para auditor:
  - "Mal parametro no rompe cobros/montos/documentos": ParametroConfig cae al default del llamador si el valor falta o no parsea (entero/decimal/booleano con try/catch).
  - "Tenant especifico sobre global": query `WHERE clave AND tenant IN (:t,-1) ORDER BY tenant DESC LIMIT 1`.
  - Cache invalidada en ParametroService.guardar (sin desincronizar cambios criticos).

## Resumen Funcional

Se centraliza la configuracion en `parametro_sistema` con metadatos (tipo, grupo, default) y 9 parametros
iniciales (mora, caja obligatoria, dias alerta contrato, limite export, pie comprobante, logo, politica
documental, agenda). Los servicios leen los parametros via ParametroConfig (ej.: AgendaService). La
pantalla de parametros muestra el grupo.

## Resumen Tecnico

ParametroConfig @AislarTenant resuelve el valor efectivo (empresa sobre default global) con cache
concurrente invalidada al guardar. Tolerante a valores ausentes/mal tipados (cae al default del llamador).

## Limitaciones Conocidas (transparencia)

- Alcance por SUCURSAL: DIFERIDO; el alcance actual es empresa (tenant) + default global. Cambiar la PK a
  (clave, tenant, sucursal) es un refinamiento mayor.
- Migracion de TODAS las constantes a parametros: se demostro con AgendaService; el resto migra
  incrementalmente ahora que existe la infra (ParametroConfig).
- Editor tipado en la UI (validar por tipo al guardar): refinamiento; hoy valida el ParametroConfig al leer.

## Archivos Modificados

Ver Manifiesto. V41 nueva.

## Cambios De Datos

V41: ALTER `parametro_sistema` (tipo/grupo/valor_defecto) + seed de 9 parametros globales.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V41 rollback OK (override verificado); deploy + Flyway success; smoke 26/26. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Parametros -> ver el grupo; sobrescribir AGENDA_DIAS_ALERTA para una empresa y verificar que la agenda usa el nuevo valor.

## Riesgos Conocidos

- Config consumida por servicios: mitigado por defaults tolerantes. Ver "Limitaciones".
