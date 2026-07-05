# 10 — Auditoría del sistema de Gestión (Oracle 11g XE) — patrón documento/cobros

**Fuente:** BD `one@localhost:1521/XE` (Oracle Database 11g XE 11.2.0.2.0), auditada 2026-07-04.
**Propósito:** referencia obligatoria para el módulo de dinero de SGInmo Web (decisión P8 del usuario) y registro de triggers/procedimientos según pedido del usuario. Fuentes completas extraídas con `user_source` (1.210 líneas, en scratchpad de la sesión).

## 1. Modelo de comprobantes (confirmado contra el esquema real)

| Tabla | Clave primaria | Rol |
|---|---|---|
| `DOCUMENTO` | **(EMPRESA, TIPO, SERIE, NUMERO)** | comprobante universal: totales (exentas/gravadas/impuestos/total), **SALDO vivo**, ESTADO ('PE' pendiente / 'CA' cancelado / 'AN' anulado), FECHA_VENCIMIENTO, **TIMBRADO**, monedas cpte/local + COTIZACION, montos _EXT, referencia a otro doc (TIPO_REF/SERIE_REF/NUMERO_REF), cliente/c_identidad, sucursal, usuario |
| `DETALLE_DOCUMENTO` | (EMPRESA, TIPO, SERIE, NUMERO, **NRO_DETALLE**) | líneas: articulo/concepto, cantidad, precio, **exentas/gravadas/%imp/impuestos por línea**, estado, saldo |
| `ARTICULOS` | (EMPRESA, **ARTICULO**) | maestro de productos/servicios: descripcion, tipo/categoria, unidad, presentacion, marca/modelo/familia/grupo/subgrupo/procedencia, impuesto, codigos, stock, proveedor, costos |
| `DETALLE_ARTICULO` | (EMPRESA, ARTICULO, **CODBARRA**) | variantes/codigos de barra del articulo: descripcion, color, talle |
| `ANEXO_ARTICULO` | (EMPRESA, ARTICULO, AMBITO_DATO_ADICIONAL, DATO_ADICIONAL) | propiedades adicionales del articulo: valor, numero, fecha y referencia opcional a otra entidad |
| `ARTICULO_COSTO` | (EMPRESA, ARTICULO, **FECHA**) | historico de costo local y costo externo |
| `TIPO_COMPROBANTE` | (TIPO) | catálogo: **AFECTACION 'I'/'E'** (entrada/salida de dinero), flag **CTA_CTE**, EXCENTO, prioridad, destino |
| `RANGO_COMPROBANTE` | (EMPRESA, SERIE, TIPO, NUMERO_DESDE) | numeración administrada: NUMERO_ACTUAL/HASTA, **FECHA_VENCIMIENTO** del rango, **CODIGO_AUTORZACION (timbrado)**, ESTADO 'A' |
| `COBROS` | (EMPRESA, CODIGO_COBRO, SUCURSAL) | acto de cobro: FPAGO, **PLANILLA (caja)**, cajero, moneda+cotización, monto_ext, vuelto, datos de cheque, RECIBO/FACTURA, ESTADO 'A' activo / 'H' anulado |
| `COBRODETALLE` | (EMPRESA, SUCURSAL, CODIGO_COBRO, SECUENCIA) | aplicación del cobro a un documento: (EMPRESA_CPTE, TIPO, SERIE, NUMERO) + MONTO |
| `PLANILLAS` | (EMPRESA, PLANILLA) | **caja diaria** por usuario/sucursal: apertura/cierre, montos, estado |
| `ANULACIONES` | (NRODETALLE) | log de anulaciones con motivo, usuario, fecha |
| `MOTIVO_DE_ANULACION` | (MOTIVO) | catálogo de motivos con ALCANCE |
| `DATOS_COBROS` | (EMPRESA, SUCURSAL, CODIGO_COBRO) | **anexo del cobro**: datos del medio de pago — emisor/procesador (tarjeta), numero/serie y vencimiento (cheque diferido), cuenta, referencia, cobrador, datos de depósito (fecha/nro/estado/motivo rechazo), NC aplicada (NTCR tipo/serie/numero) |
| `DETALLES_AFECTADOS` | (EMPRESA, SUCURSAL, CODIGO_COBRO, SECUENCIA, ...) | a qué **líneas** del documento se aplicó cada pago (FIFO por línea con saldo>0) |
| `DOCUMENTO_CUOTAS` | (EMPRESA, TIPO, SERIE, NUMERO, NRO_CUOTA) | cuotas del documento crédito: vencimiento, monto, **SALDO por cuota**, estado 'PE'/'CA' |

