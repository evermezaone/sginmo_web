# REQ-0075 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0075
- Tipo de cambio: BD (V51: tabla + RLS + param + pantalla) + backend (AlertaService) + UI (alertas.xhtml/Bean)
- Riesgo: medio (tabla de negocio + generacion automatica)
- Archivos clave:
  - `V51__alertas_gerenciales.sql`: `alerta_gerencial` (tipo, indicador, prioridad CHECK, causa, impacto, accion_sugerida, drill_clave/ref, hash_dedup, estado, motivo/usuario/fecha de cierre) con RLS + unique parcial de dedup sobre ABIERTA + param CONTRATOS_AVISO_DIAS + pantalla `alertas`.
  - `servicio/AlertaService.java` (@AislarTenant): generar() (idempotente por hash) desde objetivos (semaforo != OK) + rentabilidad negativa + contratos por vencer (param); listar() por prioridad; cerrar(REVISADA/DESCARTADA) con motivo obligatorio al descartar + auditoria (REQ-0067).
  - `web/AlertaBean.java` + `webapp/alertas.xhtml`: lista con prioridad + causa/impacto/accion + ver evidencia (0074) + revisar/descartar (dialogo con motivo).
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy + Flyway V51; `python tools/smoke-test-vps.py`: 36/36 incl. `alertas`.
- Cambios de datos: si, V51 (tabla + param + pantalla). Cambios de entorno: no.
- Decision esperada: aprobar; revisar el dedup y la generacion automatica.
- Notas para auditor:
  - Reglas configurables: objetivos (REQ-0073) aportan umbrales; parametro CONTRATOS_AVISO_DIAS; sin numeros hardcodeados.
  - Dedup: unique index parcial (tenant, hash_dedup) WHERE estado='ABIERTA' + chequeo en upsert; misma alerta no se duplica.
  - Cierre: DESCARTADA exige motivo; guarda usuario+fecha; auditado. Baja logica (historial).
  - RLS por tenant; permisos alertas/VER y alertas/EDITAR. Envio por email/notificacion: preparado (modelo completo), no implementado.

## Resumen Funcional

Pantalla "Alertas": muestra automaticamente desviaciones accionables (objetivos en riesgo/incumplidos,
rentabilidad negativa, contratos por vencer) con causa, impacto, accion sugerida y prioridad, enlazando
a la evidencia. El usuario puede marcarlas revisadas o descartarlas con motivo.

## Resumen Tecnico

Tabla de negocio con RLS + dedup; AlertaService genera desde objetivos + reglas configurables.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V51__alertas_gerenciales.sql | NUEVO - tabla + RLS + param + pantalla |
| servicio/AlertaService.java | NUEVO |
| web/AlertaBean.java + webapp/alertas.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml, tools/smoke-test-vps.py | menu + cobertura |

## Cambios De Datos

V51: alerta_gerencial (RLS) + param CONTRATOS_AVISO_DIAS + pantalla alertas.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; Flyway V51; smoke 36/36.

## Pruebas Manuales Sugeridas

1. Definir objetivo "ocupacion 90%" no cumplido -> Recalcular -> aparece alerta CRITICA/ALTA con evidencia.
2. Descartar una alerta -> exige motivo; queda auditada y no vuelve a duplicarse.
3. Contratos con fin dentro de N dias -> alerta MEDIA.

## Limitaciones Conocidas

- Cobros-bajo-mes-anterior como regla adicional (el motor MoM ya lo detecta): incremental.
- Envio por email/notificacion: preparado, no implementado (no exigido en v1).

## Riesgos Conocidos

- Generacion automatica: idempotente por dedup; cierre auditado.
