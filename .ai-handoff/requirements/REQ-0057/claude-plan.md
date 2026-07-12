# REQ-0057 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Cartera vencida = consulta sobre cronograma_cuota (misma fuente que cobros) + f_mora_cuota (no duplica
calculo). Tablas nuevas gestion_cobranza y promesa_pago (por-tenant, RLS). Integracion con agenda para
promesas vencidas. Sin modificar cuotas.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V38__mora_cobranza.sql | 2 tablas + RLS + pantalla |
| dominio/cobranza/GestionCobranza.java, PromesaPago.java | NUEVAS |
| servicio/MoraService.java | NUEVO — cartera + gestiones + promesas |
| servicio/AgendaService.java | promesas vencidas -> agenda |
| web/MoraBean.java + webapp/cobranza.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |

## Pruebas Previstas

- [ ] Build OK
- [ ] V38 rollback (2 tablas + RLS + pantalla + insert)
- [ ] Deploy + Flyway + smoke
- [ ] Mora via f_mora_cuota; promesa no toca cuota

## Riesgos

- Dominio de mora: mitigado reutilizando f_mora_cuota y sin modificar cuotas.

## Cambios De Datos

V38: gestion_cobranza + promesa_pago + pantalla cobranza.
