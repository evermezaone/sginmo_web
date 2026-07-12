# REQ-0053 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V34 en `BEGIN...ROLLBACK` | tabla + RLS + pantalla + insert de prueba | OK (adjunto=1, pantalla=1) |
| T03 | Backup previo (esquema nuevo) | dump OK | OK (sginmo-backup.sh) |
| T04 | Deploy + Flyway V34 | success=t | OK |
| T05 | `python tools/smoke-test-vps.py` | 21/21 render OK incl. documentos | OK (TODAS OK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Adjuntar PDF a una persona | queda en la lista; archivo en ~/sginmo/archivos/<tenant>/ | pendiente (verificacion del usuario) |
| M02 | Descargar adjunto | descarga con nombre original; solo del propio tenant | pendiente (verificacion del usuario) |
| M03 | Extension no permitida / >10 MB | rechazo con mensaje | pendiente (verificacion del usuario) |
| M04 | Baja logica | estado INACTIVO; archivo conservado | pendiente (verificacion del usuario) |

## Revision Transversal

- Tabla nueva de negocio con tenant: RLS inline (patron V29 documento_generado). Mismo patron que
  agenda_evento (V33).
- Regla "nunca descargar adjuntos de otra empresa": `leer()` usa `em.find` bajo RLS (solo tenant actual)
  + `exigir("documentos","VER")`. El path fisico usa `d.getTenant()` (del registro ya filtrado por RLS).
- Regla "no guardar en target/deployments": ruta base fuera del WAR (`SGINMO_ARCHIVOS_DIR`), incluida en
  el backup de REQ-0065 (tar de ~/sginmo/archivos).
- Path traversal: `nombre_fisico` es UUID+extension whitelisted generado por el servidor, no input del usuario.

## Datos De Prueba

Empresa real (tenant). Un archivo PDF/imagen de prueba <10 MB.
