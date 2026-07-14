# REQ-0097 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0097
- Tipo de cambio: backend + UI
- Riesgo: bajo (solo lectura adicional; reutiliza f_mora_cuota)
- Archivos clave:
  - `servicio/PortalService.java`: cuotas() suma dias_mora y f_mora_cuota (solo PENDIENTE+saldo>0+vencida); FilaCuota.diasMora/moraAcumulada/getIndicador.
  - `webapp/portal/inicio.xhtml`: columnas Estado (badge Atrasado/Al dia/Pagado), Dias mora, Multa/Mora.
  - `webapp/WEB-INF/portal.xhtml`: badges b-atraso/b-aldia/b-pagado.
- Comandos probados:
  - `python xml.dom.minidom.parse inicio.xhtml`: OK.
  - `powershell -File tools/deploy-vps.ps1`: Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK ===.
- Cambios de datos: no  | Cambios de entorno: no
- Decision esperada: aprobar
- Notas para auditor: la mora en dinero NO se recalcula en el portal: usa f_mora_cuota (misma fuente que
  MoraService.carteraVencida). Dias = current_date - vencimiento. Sin casts de fecha inseguros (aLocalDate).

## Resumen Tecnico

cuotas() amplia el SELECT con dos columnas condicionadas (dias_mora y f_mora_cuota) que solo cuentan si la
cuota esta PENDIENTE con saldo>0 y vencida; el resto ve 0. FilaCuota expone diasMora, moraAcumulada y un
indicador (Pagado/Atrasado/Al dia). La grilla del portal muestra badge + dias + multa (— cuando no aplica).

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `servicio/PortalService.java` | cuotas() con dias_mora/f_mora_cuota; FilaCuota diasMora/moraAcumulada/indicador |
| `webapp/portal/inicio.xhtml` | columnas Estado(badge)/Dias mora/Multa-Mora |
| `webapp/WEB-INF/portal.xhtml` | badges de estado de cuota |

## Cambios De Datos
Sin cambios.
## Variables De Entorno
Sin cambios.
## Pruebas Ejecutadas
- inicio.xhtml XML OK; Build OK; deploy Redeploy OK; smoke 37/37.
## Pruebas Manuales Sugeridas
1. Socio con una cuota vencida impaga -> Atrasado, dias>0, multa segun f_mora_cuota; cuota futura -> Al dia/0; cuota pagada -> Pagado.
## Riesgos Conocidos
La vista del portal no entra en el smoke (OTP); verificacion manual pendiente.
