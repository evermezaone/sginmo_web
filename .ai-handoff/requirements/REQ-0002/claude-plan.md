# REQ-0002 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-04

## Estrategia

1. Paquete `dominio.base`: interfaz `UsuarioActual`, superclase `Auditable`, `AuditoriaListener`.
2. Paquete `dominio.enums`: 20 enums con valores exactos del doc 07 §3 y etiqueta legible.
3. Test `DominioBaseTest`: fija los nombres de los enums contra la especificacion + verifica el listener (fallback "sistema", fechas pobladas).
4. `mvn package` (compila + surefire).

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| `src/main/java/.../dominio/base/{UsuarioActual,Auditable,AuditoriaListener}.java` | nuevos |
| `src/main/java/.../dominio/enums/*.java` (20) | nuevos |
| `src/test/java/.../dominio/DominioBaseTest.java` | nuevo |

## Pruebas Previstas

- [x] `mvn package` EXIT:0
- [x] Surefire: 12 tests, 0 fallos

## Riesgos

Ver analysis.md.

## Cambios De Datos

Sin cambios.
