# REQ-0076 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0076 (BUG)
- Tipo de cambio: backend (OperacionService, OperacionBean) + core (ErroresBd). Sin migracion (autoprovision en runtime).
- Riesgo: medio (toca el alta de operacion y el manejo de errores compartido)
- Archivos clave:
  - `onesystem-security/.../ErroresBd.java`: traducir() ahora surface los RAISE (SQLState P0001) como NegocioException (mensaje de negocio, limpio, una linea). Constraints se siguen traduciendo; errores tecnicos siguen relanzandose.
  - `sginmo-web/.../OperacionService.java` (crearDocumentoInterno): antes de f_siguiente_numero, autoprovisiona un rango_comprobante INTERNO amplio para DINT/OP del tenant si no existe (documento interno no fiscal). Idempotente (NOT EXISTS) y aislado por tenant (RLS).
  - `sginmo-web/.../web/OperacionBean.java` (crear): captura RuntimeException ademas de NegocioException -> ningun fallo queda silencioso.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy + `python tools/smoke-test-vps.py`: 36/36 (sin regresion).
  - Columnas de rango_comprobante confirmadas por migraciones (V1 + V26 rename empresa->tenant / tipo_codigo->tipo / drop tipo_lista; V4 version DEFAULT 0).
- Cambios de datos: no. Cambios de entorno: no.
- Decision esperada: aprobar; revisar la autoprovision del rango interno y el surface de P0001.
- Notas para auditor:
  - DINT/OP es interno (cuenta corriente del cronograma), no fiscal -> autoprovisionar un rango amplio es correcto; los documentos fiscales siguen requiriendo su timbrado real.
  - P0001 = RAISE EXCEPTION deliberado del motor -> se muestra; otros SQLState siguen sin exponerse crudos.
  - RLS: el INSERT del rango usa tenant = empresa actual (permitido por la policy).

## Resumen Funcional

Se puede registrar la operacion (se autoprovisiona el numerador interno del cronograma) y, ante cualquier
error, el usuario ve un mensaje claro en el dialogo en vez de "no pasar nada".

## Resumen Tecnico

ErroresBd surface P0001; crearDocumentoInterno autoprovisiona el rango interno DINT/OP; el bean captura RuntimeException.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| onesystem-security/.../ErroresBd.java | surface de RAISE P0001 |
| sginmo-web/.../OperacionService.java | autoprovision rango interno DINT/OP |
| sginmo-web/.../web/OperacionBean.java | catch RuntimeException (sin fallas silenciosas) |

## Cambios De Datos

Sin migracion; el rango interno se crea on-demand por tenant.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy; smoke 36/36. Columnas verificadas por migraciones.

## Pruebas Manuales Sugeridas

1. Alta de operacion a credito -> se registra, genera cronograma; se crea el rango DINT/OP si faltaba.
2. Forzar un RAISE (timbrado agotado de un doc fiscal) -> se muestra el mensaje en el dialogo.
3. Empresa nueva sin rango DINT -> primera operacion autoprovisiona y funciona.

## Riesgos Conocidos

- Autoprovision de numerador interno: acotada a DINT/OP no fiscal; idempotente; por tenant.
