# Implementacion Claude - REQ-0014

## Manifiesto Minimo Para Codex
Misma tabla `activo` con tipo_codigo de propiedad (DEPARTAMENTO/CASA/TERRENO...): el ABM de activos cubre propiedades y sus contenedores (edificio). Precios/comisiones de venta y alquiler, datos catastrales, atributos por tipo. El estado LIBRE/OCUPADA/VENDIDA lo mueven las operaciones (REQ-0016+).

**Archivos clave:** Comparte codigo con REQ-0013 (activos.xhtml/ActivoService); ver ese REQ.

**Comandos probados:** mvn -q clean package (multi-modulo, EXIT 0); tools/deploy-vps.ps1; verificacion por HTML contra la VPS.
