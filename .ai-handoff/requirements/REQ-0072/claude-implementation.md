# REQ-0072 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0072
- Tipo de cambio: BD (V48: param + pantalla) + backend (OcupacionService) + UI (ocupacion.xhtml/OcupacionBean)
- Riesgo: bajo (solo lectura; V48 solo agrega param + pantalla)
- Archivos clave:
  - `V48__ocupacion.sql`: parametro `OCUPACION_OBJETIVO_PCT` (global -1, default 90, grupo Gerencia) + pantalla `ocupacion`.
  - `servicio/OcupacionService.java` (@AislarTenant): resumen() (alquilables/ocupados/vacantes/ocupacionPct/objetivo/objetivoUnidades/brecha), vacantes(limite) ordenadas por prioridad comercial (precio_alquiler DESC), porTipo(). Regla ALQUILABLE = precio_alquiler>0 AND estado<>VENDIDA. Ocupado = operacion ALQUILER que cubre hoy (fecha_inicio<=hoy AND (fin IS NULL OR fin>hoy)).
  - `web/OcupacionBean.java` + `webapp/ocupacion.xhtml`: KPIs + brecha ("faltan N para el objetivo") + tabla por tipo + lista clicable de vacantes (las primeras `brecha` marcadas).
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy + Flyway V48 (migrado a version 48 en el log) + redeploy.
  - `python tools/smoke-test-vps.py`: 32/32 incl. `ocupacion` (tras corregir p:panelGrid columns=5 -> flex, requerido factor de 12).
- Cambios de datos: si, V48 (param + pantalla). Cambios de entorno: no.
- Decision esperada: aprobar; revisar la regla de "alquilable" y el calculo de brecha.
- Notas para auditor:
  - RLS/tenant: @AislarTenant; activos/operaciones filtrados por empresa; contexto global -> vacio.
  - Ocupacion es snapshot a hoy; historico por fecha se apoya en rangos de operacion (motor 0069).
  - Sucursal no aplica al universo de activos (activo no tiene sucursal); zona (ubicacion) queda como breakdown incremental.

## Resumen Funcional

Pantalla "Ocupacion": muestra alquilables, ocupados, % de ocupacion contra el objetivo (90% por
defecto, configurable), vacantes y cuantas propiedades faltan alquilar para llegar al objetivo; y lista
las propiedades vacantes por prioridad comercial, marcando las que cierran la brecha.

## Resumen Tecnico

OcupacionService @AislarTenant de solo lectura con la regla de alquilable centralizada; objetivo
parametrizable; pantalla nueva con permiso propio.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V48__ocupacion.sql | NUEVO - param objetivo + pantalla ocupacion |
| servicio/OcupacionService.java | NUEVO |
| web/OcupacionBean.java + webapp/ocupacion.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml, tools/smoke-test-vps.py | menu + cobertura |

## Cambios De Datos

V48: parametro OCUPACION_OBJETIVO_PCT (global) + pantalla ocupacion.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; Flyway V48; smoke 32/32 (ocupacion 200 tras el fix del panelGrid).

## Pruebas Manuales Sugeridas

1. Con activos alquilables y un alquiler vigente: verificar ocupacion/vacantes/brecha.
2. Cambiar OCUPACION_OBJETIVO_PCT en Parametros y ver cambiar la brecha.
3. Confirmar que la lista de vacantes son propiedades de la empresa (RLS).

## Riesgos Conocidos

- Solo lectura; riesgo bajo. Breakdown por zona/propietario diferido (incremental).
