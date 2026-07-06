# REQ-0011 - Parametros del sistema

**Numero:** REQ-0011 · **Estado:** implementado y validado por el usuario (2026-07-06)

## Objetivo Funcional
ABM de parametro_sistema: toda la configuracion viva (login, SMTP, mora, comisiones,
IMPUESTOS_MODO_AVANZADO) editable sin tocar codigo.

## Criterios De Aceptacion
- [x] Grilla lazy con busqueda por clave/descripcion; clave en monoespaciado.
- [x] Valores SENSIBLES (clave contiene CLAVE/PASS) enmascarados en la grilla; editables en el dialogo.
- [x] Clave INMUTABLE al editar (el codigo la referencia); alta con clave en mayusculas.
- [x] Sin borrado (configuracion viva); edicion de valor y descripcion.
- [x] Estandar: permisos por accion + enforcement en ParametroService, solo lectura,
      optimista con mensaje claro, errores traducidos, auditoria por permiso.
