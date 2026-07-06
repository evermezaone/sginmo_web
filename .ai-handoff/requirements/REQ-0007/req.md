# REQ-0007 - Catalogo geografia: paises, departamentos, ciudades y barrios

**Numero:** REQ-0007 · **Estado:** implementado y validado por el usuario (2026-07-06)

## Objetivo Funcional
ABM de la geografia oficial (INE 2022, 8.276 ubicaciones ya importadas en V3) sobre la
tabla recursiva ubicacion_geografica (pais > departamento > distrito > barrio/localidad).

## Criterios De Aceptacion
- [x] Grilla lazy con las 8.276 filas paginadas, busqueda por nombre/codigo INE, orden.
- [x] Alta/edicion con nivel (lista NIVELES_UBICACION) y padre por AUTOCOMPLETE lazy
      (regla combos grandes) con converter administrado y forceSelection.
- [x] codigo_oficial unico (constraint + ErroresBd traduce el conflicto).
- [x] Validacion anti-ciclo basica (una ubicacion no puede ser su propio padre).
- [x] Baja logica con permisos por accion; pantalla 'geografia' en PANTALLAS.