## 2. Procedimientos y funciones clave (lógica relevada)

### `F_OBTENERNUMERO(empresa, tipo, serie)`
Devuelve `NUMERO_ACTUAL+1` del **primer rango vigente**: `FECHA_VENCIMIENTO >= hoy AND NUMERO_ACTUAL < NUMERO_HASTA AND ESTADO='A' ORDER BY NUMERO_DESDE`. Retorna −1 si no hay rango (error de parametrización).

### `P_CREARCOMPROBANTE(...)`
1. Si `numero=0` → numera con F_OBTENERNUMERO e **incrementa el rango**; toma el TIMBRADO del rango si no vino.
2. Valida duplicado (empresa+tipo+serie+numero) y período inicio ≤ fin.
3. Inserta cabecera con **totales en 0 y ESTADO 'PE'** — los detalles van sumando por trigger.

### `P_CREARDETALLECPTE(...)`
Valida: documento existe y no está 'AN'; artículo existe; stock suficiente si el tipo afecta 'I' (no aplica a inmobiliaria); **límite de crédito del cliente** (deuda actual + nueva línea vs limite_credito → error). Calcula precio desde lista si no vino; **redondeo comercial Gs.** (a 50/100); inserta detalle (y línea de descuento por promoción si aplica).

### `P_PAGARCOMPROBANTE(tipo_pago 'C'|'T', ...)`
- **Abre la PLANILLA (caja) del día automáticamente** si el usuario no tiene una abierta (empresa+sucursal+usuario+fecha).
- Modo **'C'** (contra un comprobante): valida `saldo>0`, `monto<=saldo`, monto>0; conversión multimoneda con cotización; **recibo automático** (rango 'RECI') si el doc es crédito con cta.cte.; inserta COBROS (estado 'A') + COBRODETALLE → los triggers hacen el resto.
- Modo **'T'** (pago total): recorre los comprobantes pendientes del cliente **FIFO por fecha/vencimiento** y aplica el monto en cascada llamándose recursivamente en modo 'C'.

### `P_ANULARCOBRO(...)`
Registra cada detalle en **ANULACIONES** (motivo obligatorio) → COBROS a estado 'H' → el trigger TD_COBROS pone los COBRODETALLE en 'H' → el trigger TD_COBRODETALLE **devuelve el saldo** al documento.

### `P_ANULARCOMPROBANTE(...)`
Valida existe y no está 'AN'; borra cobrodetalles activos y detalles afectados; **archiva** documento+detalles en tablas espejo `DOCUMENTO_AN`/`DETALLE_DOCUMENTO_AN`; documento a 'AN' con motivo; registra en ANULACIONES.

### `F_SALDOCPTE` / `F_DEUDACLIENTE`
Saldo = columna SALDO del documento. Deuda cliente = Σ saldos de docs con AFECTACION 'I' y CTA_CTE 'C' − Σ totales de docs 'E'/'C' (notas de crédito).

### `P_DETALLES_AFECTADOS(...)` (llamado por cada pago)
Distribuye el monto pagado entre las **líneas** del documento con saldo>0 en orden de `NRO_DETALLE` (FIFO), registrando cada aplicación en `DETALLES_AFECTADOS`.

### `P_ACTUALIZASALDOCUOTAS(...)` (llamado tras pagar o anular)
Recalcula las cuotas del documento: resetea todas a 'PE' con saldo=monto y luego aplica lo total pagado del documento **cuota a cuota en cascada**: cubre completa → saldo 0 y 'CA'; parcial → saldo restante y 'PE'. Es la lógica exacta que necesitan las cuotas de alquiler de SGInmo (pagos parciales incluidos).

### `F_FACTURAS_COBRODETALLE`
Helper de impresión: lista "TIPO-SERIE-NUMERO" de los comprobantes pagados por un cobro.

## 3. Triggers (los que sostienen la integridad del dinero)

| Trigger | Momento | Qué hace |
|---|---|---|
| `TAI_DETALLE_DOCUMENTO` | BEFORE I/U/D detalle | prohíbe cambiar la clave; calcula **exentas/gravadas/impuestos de la línea** (IVA incluido: `imp = gravadas − gravadas/(1+p/100)`, redondeo por decimales de la moneda); valida stock; al borrar exige doc 'PE' y saldo==total |
| `TD_DETALLE_DOCUMENTO` | AFTER I/U/D detalle | **suma/resta totales y SALDO en la cabecera**; actualiza existencias y último costo |
| `TA_DOCUMENTO` | BEFORE UPDATE doc | **deriva ESTADO desde SALDO**: <0 → error; =0 → 'CA'; >0 → 'PE'; capea saldo ≤ total |
| `TAI_DOCUMENTO` | BEFORE INSERT doc | fija cotización USD/GS del día |
| `TD_COBRODETALLE` | AFTER I/U/D cobrodetalle | INSERT: `doc.saldo −= monto`, `cobro.monto += monto`; DELETE/anulación: reverso exacto |
| `TD_COBROS` | BEFORE UPDATE cobros | anulación en cascada: cobro→'H' ⇒ detalles→'H' |
| `TA_COBRODETALLE` | BEFORE UPDATE | prohíbe modificar registros anulados ('H') |

