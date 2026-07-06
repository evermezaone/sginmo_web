# Implementacion Claude - REQ-0012

## Manifiesto Minimo Para Codex
ABM unificado de personas fisicas y juridicas (subtabla por @MapsId), documento unico global, roles multiples por persona (CLIENTE/PROVEEDOR/PROPIETARIO/INQUILINO/...). PersonaService.porRol alimenta los combos de otros modulos. Estandar completo + enforcement en servicio. V14 pantalla.

**Archivos clave:** PersonaFisica/Persona/PersonaJuridica/PersonaRol, PersonaService, PersonaBean, personas.xhtml, V14.

**Comandos probados:** mvn -q clean package (multi-modulo, EXIT 0); tools/deploy-vps.ps1; verificacion por HTML contra la VPS.
