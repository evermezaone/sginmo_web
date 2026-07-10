# Analisis - REQ-0041

## Enfoque Propuesto

Implementar un motor simple de plantillas documentales propio del sistema:

- Plantilla persistida en BD, preferentemente como HTML controlado o texto enriquecido limitado.
- Catalogo cerrado de variables resolubles por backend.
- Parser de placeholders `{{...}}` con validacion al guardar.
- Renderizador que arma un modelo de datos desde `Operacion`, `CronogramaCuota`, empresa/tenant y catalogos.
- Generador PDF con OpenPDF, reutilizando `PdfService` donde alcance y extendiendolo si hace falta para bloques, tablas, saltos de pagina y firmas.

La regla central: la plantilla define la forma del documento, pero el backend define que variables existen y como se resuelven. No debe ejecutarse expresion libre enviada por el usuario.

## Modelo Sugerido

Tablas posibles:

- `plantilla_documento`
  - `plantilla_documento` PK
  - `tenant`
  - `codigo`
  - `descripcion`
  - `tipo` (`CONTRATO`, `PAGARE`)
  - `tipo_operacion`
  - `tipo_contrato`
  - `version`
  - `cuerpo`
  - `formato_cuerpo` (`HTML_LIMITADO` o `TEXTO`)
  - `estado`
  - auditoria/version optimista

- `documento_generado`
  - `documento_generado` PK
  - `tenant`
  - `operacion`
  - `cronograma_cuota` nullable
  - `tipo`
  - `plantilla_documento`
  - `version_plantilla`
  - `nombre_archivo`
  - `hash_contenido`
  - auditoria

## Variables Minimas

Contrato:

- `{{empresa.razon_social}}`
- `{{empresa.ruc}}`
- `{{sucursal.descripcion}}`
- `{{operacion.numero}}`
- `{{operacion.fecha_inicio}}`
- `{{operacion.fecha_fin}}`
- `{{operacion.plazo}}`
- `{{operacion.monto_total}}`
- `{{operacion.monto_total_letras}}`
- `{{cliente.nombre}}`
- `{{cliente.documento}}`
- `{{activo.descripcion}}`
- `{{activo.direccion}}`
- `{{tipo_contrato.descripcion}}`

Pagare:

- `{{cuota.numero}}`
- `{{cuota.vencimiento}}`
- `{{cuota.monto}}`
- `{{cuota.monto_letras}}`
- `{{deudor.nombre}}`
- `{{deudor.documento}}`
- `{{acreedor.nombre}}`
- `{{operacion.numero}}`

## Riesgos A Evitar

- No hacer PDFs hardcodeados por tipo de contrato.
- No permitir expresiones libres o SQL en plantillas.
- No mezclar datos de otro tenant al resolver variables.
- No perder trazabilidad de que version de plantilla genero cada documento.
- No generar pagares desde cuotas inexistentes, anuladas o ajenas.
- No introducir JasperReports ni `.jrxml`.

## Verificacion Esperada

- Build Maven verde.
- Tests unitarios del parser de placeholders: variables validas, variables desconocidas, repetidas y escaping.
- Prueba de generacion de contrato PDF desde una operacion real.
- Prueba de pagare individual y lote de pagares desde cronograma.
- Prueba multiempresa: tenant A no puede usar operacion/plantilla propia de tenant B.
- Prueba de versionado: modificar plantilla no altera historial del documento ya generado.
