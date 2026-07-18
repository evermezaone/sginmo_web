# REQ-0106 - Cubo BI Metabase en la VPS: tableros top-down sobre las vistas sabana

**Numero:** REQ-0106
**Fecha de creacion:** 2026-07-18
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

"crea un cubo, con varias hojas, mostrando los datos en tablas dinamicas y graficos importantes /
cubrir todo lo que el negocio necesita con metodologia Top-down / ese cubo debe ser actualizable
apuntando a la base de datos / el resultado esperado es un libro de excel (propuesto), si tienes otra
opcion libre y sin pagar licencias... mejor si es web".

**Decision del usuario:** Metabase (open source, web, en la VPS). Sin licencias. Lee la BD en vivo,
por lo que no requiere "refrescar" nada.

## Objetivo Funcional

Un cubo web con tableros por nivel de decision, construido sobre las 5 sabanas de REQ-0105, que cubra
lo que el negocio necesita sin duplicar la logica que hoy esta repartida en ~30 SQL manuales.

## Diseno Top-Down (4 niveles)

### Nivel 1 - DIRECCION: "¿como va el negocio?" (1 tablero)
Una sola pantalla, sin detalle, para decidir si hay que preocuparse.
- Recaudacion del mes vs mes anterior vs mismo mes del anio anterior (v_sabana_cobro).
- Cartera total y % de mora (v_sabana_cuota).
- Ocupacion % y unidades vacantes alquilables (v_sabana_activo).
- Resultado neto del periodo: ingresos - egresos (v_sabana_movimiento).
- Evolucion 12 meses: recaudado vs egresos (linea).
- Proyeccion de cobranza proximos 6 meses (barras) - hoy NO existe en el sistema.

### Nivel 2 - GESTION: "¿donde esta el problema?" (4 tableros)
- **Cobranza y mora**: aging 01-30/31-60/61-90/90+ (barras + tabla dinamica), mora por cliente,
  por sucursal, por tipo de activo, por zona. Top deudores.
- **Ocupacion y vacancia**: ocupacion por tipo/zona/edificio, unidades vacantes alquilables con su
  precio (= ingreso potencial perdido), contratos por vencer en 30/60/90 dias.
- **Rentabilidad**: ingresos vs egresos por aplicacion, por activo (ranking mejores/peores), por mes.
- **Recaudacion**: cobros por periodo/forma de pago/sucursal/cajero, ticket promedio, evolucion.

### Nivel 3 - OPERACION: "¿que hago hoy?" (2 tableros)
- **Cobranza del dia**: cuotas vencidas ordenadas por dias e importe, con cliente y contacto;
  promesas y gestiones pendientes.
- **Contratos**: los que vencen en 30 dias (para renovar), renovados vs no renovados, altas del mes.

### Nivel 4 - DETALLE / EVIDENCIA (5 vistas)
Las 5 sabanas crudas, filtrables y exportables a Excel/CSV desde el propio Metabase. Es el
"drill-down" natural: cualquier numero de los niveles 1-3 se abre hasta la fila que lo compone.

## Criterios De Aceptacion

- [ ] Metabase corriendo en la VPS, con arranque automatico y accesible por web.
- [ ] Conexion a PostgreSQL fijando el tenant (`options=-c app.tenant=N`), de modo que la RLS siga aplicando.
- [ ] Los 4 niveles implementados como colecciones/tableros, con tablas dinamicas y graficos.
- [ ] Cada KPI de nivel 1-2 permite drill-down hasta el detalle (nivel 4).
- [ ] Exportacion a Excel/CSV disponible en cada pregunta.
- [ ] Los numeros del cubo coinciden con los del sistema (control cruzado contra las sabanas).

## Estado actual de la implementacion

HECHO:
- Metabase 630 MB descargado en `~/apps/metabase`, corriendo como JAR con el JDK 21 existente.
- Puerto 3001 (el 3000 estaba ocupado por otro servicio). `~/apps/metabase/start-metabase.sh`.
- Arranque automatico via `@reboot` en crontab del usuario `edm`.
- Health OK: `curl localhost:3001/api/health` -> 200.
- BD interna de Metabase: H2 (suficiente para esta escala; migrable a PostgreSQL si crece).

PENDIENTE (requiere accion del usuario, ver Dependencias):
- Abrir el puerto 3001 en el firewall (o publicarlo por HTTPS detras de un proxy).
- Crear la cuenta administradora de Metabase (la crea el usuario; Claude no crea cuentas ni fija passwords).
- Con eso hecho: cargar la conexion a la BD y construir los tableros de los 4 niveles.

## Dependencias

- REQ-0105 (vistas sabana) - HECHO.
- Acceso sudo en la VPS para abrir el puerto (lo hace el usuario).
- Cuenta admin de Metabase creada por el usuario.
- Rol `bi_reader` de solo lectura: pendiente, requiere CREATEROLE (el usuario `sginmo` no lo tiene).
  Mientras tanto la conexion usara el usuario de la app acotado por RLS al tenant.
