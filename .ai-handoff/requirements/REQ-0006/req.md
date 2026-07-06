# REQ-0006 - Componentes genericos ABM y buscador modal

**Numero:** REQ-0006 · **Estado:** implementado y validado por el usuario (2026-07-06)

## Objetivo Funcional
Patron estandar de ABM replicable a todo el sistema, con implementacion de referencia
completa (Articulos) y contrato minimo repetible documentado.

## Criterios De Aceptacion
- [x] ABM de referencia (Articulos) con: lazy real, orden por columna, busqueda global,
      busqueda avanzada con boton, selector de columnas, export CSV/XML/PDF por permiso,
      dialogo con pestanas, validaciones con etiqueta, dup-check antes del submit,
      concurrencia optimista, errores de BD traducidos, validacion de dominios en Service,
      reactivacion segura, estado/habilitado, Duplicar, Mi vista por usuario, permisos por
      accion + modo solo lectura, auditoria solo con VER_AUDITORIA.
- [x] Estandar documentado y aprobado por el usuario: docs-migracion/11-estandar-abm-propuesta.md
      (matriz de decisiones de 2 estudios completos + contrato minimo repetible).
- [x] Replicado a 6 ABMs de catalogo (REQ-0007/0008/0010/0011).

## Decision Documentada (reemplaza "buscador modal")
El buscador modal del titulo se reemplazo por la regla 5.1 del estudio: combos grandes
usan p:autoComplete lazy con converter administrado (primer caso real: padre en geografia).
Decision del usuario en el estudio del estandar.
