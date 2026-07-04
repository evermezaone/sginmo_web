# REQ-0002 - Analisis

**Estado:** EN_ANALISIS
**Fecha:** 2026-07-04
**Analista:** Implementador (Claude)

## Analisis Funcional

Todas las tablas del legado comparten auditoria (USUARIO/FECHA_CREACION/MODIFICACION, doc 02) y sus estados/tipos viven como strings o en la tabla DOMINIOS. En la web: auditoria automatica por listener JPA (el legado la poblaba a mano en los forms, con inconsistencias como FECHA_MODIFICACION comentada en UsuariosService) y enums Java tipados con los valores EXACTOS de la BD real (doc 07 §3).

## Analisis Tecnico

- `dominio.base`: `Auditable` (@MappedSuperclass, columnas snake_case per estandar BD) + `AuditoriaListener` (@PrePersist/@PreUpdate) + interfaz `UsuarioActual` (CDI). El listener resuelve el usuario via `CDI.current()` con fallback "sistema" (sin contenedor: tests, jobs, ETL). La implementacion definitiva de `UsuarioActual` llega con la sesion (REQ-0004/0005).
- `dominio.enums`: 20 enums con etiqueta legible. **Regla critica: los nombres coinciden con los codigos del legado** (ej. PERFIS/PERJUR) porque el ETL (REQ-0031) migra por nombre — no renombrar.
- Decision documentada: MOTIVO_LIQUIDACION no se tipifica como enum (en la BD real tiene un solo valor generico); queda como parametrica editable (REQ-0025 decidira su forma final).

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| Renombrar un valor rompe el ETL futuro | baja | alto | test DominioBaseTest fija los nombres contra la especificacion |
| CDI.current() sin contenedor lanza excepcion | media | bajo | capturada; fallback "sistema" + test que lo verifica |

**Semaforo Codex:** medio (fundacion del dominio; sin datos ni transacciones todavia)

## Preguntas Abiertas

- [x] Ninguna

## Impacto En Costos / LLM

- Aumenta tokens por mensaje: no
- Agrega llamadas extra al LLM: no
- Puede resolverse sin IA: si

## Impacto En Datos

- Requiere migracion: no (el esquema llega en REQ-0003)
- Tablas/colecciones afectadas: ninguna todavia

## Recomendacion

**Desarrollar**
