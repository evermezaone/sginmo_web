# REQ-0089 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0089
- Tipo de cambio: backend (PersonaService, PersonaBean). Sin BD, sin UI nueva.
- Riesgo: medio (persistencia de roles / aislamiento por tenant).
- Archivos clave:
  - `servicio/PersonaService.java`: nuevo `reconciliarRoles(personaId, rolesDeseados)` (@Transactional): compara
    los roles del tenant contra la lista deseada; reactiva/inserta los que faltan y da de baja logica (INACTIVO)
    los activos que ya no estan. Respeta `perteneceAlTenant` y `tenant.actual()`; preserva historial.
  - `web/PersonaBean.java`: `agregarRol`/`quitarRol` ahora son DIFERIDOS (solo modifican la lista en memoria);
    `guardar()` llama a `reconciliarRoles()` (reemplaza `guardarRolesPendientes`, que solo insertaba).
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS; `python tools/smoke-test-vps.py`: 36/36 (personas 200).
- Cambios de datos: no. Cambios de entorno: no.
- Decision esperada: aprobar; el fix hace que editar roles persista (insert + baja logica) al Guardar.
- Notas para auditor:
  - Baja logica (INACTIVO), no delete: preserva trazabilidad de operaciones/cobros/activos que referencian el rol.
  - Reconciliacion acotada al tenant (WHERE r.tenant = actual); no toca roles de otra empresa.
  - Los metodos `agregarRol(String)`/`quitarRol(Long)` del service quedan como API (por si otros flujos los usan);
    el ABM ya no los invoca.

## Resumen Funcional

Al editar una persona, agregar/quitar roles y Guardar, los cambios se persisten: se insertan los nuevos y se dan
de baja (logica) los quitados. Antes el Guardar solo insertaba y nunca quitaba.

## Resumen Tecnico

Modelo diferido en el bean + `PersonaService.reconciliarRoles` que concilia la lista editada contra la BD por tenant.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| servicio/PersonaService.java | nuevo reconciliarRoles (insert/reactivar + baja logica por tenant) |
| web/PersonaBean.java | agregar/quitar rol diferidos; guardar() reconcilia |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy VPS; smoke 36/36 (personas 200).

## Pruebas Manuales Sugeridas

1. Editar una persona: agregar un rol y Guardar -> el rol queda asignado (persiste).
2. Editar: quitar un rol y Guardar -> el rol se da de baja (no aparece; historial preservado).
3. Combinar alta+baja en una edicion -> ambos cambios persisten.
4. Alta de persona nueva con roles -> se insertan al guardar.

## Limitaciones Conocidas

- Baja logica (no borra fisicamente) para preservar referencias historicas.

## Riesgos Conocidos

- Persistencia de roles: mitigada con reconciliacion transaccional acotada al tenant y preservacion de historial.
