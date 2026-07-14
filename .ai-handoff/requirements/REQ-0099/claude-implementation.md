# REQ-0099 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0099
- Tipo de cambio: UI
- Riesgo: bajo (solo dos XHTML; sin backend ni BD)
- Archivos clave:
  - `webapp/portal/inicio.xhtml`: boton "Informar transferencia" prominente en la tarjeta Mis cuotas (esCliente).
  - `webapp/portal/transferencia.xhtml`: formulario reducido a Monto + Comprobante (resto lo infiere el OCR).
- Comandos probados:
  - `python xml.dom.minidom.parse` inicio.xhtml/transferencia.xhtml: OK.
  - `powershell -File tools/deploy-vps.ps1`: Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK ===.
- Cambios de datos: no | Cambios de entorno: no
- Decision esperada: aprobar
- Notas para auditor: informar() del servicio ya toleraba nulls en los campos opcionales y corre OCR
  best-effort (REQ-0084); al quitar los inputs quedan null y el OCR los completa. El importe sigue siendo obligatorio.

## Resumen Tecnico

inicio.xhtml: la fila de "Periodo" pasa a flex y suma a la derecha un h:link estilo boton hacia
portal/transferencia. transferencia.xhtml: el form deja solo el inputNumber de monto y el fileUpload del
comprobante; se quitaron fecha/banco/cuenta/numero/observacion (el bean deja esos campos en null).

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `webapp/portal/inicio.xhtml` | boton "Informar transferencia" en Mis cuotas |
| `webapp/portal/transferencia.xhtml` | formulario reducido a monto + comprobante |

## Cambios De Datos
Sin cambios.
## Variables De Entorno
Sin cambios.
## Pruebas Ejecutadas
- XHTML bien formados; Build OK; deploy Redeploy OK; smoke 37/37.
## Pruebas Manuales Sugeridas
1. En el portal, tarjeta Mis cuotas -> boton "Informar transferencia" visible; lleva al formulario.
2. Informar con solo monto + comprobante -> queda RECIBIDO; el OCR completa fecha/nro/banco si puede.
## Riesgos Conocidos
La vista del portal no entra en el smoke (OTP).
