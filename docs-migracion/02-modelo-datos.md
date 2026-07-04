# 02 — Modelo de datos (31 tablas + ~23 vistas)

Fuente: `Modelo\Model1.edmx` (EF5 database-first sobre Firebird) y clases de entidad generadas.
Convenciones globales: PK `bigint` identity (salvo USUARIOS y RUC), nombres en MAYÚSCULAS, auditoría en todas las tablas (`USUARIO_CREACION`/`FECHA_CREACION` NOT NULL, `USUARIO_MODIFICACION`/`FECHA_MODIFICACION` nullable), booleanos como `varchar` "SI"/"NO", montos `numeric(15,0)` (guaraníes, sin decimales), porcentajes `numeric(5,2)`.

## 1. Entidades principales

### ENTIDADES_INMOBILIARIAS (edificios / loteamientos)
| Campo | Tipo | Null | Notas |
|---|---|---|---|
| ENTIDAD_INMOBILIARIA_ID | bigint | NO | PK identity |
| NOMBRE | varchar(180) | NO | único por TIPO (validado en form) |
| TIPO | varchar(20) | NO | EDIFICIO / LOTEAMIENTO / NINGUNA |
| EMPRESA_ID, PAIS_ID, DEPARTAMENTO_ID, CIUDAD_ID, BARRIO_ID | bigint | SÍ | FKs |
| CUENTA_CATASTRAL varchar(120), NUMERO_FINCA varchar(20), NATURALEZA varchar(20) (URBANA/RURAL) | | | catastro |
| CANTIDAD_UNIDADES int, ANO_CONSTRUCCION int, M2_CONSTRUCCION int, MEDIDAS_LOTE varchar(120) | | SÍ | físico |
| CARACTERISTICA varchar(5000), DIRECCION varchar(250), OBSERVACION varchar(250) | | SÍ | |
| ESTADO | varchar(10) | NO | VIGENTE / INACTIVO |

Relaciones: 1:N → PROPIEDADES; N:M → SOCIOS_NEGOCIOS vía PROPIETARIOS_ENT_INMOB; 1:N → IMAGENES_ENTIDAD_INMOBILIARIA.

### PROPIEDADES (unidades: deptos, casas, lotes)
| Campo | Tipo | Null | Notas |
|---|---|---|---|
| PROPIEDAD_ID | bigint | NO | PK |
| NOMBRE varchar(180) | | SÍ | ej. "Depto 101", "Lote 5" |
| TIPO | varchar(20) | NO | dominio TIPOS_PROPIEDADES (CASA, DEPARTAMENTO, DUPLEX, LOTE…) |
| ENTIDAD_INMOBILIARIA_ID, EMPRESA_ID | bigint | SÍ | FK (puede no pertenecer a entidad) |
| PAIS_ID, DEPARTAMENTO_ID, CIUDAD_ID, BARRIO_ID | bigint | NO | FK |
| DIRECCION varchar(180) NO, CARACTERISTICAS varchar(5000), DIMENSIONES varchar(5000) | | | |
| TIENE_COCHERA SI/NO, CANTIDAD_COCHERA int, M2_CONSTRUCCION, ANO_CONSTRUCCION, MEDIDA_LOTE | | SÍ | |
| PRECIO_VENTA, PRECIO_ALQUILER, PRECIO | numeric(15,0) | NO | PRECIO = genérico |
| COMISION_VENTA, COMISION_ALQUILER, COMISION | numeric(5,2) | NO | % |
| ALQUILADA, VENDIDA | varchar(20) | NO | SI/NO |
| ESTADO | varchar(10) | NO | LIBRE / OCUPADA / VENDIDA (mostrado como LIBRE/OCUPADO) |
| NIS, NUMERO_MEDIDOR (ANDE), NUMERO_MEDIDOR_ESSAP, CTA_CTE_ESSAP | varchar | SÍ | servicios PY |
| NUMERO_LOTE, NUMERO_MANZANA, NUMERO_FINCA, RESOLUCION, SENADO | varchar(20) | SÍ | catastro/lotes |

