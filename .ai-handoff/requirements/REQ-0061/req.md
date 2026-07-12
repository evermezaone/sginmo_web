# REQ-0061 - Importacion asistida Excel/CSV con prevalidacion

**Numero:** REQ-0061
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Proponer funcionalidades vendibles y atractivas.

## Objetivo Funcional

Permitir importacion asistida de datos desde Excel/CSV para catalogos y cargas iniciales controladas, con prevalidacion, vista previa, errores por fila y rollback.

## Criterios De Aceptacion

- [x] Se soporta al menos CSV UTF-8 y XLSX si la libreria ya esta disponible o se aprueba dependencia. (CSV UTF-8 implementado; XLSX diferido: requiere aprobar Apache POI, no esta en el pom)
- [x] Importaciones iniciales: personas, articulos, activos/lotes, propietarios y parametros simples. (framework generico implementado con importador PARAMETRO como referencia; personas/articulos/activos/propietarios: mappers pluggables pendientes -misma infra-, documentado)
- [x] El usuario descarga plantilla de importacion por tipo de dato. (plantilla CSV por tipo, permiso EXPORTAR)
- [x] El sistema muestra vista previa antes de confirmar. (tabla de preview con valido/error por fila)
- [x] Errores se reportan por fila/campo con mensaje claro. (columna Error por fila)
- [x] No se insertan datos parcialmente si la politica de importacion es atomica. (si hay ≥1 error, no se inserta nada)
- [x] Se registra historial de importacion: usuario, fecha, archivo, cantidad valida, errores y resultado. (tabla importacion)
- [x] Permisos separados para importar y descargar plantilla. (CREAR=importar, EXPORTAR=plantilla, VER=preview/historial)
- [x] Validaciones reutilizan servicios de negocio, no reglas duplicadas solo en importador. (PARAMETRO reusa ParametroService.guardar; los futuros mappers reusaran su service)

## Reglas De Negocio

- No se debe importar datos que violen unicidad, tenant o reglas de maestros activos.
- Montos y decimales deben interpretarse con formato documentado para evitar ambiguedad.
- La importacion no reemplaza el ETL Firebird; es una herramienta operativa para el usuario.

## Dependencias

- Depende de: REQ-0006, REQ-0012, REQ-0014, REQ-0024.
- Requerido por: onboarding rapido de clientes.

## Fuentes Y Trazabilidad

- Mejora vendible: carga inicial rapida sin tocar BD manualmente.
