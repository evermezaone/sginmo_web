# Implementacion Claude - REQ-0006

## Manifiesto Minimo Para Codex
- Referencia completa: articulos.xhtml + ArticuloBean + ArticuloService (+ ImpuestoConverter,
  ArticuloPropiedad). Contrato repetible: contar/listar con whitelist JPQL, guardar+validar
  (+@Version con mensaje claro + ErroresBd), existeX por clave unica, cambiarEstado logico
  con reactivacion segura, exigir() de permisos en cada escritura (obs 203).
- Estandar en docs-migracion/11-estandar-abm-propuesta.md; pendientes que dependen de
  modulos futuros (historial visible, dependencias visibles, importacion, auditoria de
  exportaciones) mapeados a REQs alli.
- Comandos probados: build+deploy; validacion del usuario en VPS (celular + PC).
