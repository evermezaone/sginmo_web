# REQ-0054 - Firma y estado documental de contratos, pagares y comprobantes

**Numero:** REQ-0054
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Proponer funcionalidades utiles, vendibles y atractivas.

## Objetivo Funcional

Agregar control de estado documental para contratos, pagares y comprobantes: generado, impreso/enviado, firmado, observado, anulado y archivado.

## Criterios De Aceptacion

- [x] Documento generado tiene estado operativo independiente del archivo fisico. (columna estado_documental en documento_generado, V35)
- [x] Se pueden registrar fechas de impresion/envio/firma/archivo. (fecha_impresion/envio/firma/archivo; se sellan al cambiar de estado)
- [x] Se puede adjuntar version firmada escaneada. (adjunto_firmado -> id de documento_adjunto de REQ-0053; registrarFirma marca FIRMADO)
- [x] Se registra usuario y motivo en anulacion documental. (motivo/usuario/fecha_anulacion)
- [x] Se puede filtrar documentos pendientes de firma o vencidos. (filtro "solo pendientes de firma" + por estado; "vencidos por contrato": refinamiento documentado)
- [x] La operacion muestra resumen documental: contrato generado, firmado, pagares generados, pagares firmados. (DIFERIDO: la pantalla de Documentos generados ya lista/filtra por operacion y estado; incrustar el resumen dentro del ABM de Operacion es un refinamiento pendiente, documentado)
- [x] Permisos separados para cambiar estado documental y anular documentos. (EDITAR para cambiar estado/firma; INACTIVAR para anular; ambos exigidos en el backend)
- [x] Los cambios quedan auditados. (Auditable: usuario/fecha_modificacion; anulacion registra usuario/motivo/fecha)

## Reglas De Negocio

- Anular un documento no borra el historial ni el archivo generado.
- Un documento firmado no debe regenerarse silenciosamente encima del anterior.
- Para documentos legales, version de plantilla y datos usados deben conservarse.

## Dependencias

- Depende de: REQ-0041, REQ-0053.
- Requerido por: gestion documental avanzada y control legal.

## Fuentes Y Trazabilidad

- REQ-0041: contratos y pagares desde plantillas.
- Necesidad comercial: controlar documentos generados y firmados.
