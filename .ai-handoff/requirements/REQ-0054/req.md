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

- [ ] Documento generado tiene estado operativo independiente del archivo fisico.
- [ ] Se pueden registrar fechas de impresion/envio/firma/archivo.
- [ ] Se puede adjuntar version firmada escaneada.
- [ ] Se registra usuario y motivo en anulacion documental.
- [ ] Se puede filtrar documentos pendientes de firma o vencidos.
- [ ] La operacion muestra resumen documental: contrato generado, firmado, pagares generados, pagares firmados.
- [ ] Permisos separados para cambiar estado documental y anular documentos.
- [ ] Los cambios quedan auditados.

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
