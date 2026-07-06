# Implementacion Claude - REQ-0013

## Manifiesto Minimo Para Codex
Cubierto por la tabla `activo` recursiva (reemplaza entidades_inmobiliarias del legado, decision de diseno). ABM de activos con contenedor (padre) por autocomplete, propietarios (activo_propietario, personas con rol PROPIETARIO), y atributos parametrizables por tipo (obligatoriedad validada). V15 pantalla.

**Archivos clave:** Activo/ActivoPropietario/ActivoAtributoValor, ActivoService, ActivoBean, ActivoConverter, activos.xhtml, V15.

**Comandos probados:** mvn -q clean package (multi-modulo, EXIT 0); tools/deploy-vps.ps1; verificacion por HTML contra la VPS.
