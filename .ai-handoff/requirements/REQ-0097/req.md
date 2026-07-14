# REQ-0097 - Portal socio: grilla de cuotas con atraso, dias de mora y multas

**Numero:** REQ-0097
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** media

## Objetivo Funcional

En la grilla "Mis cuotas" del portal, informar por cuota si esta al dia/atrasada/pagada, los dias de mora y
la multa/mora acumulada, reutilizando la MISMA fuente de calculo que el modulo de cobranza (no duplicar formula).

## Alcance

- PortalService.cuotas(): agrega dias de mora = (current_date - vencimiento) y mora en dinero = f_mora_cuota(...)
  SOLO cuando la cuota esta PENDIENTE, con saldo > 0 y vencida. (f_mora_cuota es la misma funcion que usa MoraService.)
- FilaCuota: campos diasMora, moraAcumulada + indicador derivado (Pagado / Atrasado / Al dia).
- inicio.xhtml: columnas Estado (badge), Dias mora, Multa/Mora; pagada o no vencida -> "—"/0 limpio. Responsive (tablas con overflow-x, REQ-0096).
- Aislamiento intacto (cuotas filtradas por o.cliente = persona del socio; @AislarTenant/RLS).
- Sin casts inseguros de fecha (usa aLocalDate, hardening REQ-0090).

## Criterios De Aceptacion

- [x] Cuota vencida con saldo -> "Atrasado", dias de mora > 0 y multa/mora si aplica.
- [x] Cuota no vencida con saldo -> "Al dia" y dias de mora 0.
- [x] Cuota pagada -> sin atraso ni mora activa.
- [x] La multa/mora usa f_mora_cuota (misma que cobranza) -> valores consistentes.
- [x] El portal no expone cuotas de otro cliente (filtro por persona + RLS).
- [x] Build (mvn clean package) OK.

## Dependencias

- Reutiliza f_mora_cuota (MoraService/cobranza). Base: PortalService.cuotas, portal/inicio.xhtml.
