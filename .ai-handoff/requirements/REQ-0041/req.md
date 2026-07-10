# REQ-0041 - Generacion de contratos y pagares desde plantillas

**Numero:** REQ-0041
**Fecha de creacion:** 2026-07-10
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Analiza y escribe un req para que se genere documento exportable llenado automatico de contrato con plantillas. Que pueda generar tambien pagares.

## Decisiones De Alcance

- No se usa ni se usara JasperReports.
- No se admiten archivos `.jrxml` ni dependencias Jasper.
- La salida obligatoria es PDF exportable generado con la infraestructura vigente de OpenPDF.
- Las plantillas deben ser configurables por el sistema, no texto hardcodeado en Java.

## Objetivo Funcional

Permitir que el usuario genere documentos legales/operativos a partir de una operacion:

- Contrato de alquiler/venta, segun tipo de operacion y tipo de contrato.
- Pagare individual o lote de pagares asociado a las cuotas/cronograma de la operacion.

El documento debe llenarse automaticamente con datos reales del sistema usando plantillas parametrizables. El usuario debe poder previsualizar, generar y descargar el resultado como PDF.

## Criterios De Aceptacion

- [ ] Existe un maestro de plantillas de documento con al menos: codigo, descripcion, tipo (`CONTRATO`, `PAGARE`), tipo de operacion aplicable, tipo de contrato aplicable, tenant/empresa, estado, version y cuerpo de plantilla.
- [ ] Las plantillas soportan variables con sintaxis estable, por ejemplo `{{operacion.numero}}`, `{{cliente.nombre}}`, `{{activo.descripcion}}`, `{{cuota.numero}}`, `{{cuota.monto_letras}}`.
- [ ] El sistema muestra un catalogo de variables disponibles, con descripcion en espanol, para evitar que el usuario escriba nombres al azar.
- [ ] Al guardar una plantilla se validan variables desconocidas y se informa el error con mensaje claro.
- [ ] Desde el detalle de Operacion se puede generar el contrato usando la plantilla activa correspondiente.
- [ ] Desde el detalle de Operacion/Cronograma se puede generar pagare para una cuota puntual y tambien un lote de pagares para cuotas seleccionadas o pendientes.
- [ ] El llenado automatico toma datos reales de operacion, cliente, propietario si aplica, activo/propiedad, sucursal/empresa, montos, moneda, fechas, cuotas y condiciones contractuales.
- [ ] Los importes relevantes pueden imprimirse en numeros y en letras, reutilizando o implementando una utilidad equivalente a la regla legacy `Convertir_Numero` / `GET_MES_LETRAS`, corrigiendo errores conocidos si existen.
- [ ] El PDF generado incluye encabezado institucional, fecha/hora, usuario generador, datos de empresa/tenant y pie de pagina.
- [ ] El PDF respeta saltos de pagina y no corta bloques criticos como firmas, clausulas finales o datos del pagare.
- [ ] El documento generado queda registrado en historial: operacion, tipo, plantilla/version usada, usuario, fecha/hora y archivo/nombre generado o hash del contenido.
- [ ] Si una plantilla cambia, documentos ya generados conservan trazabilidad a la version usada.
- [ ] Permisos separados: ver plantillas, crear/editar plantillas, activar/inactivar plantillas, generar contratos, generar pagares, descargar documentos generados.
- [ ] Aislamiento multiempresa: un usuario solo ve/usa plantillas globales o de su tenant; no puede generar documentos de operaciones de otro tenant.
- [ ] No hay JasperReports, `.jrxml`, Crystal ni generacion de documentos hardcodeada por caso.

## Reglas De Negocio

- Una operacion sin datos minimos obligatorios no permite generar contrato; el mensaje debe indicar que dato falta.
- Un pagare debe corresponder a una cuota existente de la operacion y reflejar monto, vencimiento, moneda y datos del deudor.
- Para lote de pagares, cada pagare debe quedar separado visualmente y ser imprimible sin mezclar datos entre cuotas.
- La plantilla activa se resuelve por prioridad: tenant especifico > global; tipo exacto de operacion/contrato > generica.
- Las plantillas globales solo las administra SUPERADMIN; los administradores de empresa pueden administrar plantillas propias si tienen permiso.
- Baja logica de plantillas; nunca DELETE si fueron usadas para generar documentos.

## Dependencias

- Depende de: REQ-0016, REQ-0017, REQ-0026, REQ-0038, REQ-0039.
- Requerido por: impresion formal de contratos, pagares y futura gestion documental.

## Fuentes Y Trazabilidad

- `docs-migracion/03-reglas-negocio-nucleo.md`: operaciones/contratos, cronograma, renovacion.
- `docs-migracion/04-servicios-y-logica.md`: `Convertir_Numero`.
- `docs-migracion/05-soporte-seguridad-reportes.md`: impresion de contrato y pagare desde Operaciones.
- `docs-migracion/06-propuesta-stack-web.md`: contrato/pagare con monto en letras.
- Decision usuario 2026-07-10: no usar JasperReports.
