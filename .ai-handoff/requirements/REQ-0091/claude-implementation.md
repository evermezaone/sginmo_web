# REQ-0091 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0091
- Tipo de cambio: backend + UI
- Riesgo: bajo (lectura adicional en portal; sin cambios de esquema)
- Archivos clave:
  - `servicio/PortalService.java`: pagos() ahora hace LEFT JOIN a forma_pago y expone canal (Caja/Transferencia) + forma + concepto en FilaPago.
  - `webapp/portal/inicio.xhtml`: layout de 2 columnas (portal-cols): contenido principal (cuotas/documentos) + panel lateral derecho "Mis pagos" con badges de canal.
  - `webapp/WEB-INF/portal.xhtml`: CSS del layout (.portal-cols/.portal-main/.portal-aside), items de pago y badges; stacking responsive (<=820px).
- Comandos probados:
  - `python -c xml.dom.minidom.parse(inicio.xhtml)`: XML bien formado OK.
  - `powershell -File tools/deploy-vps.ps1`: Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK === (37 pantallas admin 200).
- Cambios de datos: no
- Cambios de entorno: no
- Impacto LLM/tokens: no
- Decision esperada: aprobar
- Notas para auditor: el canal se deriva de forma_pago.codigo (TRF=Transferencia, resto/NULL=Caja). La
  vista del portal no se cubre con el smoke (requiere OTP); se verifico XML bien formado + getters.
  Aislamiento por persona + tenant intacto (cobro.persona=:p, @AislarTenant/RLS).

## Resumen Funcional

En su cuenta, el socio ve a la derecha un panel "Mis pagos" con el historial, cada pago con un badge de
canal (Caja / Transferencia), fecha y forma de pago. En celular el panel baja debajo del contenido.

## Resumen Tecnico

PortalService.pagos() hace LEFT JOIN cobro->forma_pago y arma canal ("TRF"->Transferencia, else Caja) +
forma (descripcion) + concepto. inicio.xhtml se reorganizo en .portal-cols (main + aside). CSS del panel y
badges en el template portal.xhtml, con stacking responsive.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `servicio/PortalService.java` | pagos() con canal/forma/concepto; FilaPago con getters nuevos |
| `webapp/portal/inicio.xhtml` | layout 2 columnas + panel lateral de pagos con badges |
| `webapp/WEB-INF/portal.xhtml` | CSS del layout, items de pago, badges, responsive |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

- inicio.xhtml XML bien formado (parse OK).
- Build OK (mvn clean package, EXIT 0); deploy Redeploy OK; login.xhtml 200.
- smoke-test-vps.py: 37 pantallas 200 (TODAS OK).

## Pruebas Manuales Sugeridas

1. Entrar al portal del socio -> a la derecha "Mis pagos" con badges Caja/Transferencia; en celular baja abajo.

## Riesgos Conocidos

La vista del portal no entra en el smoke automatico (OTP); verificacion manual pendiente de confirmacion del usuario.
