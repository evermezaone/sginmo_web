# REQ-0039 - Multiempresa F7: verificacion integral con 2 empresas

**Estado:** verificado a nivel BD/RLS (bateria EXIT 0, rollback); login HTTP diferido al deploy (2026-07-09)

## Objetivo Funcional
Probar, extremo a extremo, que la transformacion multiempresa (F1-F6) aisla de verdad:
con 2 empresas, cada una ve/opera SOLO su tenant; los cruces por id se niegan en el service
Y en RLS; el SUPERADMIN ve todo y puede "operar como" una empresa (soporte).

## Evidencia obtenida
- **Bateria RLS con 2 empresas** (`tools/multiempresa/f7_integracion_test.sql`), corrida
  DESPUES de V26+V27+V28 dentro de BEGIN...ROLLBACK contra la BD REAL, conectado como el rol
  de la app `sginmo` (NO superuser, NO bypassrls; V28 usa FORCE RLS -> aplica al owner):
  - A1 SUPERADMIN (app.tenant=-1) ve las 2 sucursales.
  - A2 Empresa A ve su sucursal + catalogo global (-1), NO lo de B.
  - A3 Empresa B analogamente aislada de A.
  - A4 INSERT cross-tenant NEGADO por RLS (WITH CHECK).
  - A5 UPDATE cross-tenant = 0 filas (la fila ajena es invisible).
  - A6 "operar como" (SUPERADMIN fija app.tenant=A) acota igual que un admin de A.
  Resultado: **EXIT 0**, ROLLBACK -> la BD viva sigue intacta (flyway V25).
- **Aislamiento en la capa service** (usuario/grupo estan FUERA de RLS): validado por las
  guardas de pertenencia F4/F6 (actorTenant / tenant del contexto) + build del WAR verde.

## Bloqueo Formal Documentado
El "login real via HTTP de cada usuario" requiere la app desplegada con V26+V27+V28, que es
el DEPLOY de la unidad F1-F6 (lo hace Codex/ops; V26 NO se aplica a la BD viva sola porque
rompe la app en V25). Por eso F7 prueba el aislamiento en la capa donde RLS es la ultima
defensa (BD, con el rol real de la app) + analisis de la capa service; la corrida HTTP con
2 usuarios se hace en el pos-deploy, sin dejar datos fuera de las 2 empresas de demo.
