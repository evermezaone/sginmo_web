# 08 — Backlog de requerimientos propuesto (SGI)

Derivado de los docs 00-07. Alcance: TODO lo programado en el legado (decisión del usuario, 2026-07-03). Orden = dependencia técnica; la regla del menor REQ pendiente hace que se desarrollen en este orden.

Estado: **PROPUESTO — pendiente de validación del usuario antes de cargar en la BD de coordinación.**

## Fase 1 — Fundaciones

| REQ | Título | Fuente / criterios clave | Riesgo |
|---|---|---|---|
| 0001 | Esqueleto del proyecto: Maven WAR + WildFly 40 + datasource PostgreSQL + Flyway operativo | doc 06; compila `mvn package` y deploya página base | bajo |
| 0002 | Dominio base: superclase Auditable + listener de auditoría + enums reales | doc 07 §3 (valores exactos), doc 02 | bajo |
| 0003 | Esquema PostgreSQL inicial (Flyway V1, 31 tablas normalizadas) + seed de dominios/parámetros reales | doc 02, doc 07 §2-3 (mora 50.000, gracia 3, comisiones 50/5) | medio |
| 0004 | Seguridad: login Jakarta Security + bcrypt + roles + bloqueo por intentos + logout | doc 05 §1-2; corrige bug #5/#6 del legado | alto |
| 0005 | Layout PrimeFaces + menú por rol + contexto de sesión empresa/sucursal (selector) | doc 05 §1-2, VARIABLES_ENTORNOS → sesión | medio |
| 0006 | Componentes genéricos: ABM base + buscador modal + growl/confirmaciones | patrón FrmMaestro/FrmBuscadorGenerico (doc 05 §8) | medio |

## Fase 2 — Catálogos (Mantenimiento)

| REQ | Título | Fuente | Riesgo |
|---|---|---|---|
| 0007 | Geografía: países/departamentos/ciudades/barrios + jerarquía | doc 02 §2, FrmPaises/Departamentos/Ciudades/Barrios | bajo |
| 0008 | Monedas, formas de pago e impuestos | doc 02 §2, doc 07 (4 monedas) | bajo |
| 0009 | Empresas + sucursales (auto-creación de socio y sucursal por defecto) | EmpresasService (doc 04 §8) | medio |
| 0010 | Usuarios: ABM, perfiles, activación, cambio de contraseña | doc 05 §4, UsuariosService | medio |
| 0011 | Parámetros del sistema (comisiones, mora, gracia, sueldo/jornal) — solo ADMINISTRADOR | doc 05 §5, FrmParametrosSistema | medio |

## Fase 3 — Núcleo inmobiliario

| REQ | Título | Fuente | Riesgo |
|---|---|---|---|
| 0012 | Socios de negocios: ABM completo, flags de rol, DV módulo 11, unicidad de documento, autocarga | doc 05 §3, FrmSociosNegocios | medio |
| 0013 | Entidades inmobiliarias + propietarios N:M + imágenes (storage archivos) | doc 03 §7 (RN-ENT-*), EntidadesInmobiliariaService — corrige EDITAR que no elimina propietarios | medio |
| 0014 | Propiedades: ABM, tipos, estados con color, comisiones default, imágenes | doc 03 §7 (RN-PROP-*), FrmPropiedades | medio |
| 0015 | Lotes + generación masiva de lotes (loteamiento + manzana + rango) | doc 03 §7 (RN-LOT-*, RN-GEN-*), FrmLotes/FrmGenerarLotes | medio |

## Fase 4 — Operaciones (contratos)

| REQ | Título | Fuente | Riesgo |
|---|---|---|---|
| 0016 | Operaciones alquiler/venta: alta/edición, validaciones, montos + monto en letras | doc 03 §2 (RN-OPE-001..011), FrmOperaciones | alto |
| 0017 | Cronograma de cuotas: generación (BigDecimal, ajuste última cuota, calendario DIA_PAGO), edición de cuotas | doc 03 §3 (RN-CUO-*), doc 04 §2 — corrige redondeo y moneda fija | alto |
| 0018 | Movimientos automáticos al crear operación: egreso comisión + ingreso depósito garantía | doc 03 §2 (RN-OPE-012/013) | alto |
| 0019 | Regeneración de cuotas (proceso, solo ADMINISTRADOR, transaccional) | doc 03 §7 (RN-REG-001), FrmRegenerarCuotas | alto |
| 0020 | Renovación de contratos: validación cuotas pendientes, precarga, cierre del anterior SIN duplicar cuotas | doc 03 §5 (RN-REN-*) — corrige bug #2 del legado | alto |
| 0021 | Rescisiones de operaciones | doc 03 §6, FrmRescisiones | medio |

