# 07 — Análisis de la base de datos real (Fase 0)

**Fuente:** copia de `INMOBILIARIA.FDB` (última modificación 3/7/2026, 15,8 MB), abierta con Firebird 2.5.9 embebido sobre una copia de trabajo. **Motor confirmado: Firebird 2.5 (ODS 11.2), página 16 KB, charset UTF8.**

## 1. Dimensión real de los datos

| Tabla | Filas | Observación |
|---|---|---|
| SOCIOS_NEGOCIOS | 51 | sin documentos duplicados |
| ENTIDADES_INMOBILIARIAS | 4 | |
| PROPIEDADES | 58 | 40 OCUPADA, 18 LIBRE |
| OPERACIONES_PROPIEDADES | 40 | **todas ALQUILER + VIGENTE** |
| CRONOGRAMAS_CUOTAS | 403 | 174 CANCELADO (Gs. 278,7 M) / 229 PENDIENTE (Gs. 356,8 M) |
| COBROS / COBROS_DETALLES | 51 / 52 | todos CANCELADO; sin detalles huérfanos |
| LIQUIDACIONES, RESCISIONES, INGRESOS_EGRESOS, ITEMS_INGRESOS_EGRESOS, IMAGENES_* , RUC | **0** | módulos sin uso en producción |
| EMPRESAS | 2 | IIR SA / 2R S.A. (ambas activas) |
| SUCURSALES | 1 | |
| USUARIOS | 12 | **10 ADMINISTRADOR activos**, 2 USUARIO (1 inactivo) |
| MONEDAS | 4 | Gs. (LOCAL), USD, RS, EUR |
| CRONOGRAMAS_CUOTAS_PENDIENTE | 123 | tabla **fuera del EDMX**: copia manual de cuotas (mismas columnas), vencimientos 6/2025–4/2026, 31 operaciones — parece respaldo previo a una corrección de datos. Confirmar y excluir del ETL |

**Implicación de alcance:** en producción solo se ejercita el flujo **alquiler + cronograma + cobro con mora**. Venta, liquidaciones, rescisiones, ingresos/egresos e imágenes existen en el código pero nunca se usaron (ITEMS_INGRESOS_EGRESOS vacío implica además que los movimientos automáticos de comisión/garantía —RN-OPE-012/013 del doc 03— nunca corrieron). Esto permite priorizar la migración: el núcleo real es pequeño y está sano.

## 2. Parámetros de negocio vigentes (tabla DOMINIOS, valores reales)

| Parámetro | Valor |
|---|---|
| MONTO_MORA_DEFECTO | **Gs. 50.000 por día** |
| DIAS_GRACIA_VENCIMIENTO | **3 días** |
| PORCENTAJE_COMISION_ALQUILER | **50%** |
| PORCENTAJE_COMISION_VENTA | **5%** |
| SUELDO_MINIMO | Gs. 2.112.562 |
| JORNAL_MINIMO | Gs. 81.252 |

## 3. Enumeraciones reales (para los enums de JPA)

- **TIPOS_OPERACIONES:** ALQUILER, VENTA
- **CONDICION_OPERACION:** CONTADO, CREDITO
- **TIPOS_CONTRATOS:** PRIVADO (contrato privado), PUBLICO (escritura pública)
- **TIPOS_FINANCIACIONES:** FINANCIACION_PROPIA, FINANCIACION_BANCARIA
- **TIPOS_ENTIDADES_INMOBILIARIAS:** EDIFICIO, LOTEAMIENTO, COMPLEJO, BARRIO_CERRADO, SALONES_COMERCIALES, NO_APLICA
- **TIPOS_PROPIEDADES:** CASA, DEPARTAMENTO, DUPLEX, LOTE, OFICINA, PIEZA, SALONES, ESTACIONAMIENTO, AREA_COMUN
- **ESTADO_INGRESO_EGRESO:** PENDIENTE, CANCELADO, ANULADO, **VENCIDO**
- **TIPO_ITEM_INGR_EGR:** INGRESO, EGRESO, DESCUENTO
- **TIPO_IMPUTACION:** ADMINISTRADOR, ENTIDAD_INMOBILIARIA, INQUILINO, PROPIEDAD, PROPIETARIO, VENDEDOR
- **TIPOS_DOCUMENTOS_IDENTIDAD:** CI, RUC, DOCEX, OTROS
- **TIPOS_PERSONERIAS:** PERFIS (física), PERJUR (jurídica)
- **TIPOS_PERFILES:** ADMINISTRADOR, USUARIO
- **TIPOS_GASTOS:** FIJO, VARIABLE
- **TIPOS_MONEDAS:** LOCAL, EXTRANJERA
- **MOTIVO_LIQUIDACION:** un solo valor genérico ("Liquidacion")
- Estados usados en datos: operación VIGENTE; cuota PENDIENTE/CANCELADO; cobro CANCELADO; propiedad LIBRE/OCUPADA.

