# SGInmo — Documentación para migración a web corporativa

**Fecha de análisis:** 2026-07-03
**Fuente analizada:** `Pysistemas\Inmobiliaria` (solución `Pysistemas.sln`)
**Método:** análisis estático completo del código fuente (Modelo EDMX, 23 servicios, ~100 formularios WinForms, reportes Crystal)

## Índice

| Doc | Contenido |
|---|---|
| [01-stack-actual.md](01-stack-actual.md) | Stack tecnológico legado, arquitectura, riesgos y alertas de seguridad |
| [02-modelo-datos.md](02-modelo-datos.md) | 31 tablas + ~23 vistas: campos, tipos, relaciones, grafo de dependencias, enumeraciones |
| [03-reglas-negocio-nucleo.md](03-reglas-negocio-nucleo.md) | Reglas de negocio del núcleo (cobros, operaciones, cuotas, liquidaciones, renovaciones, lotes) extraídas de los formularios |
| [04-servicios-y-logica.md](04-servicios-y-logica.md) | Lógica de la capa Services: fórmulas, transiciones de estado, seguridad, deuda técnica |
| [05-soporte-seguridad-reportes.md](05-soporte-seguridad-reportes.md) | Autenticación, perfiles, parámetros del sistema, socios de negocios, catálogo completo de reportes |
| [06-propuesta-stack-web.md](06-propuesta-stack-web.md) | Stack definido: WildFly 40 + PrimeFaces + PostgreSQL, mapeo legado→nuevo y fases |
| [07-datos-reales.md](07-datos-reales.md) | Análisis de la BD real (Fase 0): volúmenes, DOMINIOS, SPs/triggers ocultos, calidad de datos |
| [08-backlog-reqs.md](08-backlog-reqs.md) | Backlog propuesto: 32 REQs en 7 fases para la metodología ai-handoff (PROJECT_CODE=SGI) |

## Resumen ejecutivo

**Qué es:** ERP inmobiliario de escritorio (WinForms, .NET Framework 4.0, EF5 database-first, Firebird, Crystal Reports). Multiempresa, multisucursal, con soporte multimoneda *nominal* (en la práctica la moneda está fijada a ID 1 = Guaraní).

**Dominios funcionales:**
1. **Entidades inmobiliarias** (edificios/loteamientos) y **propiedades/lotes**, con propietarios N:M e imágenes.
2. **Operaciones** (contratos de ALQUILER/VENTA) con generación automática de **cronogramas de cuotas**, renovación, rescisión y regeneración de cuotas.
3. **Cobros** de cuotas con cálculo automático de **mora** (días de gracia + monto/día), descuentos y anulación (solo ADMINISTRADOR).
4. **Liquidaciones** al fin del contrato (garantía − gastos por plantilla) que liberan la propiedad.
5. **Ingresos/Egresos** (incluye comisiones y depósitos de garantía generados automáticamente al crear operaciones).
6. **~17 reportes** Crystal Reports (recaudación, vencimientos, cuentas a cobrar/pagar, estado de cuentas).

**Máquinas de estado clave:**
- Operación: `VIGENTE → FINALIZADO` (renovación/liquidación) / `RESCINDIDO`
- Cuota: `PENDIENTE → CANCELADO` (cobro) y reversa al anular cobro
- Cobro: `CANCELADO → ANULADO` (solo ADMINISTRADOR; revierte cuotas)
- Propiedad: `LIBRE → OCUPADA/VENDIDA → LIBRE` (al liquidar)

**Riesgos críticos hallados (corregir en la migración):**
1. Contraseñas "encriptadas" con **Base64(UTF-16)** — reversible, seguridad nula.
2. Credenciales de BD **hardcodeadas** (`sysdba/masterkey`) en `app.config` y en código, con IPs públicas de producción.
3. **Sin transacciones** atómicas en cobros/operaciones/liquidaciones (multi-tabla).
4. Renovación de contratos **duplica cuotas** (bug reconocido en comentario del código).
5. Redondeo por `ToString("N0")` que trunca decimales.
6. Moneda hardcodeada (ID 1) pese al diseño multimoneda.