## Fase 5 — Dinero

| REQ | Título | Fuente | Riesgo |
|---|---|---|---|
| 0022 | Cobros: búsqueda de deudor, cuotas pendientes con mora, grabación transaccional, numeración comprobante | doc 03 §1 (RN-COBR-001..005), CobrosService — corrige bug #1 (transacciones) | alto |
| 0023 | Descuentos y anulación de cobros (solo ADMINISTRADOR, reversa transaccional de cuotas) | doc 03 §1 (RN-COBR-006/007) | alto |
| 0024 | Ingresos/Egresos + catálogo de ítems (con APLICACION) | doc 02 §1.12-1.13, doc 05 §7 — unifica los 2 módulos duplicados del legado | medio |
| 0025 | Liquidaciones: plantilla de gastos, saldo garantía−gastos, cierre de operación y liberación de propiedad (transaccional) | doc 03 §4 (RN-LIQ-*, RN-PLANT-*), LiquidacionesService — unicidad por operación | alto |

## Fase 6 — Reportes (JasperReports, trazados a los SPs RPT_* de Firebird)

| REQ | Título | Fuente | Riesgo |
|---|---|---|---|
| 0026 | Infraestructura JasperReports + impresión de contrato y pagaré (monto en letras) | RPT_PAGARE, GET_MES_LETRAS, Convertir_Numero | medio |
| 0027 | Reportes de propiedades (datos, situación, ingresos fijos, contratos a vencer, ventas) | RPT_DATOS_PROPIEDADES*, RPT_INGRESOS_POR_ENTIDADES, RPT_PROPIEDADES_CONTRATO, RPT_VENTAS_PROPIEDADES | medio |
| 0028 | Reportes de cobros y cronogramas (planilla cobros, cuotas cobradas, resumen de cuenta, cronogramas, vencimientos, cuentas a cobrar semanal) | RPT_PLANILLA_COBROS_ALQUILERES, RPT_CUOTAS_COBRADAS, RPT_ESTADO_CUENTAS, RPT_CRONOGRAMA_* , SP_AUX_COBRO_CUOTA | medio |
| 0029 | Reportes de egresos y recaudación (planilla egresos, por ítems, cuentas a pagar, ingresos vs egresos, recaudación mensual, liquidación) | RPT_PLANILLA_EGRESOS, RPT_INGRESOS_EGRESOS*, RPT_EGRESOS_ADMINISTRADOR, RPT_PLANILLA_RECAUDACIONES, RPT_LIQUIDACION_OPERACION | medio |
| 0030 | Dashboard de inicio: vencimientos de la semana, ocupación, indicadores | doc 05 §9 (widget cuentas a cobrar semanal) | bajo |

## Fase 7 — Datos y puesta en producción

| REQ | Título | Fuente | Riesgo |
|---|---|---|---|
| 0031 | ETL Firebird → PostgreSQL (transformaciones y exclusiones del doc 07/estándar BD; passwords→bcrypt) | doc 07 §6, database-postgresql.md — no se ejecuta contra BD definitiva sin aviso | alto |
| 0032 | Provisión de servidor: WildFly 40 + PostgreSQL + HTTPS (reverse proxy) + procedimiento de deploy | doc 06 (infraestructura) | alto |

## Notas

- 32 REQs. Los lotes a Codex respetan el máximo de 5.
- Los REQs de reportes (0027-0029) agrupan varios reportes cada uno; si Codex o el usuario prefieren granularidad 1 reporte = 1 REQ, se dividen al llegar a la Fase 6.
- Módulos sin datos reales (VENTA, liquidaciones, rescisiones, ingresos/egresos): se implementan igual (alcance total); las dudas de comportamiento no documentado se escalan como `ESPERA_USUARIO` (regla anti-invención).
- Pendiente del usuario: confirmar si `CRONOGRAMAS_CUOTAS_PENDIENTE` es respaldo manual (afecta solo REQ-0031).
