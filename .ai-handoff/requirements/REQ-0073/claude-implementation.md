# REQ-0073 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0073
- Tipo de cambio: BD (V50: 2 tablas + RLS + pantalla) + backend (ObjetivoService) + UI (objetivos.xhtml/Bean)
- Riesgo: medio (tabla de negocio + ABM con calculo automatico)
- Archivos clave:
  - `V50__objetivos_gerenciales.sql`: `objetivo_gerencial` (indicador CHECK, meta, unidad, sentido, periodo, alcance, alcance_ref, moneda, umbral_adv, vigencia, estado) + `objetivo_medicion` (historial) con RLS (patron V28) + pantalla `objetivos`.
  - `servicio/ObjetivoService.java` (@AislarTenant): listar/porId con calcular() automatico; guardar/cambiarEstado (permisos objetivos/* + auditoria REQ-0067); registrarMedicion(); valorActual() reutiliza DashboardMetricasService/OcupacionService/RentabilidadService; semaforo por sentido MINIMO/MAXIMO.
  - `web/ObjetivoBean.java` + `webapp/objetivos.xhtml`: lista con semaforo + dialogo ABM + medir/inactivar/reactivar; para ocupacion muestra "faltan N prop.".
  - `servicio/DashboardMetricasService.java`: + `valorMesActual(indicador, moneda, sucursal)`.
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy + Flyway V50; `python tools/smoke-test-vps.py`: 34/34 incl. `objetivos`.
- Cambios de datos: si, V50 (2 tablas + pantalla). Cambios de entorno: no.
- Decision esperada: aprobar; revisar el calculo automatico y la validacion por unidad.
- Notas para auditor:
  - Calculo centralizado en el servicio (no en XHTML/Bean). Semaforo: MINIMO -> OK si valor>=meta; MAXIMO -> OK si valor<=meta; ADVERTENCIA con umbral (default meta*0.9 / *1.1).
  - No mezcla monedas: MONTO exige moneda; cobros/mora se calculan en esa moneda.
  - RLS en objetivo_gerencial/objetivo_medicion; ABM audita alta/edicion/inactivacion (REQ-0067). Baja logica.
  - Alcance por sucursal aplicado en el calculo; tipo/zona/propietario documentados como refinamiento del calculo por alcance. Periodos mayores (trim/anual) documentados (calculo base mensual).

## Resumen Funcional

Pantalla "Objetivos": define metas (ej. ocupacion 90%), calcula solo el valor actual, la brecha, el %
de cumplimiento y el semaforo; para ocupacion indica cuantas propiedades faltan alquilar. Historial de
mediciones para ver evolucion.

## Resumen Tecnico

Tablas de negocio con RLS; ObjetivoService con motor de calculo que reutiliza los servicios de metricas.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V50__objetivos_gerenciales.sql | NUEVO - tablas + RLS + pantalla |
| servicio/ObjetivoService.java | NUEVO |
| web/ObjetivoBean.java + webapp/objetivos.xhtml | NUEVOS |
| servicio/DashboardMetricasService.java | + valorMesActual |
| WEB-INF/plantilla.xhtml, tools/smoke-test-vps.py | menu + cobertura |

## Cambios De Datos

V50: objetivo_gerencial + objetivo_medicion (RLS) + pantalla objetivos.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; Flyway V50; smoke 34/34.

## Pruebas Manuales Sugeridas

1. Crear objetivo "ocupacion 90% MINIMO PORCENTAJE" y ver semaforo/brecha/faltan N.
2. Objetivo "mora_maxima MAXIMO MONTO" con moneda: ver semaforo si supera.
3. Registrar medicion y ver que queda historial.

## Limitaciones Conocidas

- Calculo base mensual; periodos trimestral/anual documentados (extension).
- Alcance por tipo/zona/propietario: refinamiento del calculo (hoy empresa/sucursal).

## Riesgos Conocidos

- ABM con calculo: mitigado (validaciones + permisos + RLS + auditoria).
