# REQ-0046 - Codex Review

## Decision

APROBADO_POR_CODEX.

## Revision

- `operaciones.xhtml` incluye selector obligatorio de Moneda.
- `OperacionBean.nuevo()` asigna moneda por defecto, priorizando Guaranies y con fallback a primera moneda visible.
- `CatalogoService.monedasActivas()` respeta estado ACTIVO y tenant global/actual.
- El formulario ya no intenta persistir `operacion.moneda = null` en altas normales.

## Verificacion

- `mvn -q clean package`: OK.
