# REQ-0040 - Auto-auditoria Codex

**Fecha:** 2026-07-10T16:22:56-03:00  
**Resultado:** APROBADO

## Revision

- El encabezado ya no depende directamente de `fisica != null` en el XHTML; usa una propiedad estable del bean.
- Roles se pueden agregar durante el alta y quedan visibles en la tabla antes de guardar.
- Al confirmar el alta, los roles pendientes se guardan en la tabla `persona_rol` mediante el servicio existente, que valida permisos, tenant y duplicados.
- La edicion de personas existentes conserva el flujo previo.

## Verificacion

```text
mvn -q clean package
EXIT 0
```

