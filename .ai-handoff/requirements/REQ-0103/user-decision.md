# REQ-0103 - Decision Del Usuario

**Fecha:** 2026-07-16

## Decision

**APROBADO** (carga inicial destructiva sobre tenant 1, autorizada por el usuario en el chat).

Autorizaciones explicitas del usuario (transcripcion de la sesion):
- Empresa/tenant destino: **Pysistemas = tenant 1** ("al pysistemas").
- Borrado de datos previos del tenant 1: **autorizado** — "los datos que estan en la web, son todos pruebas" y
  "puedes borrar los datos que creas necesarios".
- Backup previo del PostgreSQL destino: **autorizado y tomado** — respuesta "OK" a "Backup del PostgreSQL
  destino antes del --apply (recomendado)". Backup con `pg_dump --enable-row-security` + `PGOPTIONS='-c app.tenant=-1'`.
- Alcance: continuar la migracion completa — "si, continua hasta finalizar".

## Naturaleza destructiva y control (obs 320)

Es una **carga inicial de una sola vez**, destructiva por diseno: antes de cargar se borra la data de
prueba del tenant 1 (transaccional y financiera) en orden de FK. NO es idempotente por upsert; la
re-ejecucion segura se logra volviendo a correr el borrado + carga (misma clave natural en personas por
numero_documento y en articulos por codigo MIG-ITEM-N). Controles: backup previo tomado; rollback = restore
del backup. No se corre en un tenant con datos productivos reales (el tenant 1 solo tenia pruebas).

## Resultado De Prueba Manual

El usuario ingreso al sistema como `admin` (Pysistemas) y verifico el dashboard de Inicio y el gerencial.
Se corrigieron los hallazgos derivados de esa verificacion: ocupacion (activos OCUPADA/VENDIDA),
capa financiera coherente (documento interno + cobros por el motor) e importes exactos del legado (obs 321).

## Observaciones

Montos: se migran los importes/fechas/estados EXACTOS del legado (CRONOGRAMAS_CUOTAS), sin regenerar con
f_generar_cronograma; suma total de cuotas = Gs. 1.224.081.000, identica al legado (obs 321 resuelta).
