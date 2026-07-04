# 04 — Capa Services y Utilidades: lógica y deuda técnica

23 servicios en `Services\` + 6 utilidades en `Utilidades\`. Los servicios son mayormente wrappers CRUD con auditoría; la lógica sustantiva está en los que siguen.

## 1. CobrosService (`Services\Cobros\CobrosService.cs`)

- **CalcularMora(fechaVencimiento, fechaCobro, diasGracia, montoMoraDia)**:
  `diasMora = (fechaCobro − (fechaVencimiento + diasGracia)).Days` (si positivo); `montoMora = diasMora × montoMoraDia`. Mora simple, sin interés compuesto.
- **GuardarCobro(cobro, detalles, operacion)**:
  - "N": inserta cabecera COBROS; por cada detalle: si TIPO ≠ DESCUENTO → `CancelarCuota()` marca la CRONOGRAMAS_CUOTAS referida (DOCUMENTO_ID) como `CANCELADO` con FECHA_CANCELACION = fecha cobro. Si es DESCUENTO → toma DOCUMENTO_ID del primer detalle (lógica ambigua).
  - "E": solo actualiza cabecera, no regenera detalles.
- ⚠️ **Sin transacción** entre cabecera, detalles y actualización de cuotas; sin validación previa de existencia de la cuota.

## 2. CronogramasCuotasService

- **GenerarCuotas(operacion)**:
  - Monto por cuota: VENTA → `precio / plazo`; ALQUILER → `precio` (igual cada mes).
  - ⚠️ Redondeo con `ToString("N0")` → **trunca a entero** (los centavos se pierden; hoy irrelevante en Gs., crítico si se habilita USD).
  - Vencimientos: cuota 1 = FECHA_INICIO_CONTRATO; siguientes = +1 mes, ajustando DIA_PAGO al calendario (feb 28/29; meses de 30 días → día 30), restaurando el día original en cada iteración.
  - Campos generados: NUMERO_CUOTA 1..plazo, ESTADO=PENDIENTE, **MONEDA_ID=1 hardcodeado**.
- **BorrarCuotasParaRegenerar()**: elimina cuotas sin validar si ya tienen cobros asociados ⚠️.
- **RefinanciarCuotas()**: **método vacío** (no implementado).
- **CalculaDiaPago()**: contiene bug (variable no inicializada) pero no se invoca.

## 3. OperacionesService

**GuardarOperacion(operacion, cuotas, tipoOperacionABM)**:
- "N" (nuevo): inserta OPERACIONES_PROPIEDADES + cuotas; **cambia estado de la propiedad**: ALQUILER → `OCUPADA`, VENTA → `VENDIDA`.
- "R" (renovar): inserta las cuotas nuevas. ⚠️ **BUG reconocido en comentario del código: no elimina las cuotas previas → duplicación**.
- "E" (editar): actualiza operación y cuotas existentes.
- ⚠️ Sin transacción multi-tabla.

## 4. LiquidacionesService

**Guardar(liquidacion, items, operacionABM)**:
1. Inserta/actualiza LIQUIDACIONES.
2. Por cada ítem: ID=0 → inserta LIQUIDACIONES_DETALLES; ID>0 → actualiza.
3. **Cierra la operación**: OPERACIONES_PROPIEDADES.ESTADO = `FINALIZADO` y PROPIEDADES.ESTADO = `LIBRE`.
- ⚠️ No previene liquidaciones duplicadas para la misma operación; USUARIO_MODIFICACION toma el usuario de la cabecera (no el actual); sin transacción.

## 5. PropiedadesService / EntidadesInmobiliariaService

- PropiedadesService: CRUD; la validación/gestión de propietarios está **comentada** (nunca se ejecuta) — propietarios opcionales de facto.
- EntidadesInmobiliariaService: exige ≥1 propietario pero retorna `null` con mensaje informativo en vez de excepción; en EDITAR **solo agrega** propietarios nuevos, nunca elimina los removidos ⚠️.

## 6. Seguridad

- **IngresoSistemaService.IngresoSistema(usuario, password)**: busca por CODIGO_USUARIO → compara `Encriptar(password)` contra USUARIOS.PASSWORD → exige ACTIVO="SI". Respuesta {Status OK/ERROR, Mensaje, Entity}.
- **PysistemasSeguridad.Encriptar/Desencriptar**: `Base64(Encoding.Unicode(clave))` — **totalmente reversible, no es hash**. Riesgo crítico: BD comprometida = todas las contraseñas expuestas. Sin límite de intentos ni bloqueo de cuenta.
- **UsuariosService**: al crear usuario copia las VARIABLES_ENTORNOS del usuario "admin" (herencia de configuración). En EDITAR, FECHA_MODIFICACION está comentada (no se registra).

## 7. PysistemasConfiguraciones (multi-tenant y conexión)

- Archivo `.ini` junto al exe con la conexión; si no existe lo crea con **sysdba/masterkey y `c:\inmobiliaria.fdb` hardcodeados** ⚠️. Reconstruye el connection string de EF en runtime.
- **Variables de entorno por usuario** (tabla VARIABLES_ENTORNOS, diccionario en memoria): `empresa_id`, `empresa`, `sucursal_id`, `sucursal`. `CambiarVariablesEntorno()` al cambiar de empresa elige la sucursal con POR_DEFECTO="SI". Este mecanismo es el **contexto multi-tenant** → en web pasa a claims/sesión.

## 8. Servicios paramétricos

- **MonedasService.ObtenerMonedaPorDefecto()**: TIPO_MONEDA="LOCAL" (puede retornar null).
- **EmpresasService.Guardar()**: si no hay socio asociado crea SOCIOS_NEGOCIOS (ES_EMPRESA=SI, CLASIFICACION_FISCAL="GRAVADA" fijo) + crea SUCURSAL por defecto con el nombre de la empresa.
- Territoriales (Paises/Departamentos/Ciudades/Barrios + puentes): CRUD con antipatrón repetido en EDITAR (extraer → recargar → reasignar); códigos mágicos "123456"/"9999"/"9426"; typo `FECHA_MODIFCACION` en CiudadesService.
- FormasPagosService, ImpuestosService, IngresosEgresosService, ItemsIngresosEgresosService: CRUD simples con auditoría.

## 9. Utilidades transversales

| Utilidad | Función | Nota migración |
|---|---|---|
| `ControlesUtilidades.calcularDigitoVerificador` | **módulo 11** (pesos 2..9 cíclicos; si resto ≥10 → 0) para CI/RUC | portar tal cual al backend |
| `ControlesUtilidades.GetPuntoVentaAutomaticamente` | numeración "001-001-NNNNNNN" | portar (numeración de comprobantes) |
| `ControlesUtilidades.Convertir_Numero` | número → letras (contratos/pagarés) — typo "milllones" | portar o reemplazar por librería |
| `camposNumericos`, `aplicarFormatoNumerico` | validación/formato de inputs | pasa al frontend |
| `PysistemasMensajes` | wrappers MessageBox | reemplazar por respuestas HTTP/notificaciones UI |
| `PysistemasLogger` | Trace.WriteLine con timestamp | reemplazar por logging estructurado |
| `RestUtility` | HTTP helper con Basic Auth | reemplazar |
| `ValidacionesUtilidades` | fail-fast + mapeo de códigos de error (incompleto) | reemplazar por validación del framework |

## 10. Deuda técnica consolidada (prioridad)

**CRÍTICA (seguridad):** contraseñas reversibles; credenciales hardcodeadas; sin protección de fuerza bruta; BD expuesta al cliente.

**ALTA (negocio):** duplicación de cuotas al renovar; moneda hardcodeada; redondeo que trunca; RefinanciarCuotas vacío; sin transacciones multi-tabla en cobros/operaciones/liquidaciones; liquidaciones duplicables.

**MEDIA:** validación de propietarios comentada; EDITAR de propietarios no elimina; antipatrón EDITAR repetido; loops O(n·m) en liquidaciones.

**BAJA:** typos (`FECHA_MODIFCACION`, "milllones"); valores mágicos en códigos territoriales; booleanos como strings SI/NO.
