# Implementacion Claude - REQ-0011

## Manifiesto Minimo Para Codex
- ParametroSistema (entidad, clave String PK, isSensible()), ParametroService (guardar
  esNuevo con unicidad de clave + exigir permisos), ParametroBean, parametros.xhtml.
- Es la pantalla con la que el usuario carga SMTP_CLAVE para activar alertas (REQ-0004).
- Comandos probados: build+deploy; grilla con ~16 parametros reales y SMTP enmascarado.
