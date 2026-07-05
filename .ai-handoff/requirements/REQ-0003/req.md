# REQ-0003 - Esquema PostgreSQL inicial + seed de dominios y parametros

**Numero:** REQ-0003
**Fecha de creacion:** 2026-07-04
**Estado inicial:** NUEVO
**Prioridad:** alta (base de datos del sistema)

## Texto Original

Backlog doc 08 + revision interactiva del usuario (2026-07-04/05, 11 observaciones + arquitectura BD-centrica) + aprobacion final: "La base queda aprobada en su version 1, y luego ajustamos si hace falta ya al probar los procesos."

## Objetivo Funcional

Esquema PostgreSQL completo del sistema (diseñado con el usuario tabla por tabla) aplicado en la BD `sginmo` de la VPS, con seed de parametros reales, listas configurables y ubicaciones geograficas oficiales del Paraguay.

## Criterios De Aceptacion

- [x] `V1__esquema_inicial.sql`: 36 tablas + vista `v_persona`, con las convenciones del usuario (singular, PK=nombre de tabla, FKs sin _id, `descripcion` para textos, `estado` ACTIVO/INACTIVO en vez de boolean activo) y las 11 observaciones aplicadas (modelo persona/fisica/juridica/rol; `activo` recursivo; sin tabla empresa; articulo absorbe items e incluye FK impuesto con base imponible parcial; archivo_adjunto con FKs reales; codigo_oficial INE en ubicaciones).
- [x] `V2__seed_basico.sql`: 7 parametros (6 valores reales de produccion doc 07 + IMPUESTOS_MODO_AVANZADO), ~84 valores de listas en `entidad`, 4 monedas, 3 impuestos (IVA PY), 5 formas de pago con flags requiere_*, 15 articulos de servicio con `aplicacion`.
- [x] `V3__ubicaciones_paraguay.sql`: GENERADO desde los XLSX oficiales INE 2022 (fuentes archivadas en docs-migracion/fuentes-ine-2022): 1 pais + 18 departamentos + 263 distritos + 7.994 barrios/localidades, con codigo_oficial para upsert futuro.
- [x] Migraciones aplicadas en la BD `sginmo` de la VPS y verificadas: 36 tablas, 1 vista, 84 listas, 8.276 ubicaciones, 15 articulos, 7 parametros, 68 barrios de Asuncion.
- [x] Sin credenciales en los scripts.

## Dependencias

- Depende de: REQ-0001/0002.
- Requerido por: todos los modulos. La logica BD (triggers/SPs estilo Gestion) llega en migraciones V4+ (fase dinero) segun decision de arquitectura del usuario.
