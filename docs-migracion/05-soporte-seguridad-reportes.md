# 05 — Módulos de soporte: seguridad, mantenimiento, reportes

## 1. Arranque y sesión

`Inicio.cs` (Main) → Splash → **Login** (`Presentacion\Inicio\Login.cs`) → carga de VARIABLES_ENTORNOS (empresa/sucursal activas del usuario) → **Principal** (MDI con menú). Recuerda el último usuario logueado en configuración local.

## 2. Permisos y roles

Dos perfiles (`USUARIOS.PERFIL`): **ADMINISTRADOR** y **USUARIO**.

| Restricción | Dónde se aplica |
|---|---|
| Menús Informes, Usuarios y Procesos visibles solo para ADMINISTRADOR | `Principal.cs` |
| Anular cobros | `FrmCobros.cs` |
| Aplicar descuentos | `FrmCobros.cs` |
| Regenerar cuotas | `Principal.cs` |
| Parámetros del sistema | `Principal.cs` |

⚠️ El control es solo de UI (ocultar menús/botones); no hay autorización en la capa de servicios. En web se necesita autorización real en el backend (policies/middleware) y conviene evaluar roles más granulares (COBRADOR, CONSULTA, GERENTE).

## 3. Socios de negocios (`FrmSociosNegocios.cs`)

Persona/empresa única con **flags de rol combinables**: propietario, inquilino, portero, administrador, cliente, proveedor, empresa, vendedor.

Validaciones: razón social, tipo+número de documento, personería, dirección y teléfono obligatorios; documento único (si ya existe, autocarga el registro); dígito verificador módulo 11 si es contribuyente; sexo obligatorio en persona física. Campo UBICACION admite URL de mapa.

## 4. Usuarios (`FrmUsuarios.cs`)

Código de usuario único e inmutable; contraseña con confirmación; perfil obligatorio (dominio TIPOS_PERFILES); ACTIVO SI/NO; vinculado a EMPRESA_ID.

## 5. Parámetros del sistema (`FrmParametrosSistema.cs`, solo ADMINISTRADOR)

Editan valores de la tabla DOMINIOS:

| Parámetro | Dominio | Nota |
|---|---|---|
| Salario mínimo | SUELDO_MINIMO | |
| Jornal mínimo | JORNAL_MINIMO | = salario/26 |
| % comisión venta | PORCENTAJE_COMISION_VENTA | default de propiedades |
| % comisión alquiler | PORCENTAJE_COMISION_ALQUILER | default de propiedades |
| Mora por día | MONTO_MORA_DEFECTO | default por operación |
| Días de gracia | DIAS_GRACIA_VENCIMIENTO | default por operación |

## 6. Catálogos (Mantenimiento)

Empresas (crea socio + sucursal por defecto), Sucursales (POR_DEFECTO), Formas de pago (código corto, REQUERE_BANCO, DIAS), Monedas (tipo LOCAL/extranjera, precisión), Impuestos (%, factores), Geografía (País→Departamento→Ciudad→Barrio con tablas puente). Todos ABM estándar heredando `FrmMaestro`.

## 7. Ingresos/Egresos y Gastos

Dos módulos casi idénticos (`IngresosEgresos\` y `Gastos\`) sobre las mismas tablas: registrar movimientos INGRESO/EGRESO con ítem de catálogo, socio, propiedad/entidad, factura y forma de pago. Los movimientos de comisión y depósito de garantía se generan automáticamente desde Operaciones (ver doc 03 §2). En la migración: **unificar en un solo módulo**.

## 8. Patrón base de la UI

- `FrmMaestro`: base de todos los ABM (estilo de grillas, limpiar, confirmación de cierre) → en web equivale a componentes CRUD genéricos (tabla + formulario + validación).
- `FrmBuscadorGenerico`: base de los ~15 buscadores modales → en web: componentes de búsqueda/autocomplete reutilizables.

## 9. Catálogo de reportes (Crystal Reports → rehacer en web)

| # | Reporte | Parámetros | Contenido |
|---|---|---|---|
| 1 | Datos de inmuebles | entidad, propiedad | ficha/listado de propiedades |
| 2 | Situación de inmuebles | entidad, propiedad | estado ocupada/libre/vendida |
| 3 | Ingresos fijos por predios | entidad | ingresos mensuales por alquileres |
| 4 | Contratos a vencer | período | operaciones próximas a expirar |
| 5 | Ventas de inmuebles | período, entidad | historial de ventas |
| 6 | Planilla de cobros de alquileres | período, entidad | cobros a inquilinos |
| 7 | Cuotas cobradas | período, entidad | consolidado de cuotas pagadas |
| 8 | Resumen de cuenta | socio | estado de cuenta individual |
| 9 | Cronograma de cuotas | entidad | todas las cuotas generadas |
| 10 | Vencimientos de cuotas por fechas | empresa, entidad, propiedad, desde/hasta | cuotas pendientes en rango |
| 11 | Cuentas a cobrar (semanal) | semana actual (dashboard) | cuotas vencidas de la semana |
| 12 | Planilla de egresos | período, entidad | gastos del período |
| 13 | Egresos por ítems | período, ítem | gastos por categoría |
| 14 | Cuentas a pagar / gastos administración | empresa, período | egresos administrativos |
| 15 | Liquidación de operación | operación | garantía − gastos, saldo |
| 16 | Ingresos vs egresos | período, entidad | comparativa |
| 17 | Planilla de recaudación mensual | empresa, período | resumen de cobros del mes |

Además: impresión de **contrato** y **pagaré** desde Operaciones (usa monto en letras), y estado de cuentas de socios. Exportación actual: PDF vía visor Crystal (`FrmReporteMaestro`).

## 10. Configuración local

`.ini` junto al exe (servidor/BD/credenciales ⚠️) + VARIABLES_ENTORNOS en BD por usuario (empresa/sucursal activa). En web: `.env`/secretos del servidor + claims de sesión.
