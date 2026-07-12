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

- [ ] Se soporta al menos CSV UTF-8 y XLSX si la libreria ya esta disponible o se aprueba dependencia.
- [ ] Importaciones iniciales: personas, articulos, activos/lotes, propietarios y parametros simples.
- [ ] El usuario descarga plantilla de importacion por tipo de dato.
- [ ] El sistema muestra vista previa antes de confirmar.
- [ ] Errores se reportan por fila/campo con mensaje claro.
- [ ] No se insertan datos parcialmente si la politica de importacion es atomica.
- [ ] Se registra historial de importacion: usuario, fecha, archivo, cantidad valida, errores y resultado.
- [ ] Permisos separados para importar y descargar plantilla.
- [ ] Validaciones reutilizan servicios de negocio, no reglas duplicadas solo en importador.

## Reglas De Negocio

- No se debe importar datos que violen unicidad, tenant o reglas de maestros activos.
- Montos y decimales deben interpretarse con formato documentado para evitar ambiguedad.
- La importacion no reemplaza el ETL Firebird; es una herramienta operativa para el usuario.

## Dependencias

- Depende de: REQ-0006, REQ-0012, REQ-0014, REQ-0024.
- Requerido por: onboarding rapido de clientes.

## Fuentes Y Trazabilidad

- Mejora vendible: carga inicial rapida sin tocar BD manualmente.
