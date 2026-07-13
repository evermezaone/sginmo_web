# REQ-0079 - Proteger la anulacion de cobros en caja: control escondido, solo ultimo cobro del dia, con motivo

**Numero:** REQ-0079
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

En caja, la opcion de impresion de recibo esta muy cerca de la opcion de anulacion. La opcion de anulacion debe estar mas escondida, disponible solo para cobros del dia de hoy y el ultimo cobro. Tal vez debe habilitarse con una combinacion de teclas, o un boton mas escondido que habilite la opcion en la grilla. Ademas debe pedir un motivo.

## Objetivo Funcional

Evitar anulaciones de cobro por error (el boton "Anular" estaba pegado al de "Recibo PDF"). La anulacion pasa a ser una accion deliberada y acotada: se habilita con un control aparte, solo se ofrece para el ULTIMO cobro de la caja y solo si es del dia de hoy, exige motivo y queda auditada. La regla se valida en el backend (no basta con ocultar el boton).

## Criterios De Aceptacion

- [x] Por defecto la grilla de cobros NO muestra el boton de anular (X); solo el de recibo PDF.
- [x] Existe un control aparte ("Anular ultimo cobro...") que habilita el modo anulacion; al activarlo aparece un aviso y el selector de motivo.
- [x] En modo anulacion, la X solo aparece en la fila del ultimo cobro ACTIVO y solo si es del dia de hoy.
- [x] La anulacion exige motivo (obligatorio) y pide confirmacion.
- [x] El backend (CajaService.anularCobro) re-valida: cobro ACTIVO, fecha = hoy y que sea el ultimo cobro ACTIVO de su planilla; si no, rechaza con mensaje claro.
- [x] La anulacion queda auditada (accion ANULAR con motivo, REQ-0067).
- [x] Solo usuarios con permiso caja/INACTIVAR ven el control y pueden anular.

## Dependencias

- Depende de: REQ-0022/0023 (caja y cobros), REQ-0067 (auditoria funcional).
- Requerido por: operacion segura de caja diaria.
