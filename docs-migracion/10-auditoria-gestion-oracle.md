# 10 — Auditoría del sistema de Gestión (Oracle 11g XE) — patrón documento/cobros

**Fuente:** BD `one@localhost:1521/XE` (Oracle Database 11g XE 11.2.0.2.0), auditada 2026-07-04.
**Propósito:** referencia obligatoria para el módulo de dinero de SGInmo Web (decisión P8 del usuario) y registro de triggers/procedimientos según pedido del usuario. Fuentes completas extraídas con `user_source` (1.210 líneas, en scratchpad de la sesión).

## 1. Modelo de comprobantes (confirmado contra el esquema real)

| Tabla | Clave primaria | Rol |
|---|---|---|
| `DOCUMENTO` | **(EMPRESA, TIPO, SERIE, NUMERO)** | comprobante universal: totales (exentas/gravadas/impuestos/total), **SALDO vivo**, ESTADO ('PE' pendiente / 'CA' cancelado / 'AN' anulado), FECHA_VENCIMIENTO, **TIMBRADO**, monedas cpte/local + COTIZACION, montos _EXT, referencia a otro doc (TIPO_REF/SERIE_REF/NUMERO_REF), cliente/c_identidad, sucursal, usuario |
| `DETALLE_DOCUMENTO` | (EMPRESA, TIPO, SERIE, NUMERO, **NRO_DETALLE**) | líneas: articulo/concepto, cantidad, precio, **exentas/gravadas/%imp/impuestos por línea**, estado, saldo |
| `TIPO_COMPROBANTE` | (TIPO) | catálogo: **AFECTACION 'I'/'E'** (entrada/salida de dinero), flag **CTA_CTE**, EXCENTO, prioridad, destino |
| `RANGO_COMPROBANTE` | (EMPRESA, SERIE, TIPO, NUMERO_DESDE) | numeración administrada: NUMERO_ACTUAL/HASTA, **FECHA_VENCIMIENTO** del rango, **CODIGO_AUTORZACION (timbrado)**, ESTADO 'A' |
| `COBROS` | (EMPRESA, CODIGO_COBRO, SUCURSAL) | acto de cobro: FPAGO, **PLANILLA (caja)**, cajero, moneda+cotización, monto_ext, vuelto, datos de cheque, RECIBO/FACTURA, ESTADO 'A' activo / 'H' anulado |
| `COBRODETALLE` | (EMPRESA, SUCURSAL, CODIGO_COBRO, SECUENCIA) | aplicación del cobro a un documento: (EMPRESA_CPTE, TIPO, SERIE, NUMERO) + MONTO |
| `PLANILLAS` | (EMPRESA, PLANILLA) | **caja diaria** por usuario/sucursal: apertura/cierre, montos, estado |
| `ANULACIONES` | (NRODETALLE) | log de anulaciones con motivo, usuario, fecha |
| `MOTIVO_DE_ANULACION` | | catálogo de motivos |

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

**Principio (estándar backend-jakarta.md):** en la web esta lógica NO va en triggers — va en los **servicios transaccionales** (`@Transactional`), con las mismas invariantes. Los triggers de Oracle son la ESPECIFICACIÓN del comportamiento.

Se adopta en el esquema (doc 09 §B.5 actualizado):

1. `documentos` con **saldo vivo + estado derivado** (PENDIENTE/CANCELADO/ANULADO ⇔ 'PE'/'CA'/'AN'), fecha_vencimiento, timbrado, referencia a otro documento, cotización.
2. **`rangos_comprobantes`**: numeración administrada por (empresa, tipo, serie) con vencimiento y timbrado — reemplaza la numeración manual del legado SGInmo (resuelve la vieja P8).
3. **`anulaciones`** + catálogo de motivos: toda anulación (cobro o documento) queda logueada con motivo y usuario.
4. **`planillas`** (caja diaria): regla "no registrar cobros sin planilla abierta" — [A CONFIRMAR por el usuario si aplica a la inmobiliaria].
5. Invariantes de servicio (espejo de los triggers): crear detalle recalcula totales+saldo de cabecera; cobrar descuenta saldo y deriva estado; anular revierte exacto; nunca saldo<0 ni saldo>total; prohibido tocar registros anulados.
6. Reglas de negocio a portar: pago 'C'/'T' (FIFO por vencimiento), recibo automático para crédito, redondeo comercial Gs. a 50/100, límite de crédito (aplicable a inquilinos si se define), deuda de cliente.

**No se adopta:** stock/existencias, promociones, artículos, asientos contables (fuera del alcance de SGInmo por ahora), tablas espejo `*_AN` (la web conserva el documento anulado en la misma tabla con estado ANULADO + log en anulaciones — más simple y auditable).

## 5. Observaciones técnicas del sistema auditado (para conocimiento)

- Varios objetos INVALID en el esquema (F_CREARCOMPROBANTE, P_PAGARCOMPROBANTEBK, P_GENERARASIENTOCOBRO, etc.) — versiones viejas/backup, no afectan.
- `P_CREARCOMPROBANTE` hace `COMMIT` dentro del procedure (rompe atomicidad si el caller falla después) — en la web la transacción es una sola por caso de uso.
- La numeración lee `NUMERO_ACTUAL+1` y actualiza sin lock explícito — en PostgreSQL se hará con `SELECT ... FOR UPDATE` sobre el rango para evitar duplicados bajo concurrencia.
- El estado del documento se deriva por trigger de UPDATE — sensible al orden de operaciones; en la web será una función pura del saldo dentro del servicio.
