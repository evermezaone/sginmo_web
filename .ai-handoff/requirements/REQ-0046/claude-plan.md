# REQ-0046 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-11

## Estrategia

Exponer un selector de Moneda obligatorio en el formulario de operacion, alimentado por un
metodo de catalogo `monedasActivas()` (multi-tenant), y preseleccionar Guaranies por defecto
al crear una operacion nueva. Sin migracion (usa tablas existentes).

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| webapp/operaciones.xhtml | `p:selectOneMenu` de Moneda `required="true"` con `#{operacionBean.monedas}` |
| web/OperacionBean.java | Lista `monedas` + carga desde `catalogoService.monedasActivas()` + default Guaranies en `nuevo()` |
| servicio/CatalogoService.java | Metodo `monedasActivas()` (Moneda ACTIVO, tenant -1 o propio) |

## Pruebas Previstas

- [x] Build OK (`mvn clean package`).
- [x] Render de `operaciones` OK en smoke-test post-deploy.
- [x] Alta de operacion se guarda sin error de integridad por moneda null.

## Riesgos

Medio: bug de integridad en alta de operacion. Mitigado por `required` + default Guaranies +
fallback a primera moneda visible.

## Cambios De Datos

Sin migracion. Se escribe `operacion.moneda` en altas nuevas (antes quedaba null y fallaba).