## 4. Traducción a SGInmo Web (qué se adopta y dónde vive)

**Principio (ACTUALIZADO por decisión del usuario, 2026-07-05):** la lógica de consistencia SÍ vive en la base de datos, como en Gestión — triggers PL/pgSQL para el cuadre básico (documento/cobros/utilización/liberación de saldos) y procedimientos/funciones/vistas para las operaciones estándar (pagos, anulaciones, saldos). Motivo: consistencia única ante múltiples clientes futuros (web, web services, integraciones). Los servicios Java llaman a los SPs dentro de sus transacciones; las correcciones al patrón Oracle (sin COMMIT interno, numeración con FOR UPDATE, CHECKs de respaldo) se aplican al portarlos. Los fuentes de Oracle de este documento son la ESPECIFICACIÓN a portar.

Se adopta en el esquema (doc 09 §B.5 actualizado):

1. `documentos` con **saldo vivo + estado derivado** (PENDIENTE/CANCELADO/ANULADO ⇔ 'PE'/'CA'/'AN'), fecha_vencimiento, timbrado, referencia a otro documento, cotización.
2. **`rangos_comprobantes`**: numeración administrada por (empresa, tipo, serie) con vencimiento y timbrado — reemplaza la numeración manual del legado SGInmo (resuelve la vieja P8).
3. **`anulaciones`** + catálogo de motivos: toda anulación (cobro o documento) queda logueada con motivo y usuario.
4. **`planillas`** (caja diaria): regla "no registrar cobros sin planilla abierta" — [A CONFIRMAR por el usuario si aplica a la inmobiliaria].
5. Invariantes de servicio (espejo de los triggers): crear detalle recalcula totales+saldo de cabecera; cobrar descuenta saldo y deriva estado; anular revierte exacto; nunca saldo<0 ni saldo>total; prohibido tocar registros anulados.
6. Reglas de negocio a portar: pago 'C'/'T' (FIFO por vencimiento), recibo automático para crédito, redondeo comercial Gs. a 50/100, límite de crédito (aplicable a inquilinos si se define), deuda de cliente.
7. **`forma_pago` parametriza `datos_cobros`**: cada forma de pago define los campos exigibles al cobrar (`requiere_*`: emisor/procesador, número/serie, vencimiento, cuenta, referencia, cobrador, depósito, rechazo, nota de crédito). El servicio valida esos flags antes de registrar `cobros` + `datos_cobros`.
8. **`articulo` reemplaza `items_ingresos_egresos`**: Oracle `DETALLE_DOCUMENTO` referencia `ARTICULOS`, por tanto SGInmo usa un maestro unico `articulo` para productos/servicios y lo referencia desde `documentos_detalles`, `ingresos_egresos` y `liquidaciones_detalles`. Se porta lo util de `ARTICULOS`, `DETALLE_ARTICULO`, `ANEXO_ARTICULO` y `ARTICULO_COSTO`.

**No se adopta:** stock/existencias como logica operativa, promociones, asientos contables (fuera del alcance de SGInmo por ahora), tablas espejo `*_AN` (la web conserva el documento anulado en la misma tabla con estado ANULADO + log en anulaciones — más simple y auditable).

## 5. Observaciones técnicas del sistema auditado (para conocimiento)

- Varios objetos INVALID en el esquema (F_CREARCOMPROBANTE, P_PAGARCOMPROBANTEBK, P_GENERARASIENTOCOBRO, etc.) — versiones viejas/backup, no afectan.
- `P_CREARCOMPROBANTE` hace `COMMIT` dentro del procedure (rompe atomicidad si el caller falla después) — en la web la transacción es una sola por caso de uso.
- La numeración lee `NUMERO_ACTUAL+1` y actualiza sin lock explícito — en PostgreSQL se hará con `SELECT ... FOR UPDATE` sobre el rango para evitar duplicados bajo concurrencia.
- El estado del documento se deriva por trigger de UPDATE — sensible al orden de operaciones; en la web será una función pura del saldo dentro del servicio.
