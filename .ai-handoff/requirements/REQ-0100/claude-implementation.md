# REQ-0100 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0100
- Tipo de cambio: UI + lectura
- Riesgo: bajo (reusa mias(); sin BD)
- Archivos clave:
  - `web/PortalBean.java`: inject PortalTransferenciaService; transferenciasEnProceso = mias(persona) filtrado a estado != APLICADO; getter.
  - `webapp/portal/inicio.xhtml`: panel "Transferencias en proceso" en el aside (arriba de Mis pagos).
  - `webapp/portal/transferencia.xhtml`: boton "Volver a mi cuenta".
  - `webapp/WEB-INF/portal.xhtml`: badge b-proceso.
- Comandos probados:
  - `python xml.dom.minidom.parse` inicio/transferencia: OK.
  - `powershell -File tools/deploy-vps.ps1`: Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK ===.
- Cambios de datos: no | Cambios de entorno: no
- Decision esperada: aprobar
- Notas para auditor: las transferencias APLICADO se excluyen del panel (ya son cobro y salen en Mis pagos
  via PortalService.pagos, REQ-0091). El panel muestra RECIBIDO/EN_REVISION/OBSERVADO/RECHAZADO con estado amigable.

## Resumen Tecnico

PortalBean carga transferenciasEnProceso desde mias() excluyendo APLICADO. inicio.xhtml agrega un panel en
el aside (rendered si no vacio) con monto+badge estado+fecha. transferencia.xhtml suma un boton "Volver a mi
cuenta" al tope. Badge b-proceso (ambar) en el template.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `web/PortalBean.java` | transferenciasEnProceso (mias filtrado) + getter + inject |
| `webapp/portal/inicio.xhtml` | panel Transferencias en proceso |
| `webapp/portal/transferencia.xhtml` | boton Volver a mi cuenta |
| `webapp/WEB-INF/portal.xhtml` | badge b-proceso |

## Cambios De Datos
Sin cambios.
## Variables De Entorno
Sin cambios.
## Pruebas Ejecutadas
- XHTML bien formados; Build OK; deploy Redeploy OK; smoke 37/37.
## Pruebas Manuales Sugeridas
1. Informar una transferencia -> aparece en "Transferencias en proceso" del inicio (Pendiente de validacion).
2. Aplicarla (operador/auto) -> desaparece de "en proceso" y figura en "Mis pagos".
3. En transferencia.xhtml, el boton "Volver a mi cuenta" lleva al inicio.
## Riesgos Conocidos
La vista del portal no entra en el smoke (OTP).
