# REQ-0002 - Dominio base: Auditable, listener de auditoria y enums

**Numero:** REQ-0002
**Fecha de creacion:** 2026-07-04
**Estado inicial:** NUEVO
**Prioridad:** alta (lo usan todas las entidades JPA)

## Texto Original

Backlog doc 08, Fase 1: "Superclase Auditable + listener JPA (usuario/fecha creacion-modificacion desde sesion) + enums con valores reales del doc 07 s3 (EstadoCuota, EstadoOperacion, TipoOperacion, etc.)".

## Objetivo Funcional

Toda entidad del dominio hereda auditoria automatica (usuario/fecha de creacion y modificacion, sin setearla a mano — corrige la practica del legado donde los forms la poblaban) y todos los estados/tipos del negocio existen como enums Java tipados con los valores EXACTOS relevados de la BD real (doc 07 §3), listos para `@Enumerated(STRING)`.

## Criterios De Aceptacion

- [x] `Auditable` (`@MappedSuperclass`) con `usuarioCreacion`, `fechaCreacion` (NOT NULL), `usuarioModificacion`, `fechaModificacion`, poblados por `AuditoriaListener` (`@PrePersist`/`@PreUpdate`), nunca manualmente.
- [x] El listener obtiene el usuario del contexto de sesion via interfaz CDI `UsuarioActual` (implementacion definitiva llega con REQ-0004/0005); fallback "sistema" si no hay sesion (jobs/ETL).
- [x] Enums en `dominio.enums`, todos con etiqueta legible y valores EXACTOS del doc 07 §3: EstadoOperacion, EstadoCuota, EstadoCobro, EstadoPropiedad, EstadoIngresoEgreso, TipoOperacion, CondicionOperacion, TipoContrato, TipoFinanciacion, TipoEntidadInmobiliaria, TipoPropiedad, TipoMovimiento, TipoItemIngresoEgreso, TipoImputacion, TipoDocumentoIdentidad, TipoPersoneria, Perfil, TipoGasto, TipoMoneda, TipoDocumentoCobro.
- [x] Test unitario que verifica los valores criticos de los enums contra la especificacion (doc 07) y el comportamiento del listener (`mvn test` EXIT:0).
- [x] `mvn -q package` EXIT:0.
- [x] Sin credenciales ni secretos.

## Dependencias

- Depende de: REQ-0001 (esqueleto).
- Requerido por: REQ-0003 (esquema/entidades) y todos los modulos.
