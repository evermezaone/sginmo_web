# Implementacion Claude - REQ-0007

## Manifiesto Minimo Para Codex
- UbicacionGeografica (entidad, padre @ManyToOne EAGER + LEFT JOIN FETCH en listados),
  GeografiaService (whitelist orden, buscar() max 15 para autocomplete, exigir() permisos),
  GeografiaBean, geografia.xhtml, UbicacionConverter (managed).
- Datos: V3 (INE 2022) ya aplicada; V12 agrega pantalla 'geografia' a PANTALLAS.
- Comandos probados: build+deploy; grilla reporta "de 8276" contra la VPS.