### OPERACIONES_PROPIEDADES (contratos de alquiler/venta)
| Campo | Tipo | Null | Notas |
|---|---|---|---|
| OPERACION_PROPIEDAD_ID | bigint | NO | PK |
| FECHA_OPERACION | timestamp | NO | |
| TIPO_OPERACION | varchar(20) | NO | **VENTA / ALQUILER** |
| SOCIO_NEGOCIO_ID | bigint | NO | FK — inquilino/comprador |
| VENDEDOR_ID | bigint | SÍ | FK SOCIOS_NEGOCIOS |
| PROPIEDAD_ID / ENTIDAD_INMOBILIARIA_ID | bigint | SÍ | uno u otro según TIPO_ENTIDAD |
| TIPO_ENTIDAD | varchar(20) | NO | ENTIDAD_INMOBILIARIA / PROPIEDAD |
| FECHA_INICIO_CONTRATO NO, FECHA_FIN_CONTRATO SÍ, FECHA_FINALIZACION SÍ, FECHA_RENOVACION SÍ | timestamp | | fin = inicio + PLAZO meses |
| PLAZO | int | SÍ | meses / cantidad de cuotas |
| PRECIO | numeric(15,0) | NO | mensual (alquiler) o total (venta) |
| MONTO_TOTAL_OPERACION numeric(15,0), MONTO_TOTAL_LETRAS varchar(5000) | | | total y en letras |
| GARANTIA | numeric(15,0) | NO | depósito (solo alquiler) |
| ESTADO | varchar(10) | NO | **VIGENTE / FINALIZADO** (+ rescindido vía RESCISIONES) |
| SUCURSAL_ID, MONEDA_ID | bigint | NO | FK |
| TIPO_CONTRATO, CONDICION_OPERACION | varchar(20) | NO | dominios; CONDICION: CONTADO/CREDITO |
| TIPO_FINANCIACION | varchar(120) | SÍ | default FINANCIACION_PROPIA |
| AVISO_RENOVACION SI/NO, DIA_PAGO bigint, MONTO_MORA numeric(15,0), DIAS_GRACIA bigint | | | parámetros de mora/renovación |

Relaciones: 1:N → CRONOGRAMAS_CUOTAS, LIQUIDACIONES, RESCISIONES, IMAGENES_OPERACIONES.

### CRONOGRAMAS_CUOTAS
| Campo | Tipo | Null | Notas |
|---|---|---|---|
| CRONOGRAMA_CUOTA_ID | bigint | NO | PK |
| NUMERO_CUOTA | int | SÍ | 1..PLAZO |
| FECHA_VENCIMIENTO | timestamp | NO | |
| MONTO | numeric(15,0) | NO | |
| OPERACION_PROPIEDAD_ID | bigint | NO | FK |
| ESTADO | varchar(20) | NO | **PENDIENTE / CANCELADO** (cancelado = pagada) |
| MONEDA_ID | bigint | NO | FK (en la práctica siempre 1) |
| FECHA_CANCELACION | timestamp | SÍ | fecha real de pago |

### COBROS / COBROS_DETALLES
**COBROS**: COBRO_ID PK, FECHA, SOCIO_NEGOCIO_ID FK, ESTADO (**CANCELADO / ANULADO**), TOTAL, NUMERO_FACTURA, FECHA_FACTURA, CONDICION (CONTADO), NUMERO_DOCUMENTO_COBRO, OBSERVACION.

**COBROS_DETALLES**: COBRO_DETALLE_ID PK, COBRO_ID FK, NUMERO_ITEM, TIPO_DOCUMENTO (**CUOTA / MORA / DESCUENTO**), MONTO_DOCUMENTO, MONTO_PAGADO, MONEDA_DOCUMENTO FK, MONEDA_PAGO FK, TASA_CAMBIO numeric(15,2), FORMA_PAGO_ID FK, DOCUMENTO_ID (→ CRONOGRAMA_CUOTA_ID de la cuota pagada), OBSERVACION.

### LIQUIDACIONES / LIQUIDACIONES_DETALLES (/ _ITEMS)
**LIQUIDACIONES**: LIQUIDACION_ID PK, FECHA, FECHA_FISCALIZACION, NUMERO, OPERACION_PROPIEDAD_ID FK, ENTREGO_LLAVES SI/NO, TOTAL_GARANTIA, MOTIVO_LIQUIDACION (dominio), TOTAL_GASTOS, **SALDO = GARANTIA − GASTOS**, OBSERVACION.

**LIQUIDACIONES_DETALLES**: LIQUIDACION_DETALLE_ID PK, LIQUIDACION_ID FK, NUMERO_ITEM, ITEM_INGRESO_EGRESO_ID FK, MONTO. (`LIQUIDACIONES_DETALLES_ITEMS` es la variante con descripción del ítem usada por la UI.)

### RESCISIONES
RESCISION_ID PK, FECHA, TIPO varchar(20), MONTO (penalidad), OPERACION_ID FK, OBSERVACION varchar(5000).