## 4. Objetos de BD que el EDMX no mostraba

- **31 triggers**: todos `*_BI` (before insert) que asignan la PK desde el generador `GEN_*_ID`. **Sin lógica de negocio en triggers** → en PostgreSQL se reemplazan por `IDENTITY`/secuencias.
- **31 procedimientos almacenados**:
  - **25 `RPT_*`**: las consultas de los reportes Crystal (cronogramas, recaudaciones, estado de cuentas, pagaré, etc.). Son la especificación exacta de cada reporte → traducirlos a consultas/JPQL al rehacerlos con JasperReports. Incluye 2 versiones `_OLD` (descartar).
  - **`GET_MES_LETRAS`**: mes en letras (para pagaré/contratos).
  - **`SP_AUX_COBRO_CUOTA`**: helper de reportes — dado una cuota devuelve nº de factura del cobro ("Pagado" si vacío) y la mora pagada.
  - **`CORRECCION_FECHA_CANCELACION`**: fix de datos (alinea FECHA_CANCELACION de cuotas con la fecha del cobro).
  - **`SP_CORRIGE_ESTADO_PROPIEDADES`**: fix de datos (propiedades LIBRE con operación VIGENTE → OCUPADA).
  - **`SP_BORRA_OPERACIONES_ENTIDADES`**: herramienta destructiva de limpieza (resetea cuotas a PENDIENTE y borra cobros de operaciones vigentes por tipo). **No migrar.**
  - Los dos SP de "corrección" son evidencia de desincronizaciones históricas app↔datos — refuerzan la necesidad de transacciones y de invariantes en la nueva app (el estado de la propiedad debe derivarse de la operación, no setearse aparte).
- Tablas `IBE$LOG_*` e `IBE$VERSION_HISTORY`: artefactos de IBExpert (logging/versionado de la herramienta). No migrar.

## 5. Calidad de datos (verificada)

- Sin cuotas huérfanas; sin detalles de cobro apuntando a cuotas inexistentes; sin operaciones sin propiedad; sin documentos duplicados en socios. **El ETL puede ser directo.**
- ⚠️ 10 de 12 usuarios son ADMINISTRADOR: el esquema de roles está de facto sin usar. En la nueva app conviene rediseñar roles (y las contraseñas actuales, al ser Base64 reversible, se re-hashean con bcrypt en el ETL o se fuerza reset).

## 6. Decisiones/pendientes que surgen de este análisis

1. **Alcance — DECIDIDO (2026-07-03): se migra TODO lo programado** (venta, liquidaciones, rescisiones, ingresos/egresos, imágenes), independientemente de que hoy no tengan datos. El usuario conseguirá una BD con más datos para validar esos flujos.
2. `CRONOGRAMAS_CUOTAS_PENDIENTE`: confirmar que es un respaldo manual y excluirla del ETL (pendiente).
3. Los 23 SPs `RPT_*` vigentes son la especificación de los reportes → extraer sus fuentes completos como anexo al rediseñar cada JasperReport.
4. Multimoneda: hay 4 monedas cargadas pero todos los datos son Gs. — decisión pendiente (doc 06) sigue abierta, con sesgo a operar solo Gs.
