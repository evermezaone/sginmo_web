# Codex Review - REQ-0008

Fecha: 2026-07-06
Auditor: codex
Resultado: APROBADO_POR_CODEX en ronda 2

## Verificacion

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md`, `preaudit-checklist.md` y observaciones registradas en `AUDITORIA_OBSERVACION`.
- Reauditadas observaciones previas:
  - Obs 204: corregida. `ImpuestoService.guardar()` ahora asigna en modo simplificado `factorDiscriminado=(100+p)/p`, `factorImpuesto=(100+p)/100`; exenta queda `0/1.00`, alineado con `V2__seed_basico.sql`.
  - Obs 205: aceptada/corregida por evidencia. El criterio se corrigio de 14 a 13 requisitos y queda trazado a `docs-migracion/09-borrador-esquema.md` lineas 84-96 y `docs-migracion/10-auditoria-gestion-oracle.md` punto 7. La tabla V1, entidad y XHTML exponen esos 13 flags.
  - Obs 206: corregida. `monedas.xhtml`, `impuestos.xhtml` y `formas-pago.xhtml` incluyen selector de columnas, export CSV/XML/PDF `pageOnly` por permiso `EXPORTAR`, boton limpiar busqueda, mensajes vacios diferenciados y acciones de Mi vista.
- Revisados:
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/MonedaService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ImpuestoService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/FormaPagoService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/MonedaBean.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ImpuestoBean.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/FormaPagoBean.java`
  - `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/VistaUsuario.java`
  - `Desarrollo/sginmo-web/src/main/webapp/monedas.xhtml`
  - `Desarrollo/sginmo-web/src/main/webapp/impuestos.xhtml`
  - `Desarrollo/sginmo-web/src/main/webapp/formas-pago.xhtml`
  - `docs-migracion/09-borrador-esquema.md`
  - `docs-migracion/10-auditoria-gestion-oracle.md`

## Resultado

Sin observaciones bloqueantes.

## Pruebas

- Revision estatica de servicios, beans, XHTML, helper `VistaUsuario` y documentacion.
- Build multi-modulo ejecutado con JDK 23:
  - Comando: `mvn -q clean package`
  - Directorio: `Desarrollo`
  - Resultado: EXIT 0