### SOCIOS_NEGOCIOS (personas/empresas: clientes, propietarios, inquilinos…)
| Campo | Notas |
|---|---|
| SOCIO_NEGOCIO_ID | PK |
| RAZON_SOCIAL varchar(180) NO, NUMERO_DOCUMENTO varchar(20) NO (único, validado en form), DIGITO_VERIFICADOR varchar(1) (módulo 11) | identidad |
| TIPO_DOCUMENTO (CI/RUC/PASAPORTE, dominio), TIPO_PERSONERIA (FISICA/JURIDICA), CLASIFICACION_FISCAL (GRAVADA…), ES_CONTRIBUYENTE SI/NO, SEXO | fiscal |
| DIRECCION, TELEFONO, EMAIL, UBICACION (URL mapa), OBSERVACION varchar(5000) | contacto |
| Flags de rol (todos SI/NO): ES_PROPIETARIO, ES_INQUILINO, ES_PORTERO, ES_ADMINISTRADOR, ES_CLIENTE, ES_PROVEEDOR, ES_EMPRESA, ES_VENDEDOR | un socio puede tener varios roles |
| ACTIVO SI/NO | |

### INGRESOS_EGRESOS / ITEMS_INGRESOS_EGRESOS
**INGRESOS_EGRESOS**: INGRESO_EGRESO_ID PK, FECHA, TIPO (**INGRESO/EGRESO**), MONTO, SALDO, SOCIO_NEGOCIO_ID FK, ENTIDAD_INMOBILIARIA_ID FK, PROPIEDAD_ID FK, ITEM_INGRESO_EGRESO_ID FK NO, ESTADO (PENDIENTE/CANCELADO/ANULADO), TIPO_IMPUTACION varchar(120) (ej. PROPIEDADES), FORMA_PAGO_ID FK, NUMERO_FACTURA, FECHA_FACTURA, NUMERO_DOCUMENTO_PAGO, OBSERVACION varchar(5000).

**ITEMS_INGRESOS_EGRESOS** (catálogo de conceptos): ITEM_INGRESO_EGRESO_ID PK, ITEM varchar(180), TIPO_ITEM, TIPO (INGRESO/EGRESO), MODIFICA_ESTADO SI/NO, APLICACION varchar(20) — clave funcional: COMISION_VENTA, COMISION_ALQUILER, DEPOSITO_GARANTIA, DESCUENTO, ANDE, MATERIALES, MANO_OBRA, CERRAJERIA, LIMPIEZA, ADMINISTRATIVOS, ALQUILERES_PENDIENTES, MORA.

### PROPIETARIOS_ENT_INMOB (N:M propietarios)
PROPIETARIO_ENT_INMOB_ID PK, ENTIDAD_INMOBILIARIA_ID FK (nullable), PROPIEDAD_ID FK (nullable), PROPIETARIO_ID FK NO. ⚠️ Requiere validar que al menos uno de los dos primeros no sea null.

### IMAGENES_ENTIDAD_INMOBILIARIA / IMAGENES_OPERACIONES
Blob (`FOTO`/`IMAGEN`) + `RUTA_IMAGEN` varchar(5000); FK a entidad/propiedad u operación. En web: migrar a storage de archivos/S3 y guardar URL.

## 2. Tablas paramétricas (resumen)

