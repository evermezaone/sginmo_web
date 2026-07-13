# REQ-0083 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

1. **La aprobacion puede generar doble cobro en concurrencia sobre la misma transferencia.**

   En `PortalTransferenciaService#aprobar`, el estado se lee sin bloqueo (`SELECT ... WHERE portal_pago_transferencia = :id`), luego se llama a `cajaService.cobrar(...)`, y recien despues se actualiza la transferencia a `APLICADO` sin condicionar por estado. Dos operadores o dos requests simultaneos pueden leer `RECIBIDO`, generar dos cobros y finalmente actualizar la misma fila. El indice unico `ux_ppt_aplicada` no evita este caso porque actua sobre `numero_transaccion` entre filas aplicadas, no sobre doble aprobacion concurrente de la misma fila.

   **Impacto:** riesgo directo de doble aplicacion de dinero, doble imputacion y saldo incorrecto. Es uno de los casos que el estandar no permite aprobar: operaciones de cobro deben ser transaccionales y con invariantes fuertes.

   **Evidencia:** `PortalTransferenciaService.java` lineas 184-206; `V56__portal_pago_transferencia.sql` lineas 52-54.

   **Solucion esperada:** bloquear o reclamar atomicamente la transferencia antes de cobrar. Por ejemplo, dentro de la misma transaccion hacer `SELECT ... FOR UPDATE` o un `UPDATE ... SET estado='EN_REVISION' WHERE id=:id AND estado IN (...) RETURNING ...`, validar que afecto una sola fila, y al final actualizar a `APLICADO` con condicion de estado/version. Si otro request llega tarde debe fallar antes de invocar `cajaService.cobrar`. Tambien conviene usar `version` o condicion `estado IN (...)` en el update final.

2. **La validacion de archivo no verifica el MIME real ni la firma del contenido.**

   El REQ exige validar tipo MIME real, extension, tamanio y hash. La implementacion acepta el archivo si el nombre termina en `.pdf/.jpg/.png/.webp` o si el `Content-Type` enviado por el cliente contiene esas cadenas. Eso permite subir contenido arbitrario renombrado como PDF/imagen o con header manipulado.

   **Impacto:** el portal acepta archivos externos de clientes; aunque se guarden fuera del webroot, quedan disponibles para descarga por operadores y pueden contener contenido no permitido.

   **Evidencia:** `PortalTransferenciaService.java` lineas 64-65 y 256-263.

   **Solucion esperada:** validar por firma/magic bytes del contenido y cruzarlo con extension y MIME declarado. Minimo: `%PDF-` para PDF, `FF D8 FF` para JPEG, firma PNG `89 50 4E 47 0D 0A 1A 0A`, y firma RIFF/WEBP. Rechazar si contenido, extension y MIME no coinciden con un tipo permitido.

### No Bloqueantes

- La Fase 1 excluye OCR y conciliacion bancaria por descomposicion explicita a `REQ-0084` y `REQ-0085`; no se considera incumplimiento de este REQ.
- La descarga del recibo desde el portal queda documentada como follow-up menor; el alcance de Fase 1 acepta que el recibo quede disponible internamente.

## Validacion Realizada

- Revision estatica de `PortalTransferenciaService`.
- Revision estatica de `PortalTransferenciaBean`.
- Revision estatica de `TransferenciaBandejaBean`.
- Revision estatica de `portal/transferencia.xhtml` y `transferencias.xhtml`.
- Revision estatica de `V56__portal_pago_transferencia.sql`.

## Pruebas

- No se ejecuto build porque la auditoria encontro bloqueantes de comportamiento/seguridad transaccional que no dependen de compilacion.
