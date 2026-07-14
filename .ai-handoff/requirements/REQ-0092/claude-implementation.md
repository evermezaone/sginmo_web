# REQ-0092 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0092
- Tipo de cambio: backend + UI
- Riesgo: bajo-medio (agrega borrado por el socio; atomico y acotado por estado/persona)
- Archivos clave:
  - `servicio/PortalTransferenciaService.java`: eliminar(id, persona) atomico (DELETE ... WHERE estado='RECIBIDO' AND persona=:p RETURNING) + borra evidencia + auditoria; Fila.getEstadoLabel()/isPuedeEliminar().
  - `web/PortalTransferenciaBean.java`: eliminar(Fila) + descargarComprobante(Fila) (StreamedContent).
  - `webapp/portal/transferencia.xhtml`: tabla "mis transferencias" en form; estado amigable; acciones Descargar (propia) + Eliminar (solo Pendiente de validacion, con confirm).
- Comandos probados:
  - `python xml.dom.minidom.parse` de transferencia.xhtml e inicio.xhtml: OK.
  - `powershell -File tools/deploy-vps.ps1`: Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK ===.
- Cambios de datos: no (usa estados y tabla existentes de REQ-0083)
- Cambios de entorno: no
- Decision esperada: aprobar
- Notas para auditor: el borrado es ATOMICO contra el reclamo del operador: si la fila ya paso a
  EN_REVISION, el DELETE no toca filas (NoResultException -> mensaje). RLS + persona=:p garantizan
  aislamiento. La evidencia fisica se borra best-effort. Estados: RECIBIDO=Pendiente de validacion,
  EN_REVISION=En verificacion (mapeo REQ-0092, sin renombrar codigos en BD).

## Resumen Funcional

El socio ve sus transferencias con estado amigable; mientras estan "Pendiente de validacion" puede
descargar su comprobante y ELIMINAR el registro; cuando pasan a "En verificacion" ya no puede eliminarlas.

## Resumen Tecnico

Nuevo eliminar(id,persona) con DELETE...RETURNING acotado a estado RECIBIDO + persona (atomico vs. el
claim a EN_REVISION del operador) que borra la evidencia y audita. Fila expone estadoLabel y puedeEliminar.
El bean agrega eliminar() y descargarComprobante(). transferencia.xhtml agrega form + columna de acciones.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `servicio/PortalTransferenciaService.java` | eliminar() atomico + Fila.getEstadoLabel/isPuedeEliminar |
| `web/PortalTransferenciaBean.java` | eliminar(Fila) + descargarComprobante(Fila) |
| `webapp/portal/transferencia.xhtml` | tabla en form, estado amigable, acciones descargar/eliminar |

## Cambios De Datos

Sin cambios (reusa portal_pago_transferencia y sus estados de REQ-0083).

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

- transferencia.xhtml e inicio.xhtml XML bien formado.
- Build OK (EXIT 0); deploy Redeploy OK; login 200.
- smoke-test-vps.py: 37 pantallas 200 (TODAS OK).

## Pruebas Manuales Sugeridas

1. Informar una transferencia -> aparece "Pendiente de validacion" con botones Descargar y Eliminar.
2. Eliminarla -> desaparece y el comprobante se borra.
3. Que un operador la tome (bandeja -> En verificacion) -> el socio ya no ve el boton Eliminar; si intenta, falla.

## Riesgos Conocidos

La vista del portal no entra en el smoke (OTP); verificacion manual pendiente.
