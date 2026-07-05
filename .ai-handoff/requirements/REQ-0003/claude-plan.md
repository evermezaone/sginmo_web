# REQ-0003 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-04

## Estrategia

1. Borrador de esquema (doc 09) → revision interactiva con el usuario (observaciones de a una, sin aplicar nada a la BD).
2. Incorporar el patron documento/cobros del sistema de Gestion (auditoria doc 10).
3. Generar V2 (seed con valores reales) y V3 (ubicaciones oficiales INE 2022, generado por script).
4. Con la aprobacion explicita del usuario: aplicar V1+V2+V3 en la BD `sginmo` de la VPS y verificar conteos.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| `src/main/resources/db/migration/V1__esquema_inicial.sql` | nuevo (6 revisiones con el usuario) |
| `src/main/resources/db/migration/V2__seed_basico.sql` | nuevo |
| `src/main/resources/db/migration/V3__ubicaciones_paraguay.sql` | nuevo (generado) |
| `docs-migracion/fuentes-ine-2022/*.xlsx` | fuentes oficiales archivadas |

## Pruebas Previstas

- [x] Aplicacion sin errores en la VPS
- [x] Conteos de verificacion exactos
- [x] Jerarquia geografica navegable

## Riesgos

Ver analysis.md.

## Cambios De Datos

Creacion inicial del esquema completo (BD vacia).
