# REQ-0003 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-05
**Rama:** main

## Manifiesto Minimo Para Codex

- REQ: REQ-0003
- Tipo de cambio: BD
- Riesgo: alto (esquema fundacional)
- Archivos clave:
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql`: 36 tablas + v_persona (rev final aprobada por el usuario)
  - `.../V2__seed_basico.sql`: parametros reales + listas + monedas/impuestos/formas de pago + articulos de servicio
  - `.../V3__ubicaciones_paraguay.sql`: 8.276 ubicaciones oficiales INE 2022 (generado)
  - `docs-migracion/fuentes-ine-2022/*.xlsx`: fuentes oficiales archivadas
  - `docs-migracion/09-borrador-esquema.md` + `10-auditoria-gestion-oracle.md`: diseño y especificacion fuente
- Comandos probados:
  - Aplicacion en VPS (psql ON_ERROR_STOP): V1 OK, V2 OK, V3 OK (EXIT:0)
  - Verificacion: tablas=36, vistas=1, entidad=84, ubicaciones=8276, articulos=15, parametros=7
- Cambios de datos: si — creacion completa del esquema en BD `sginmo` de la VPS (estaba vacia)
- Cambios de entorno: no
- Impacto LLM/tokens: no
- Decision esperada: aprobar
- Notas para auditor: (1) El esquema fue disenado EN VIVO con el usuario (11 observaciones aplicadas, ver historial git del V1); la aprobacion es explicita: "La base queda aprobada en su version 1". (2) Convenciones deliberadas del usuario: PK = nombre de la tabla, FKs sin `_id`, tabla generica `entidad` con FK compuesta (lista,codigo). (3) Se ejecuto via psql; cuando se integre Flyway runtime se hara baseline en V3 (documentado). (4) La logica de consistencia (triggers/SPs estilo Gestion) es arquitectura decidida y llega en V4+ (fase dinero) — NO es faltante de este REQ. (5) Sin UNIQUE por nombre en ubicacion_geografica: el INE registra homonimos; unicidad = codigo_oficial.

## Resumen Funcional

La base de datos del sistema existe: esquema completo aprobado por el usuario, con datos semilla reales y la geografia oficial completa del Paraguay.

## Resumen Tecnico

3 migraciones Flyway-format aplicadas en PostgreSQL 16 (VPS). V3 generado programaticamente desde los XLSX del INE (generador reproducible).

## Cambios De Datos

Creacion del esquema completo + seeds. Sin datos preexistentes afectados (BD nueva).

## Variables De Entorno

Sin cambios (APP_DB_* ya existian en .env).

## Pruebas Ejecutadas

Ver test-plan.md.

## Riesgos Conocidos

- El ETL del legado (REQ-0031) mapeara los datos de Firebird a este esquema nuevo (persona/activo/documento); el mapeo esta especificado en docs 02/07/09.