| Tabla | Campos clave |
|---|---|
| EMPRESAS | EMPRESA_ID PK, SOCIO_NEGOCIO_ID FK (se autogenera un socio ES_EMPRESA), RAZON_SOCIAL, NOMBRE_CORTO, NUMERO_DOCUMENTO+DV, DIRECCION, TELEFONO, ACTIVO |
| SUCURSALES | SUCURSAL_ID PK, SUCURSAL, EMPRESA_ID FK, DIRECCION, TELEFONO, POR_DEFECTO SI/NO (se crea una por defecto al crear empresa) |
| USUARIOS | **CODIGO_USUARIO varchar(20) PK (natural)**, PASSWORD (Base64 reversible ⚠️), ACTIVO SI/NO, EMPRESA_ID FK, PERFIL (ADMINISTRADOR/USUARIO) |
| MONEDAS | MONEDA_ID PK, MONEDA, SIMBOLO, TIPO_MONEDA (LOCAL/…), PRECISION_DECIMALES |
| IMPUESTOS | IMPUESTO_ID PK, IMPUESTO, PORCENTAJE_IMPUESTO, FACTOR_DISCRIMINADO, FACTOR_IMPUESTO |
| FORMAS_PAGOS | FORMA_PAGO_ID PK, FORMA_PAGO, CODIGO (ej. "DB", "EFE"), DIAS, POR_DEFECTO, REQUERE_BANCO |
| PAISES / DEPARTAMENTOS / CIUDADES / BARRIOS | catálogos simples (nombre + auditoría); PAISES con ISO2/ISO3/NACIONALIDAD |
| DEPARTAMENTOS_PAISES / CIUDADES_DEPARTAMENTOS / BARRIOS_CIUDADES | tablas puente jerárquicas con CODIGO (valores mágicos "123456"/"9999"/"9426"). ⚠️ CIUDADES_DEPARTAMENTOS tiene typo de columna `FECHA_MODIFCACION` |
| DOMINIOS | DOMINIO_ID PK, DOMINIO (grupo), CODIGO_DOMINIO, DESCRIPCION, VALOR_DOMINIO — **enumeraciones y parámetros del sistema en BD** |
| VARIABLES_ENTORNOS | por CODIGO_USUARIO: CLAVE→VALOR (empresa_id, empresa, sucursal_id, sucursal) — contexto multi-tenant de la sesión |
| RUC | tabla de consulta externa: RUC1 PK, RAZON_SOCIAL, DV, RUC_V |
| IBE_VERSION_HISTORY | historial de cambios de esquema (herramienta IBExpert), no funcional |

## 3. Dominios (grupos en tabla DOMINIOS usados por el código)

`TIPOS_OPERACIONES` (VENTA, ALQUILER) · `TIPOS_CONTRATOS` · `CONDICION_OPERACION` (CONTADO, CREDITO) · `TIPOS_FINANCIACIONES` · `TIPOS_PROPIEDADES` (…, LOTE) · `TIPOS_ENTIDADES_INMOBILIARIAS` (EDIFICIO, LOTEAMIENTO) · `MOTIVO_LIQUIDACION` · `TIPOS_PERFILES` · `TIPOS_DOCUMENTOS_IDENTIDAD` · `TIPOS_PERSONERIAS` · **parámetros:** `PORCENTAJE_COMISION_ALQUILER`, `PORCENTAJE_COMISION_VENTA`, `MONTO_MORA_DEFECTO`, `DIAS_GRACIA_VENCIMIENTO`, `SUELDO_MINIMO`, `JORNAL_MINIMO`.

> Nota: los valores exactos de cada dominio viven en la BD (tabla DOMINIOS); antes de migrar, exportar su contenido real.

## 4. Grafo de dependencias (simplificado)

```
EMPRESAS ─┬─ SUCURSALES ──────────────┐
          ├─ USUARIOS ── VARIABLES_ENTORNOS
          ├─ ENTIDADES_INMOBILIARIAS ─┬─ PROPIEDADES ─┐
          │        │ (N:M PROPIETARIOS_ENT_INMOB)     │
SOCIOS_NEGOCIOS ───┴──────────────────┴───────────────┤
   │  (inquilino / comprador / vendedor / propietario)│
   │                                                  ▼
   │                              OPERACIONES_PROPIEDADES ─┬─ CRONOGRAMAS_CUOTAS ─┐
   │                                    │                  ├─ LIQUIDACIONES ── DETALLES ── ITEMS_INGRESOS_EGRESOS
   │                                    │                  ├─ RESCISIONES         │
   │                                    │                  └─ IMAGENES_OPERACIONES│
   └─ COBROS ── COBROS_DETALLES ──(DOCUMENTO_ID)───────────────────────────────────┘
   └─ INGRESOS_EGRESOS ── ITEMS_INGRESOS_EGRESOS
Geografía: PAISES → DEPARTAMENTOS → CIUDADES → BARRIOS (vía tablas puente)
Referencias transversales: MONEDAS, FORMAS_PAGOS, IMPUESTOS, DOMINIOS
```

## 5. Vistas (~23)

Patrón general: cada `*_VIEW` desnormaliza su tabla base agregando descripciones de las FKs (nombres de socio, propiedad, entidad, sucursal, moneda) para grillas y reportes. Vistas especiales con lógica:
- **OPERACIONES_CONTRATOS_VENCIDOS** — contratos con fecha de fin vencida (alimenta renovaciones).
- **OPERACIONES_PENDIENTES_VIEW** — operaciones con cuotas pendientes (alimenta cobros).

En la migración conviene reemplazarlas por consultas/joins del ORM o vistas SQL recreadas en el nuevo motor.
