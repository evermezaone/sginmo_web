# REQ-0008 - Catalogos: monedas, formas de pago e impuestos

**Numero:** REQ-0008 · **Estado:** implementado y validado por el usuario (2026-07-06)

## Objetivo Funcional
ABMs estandar de los tres catalogos economicos, con las reglas del negocio decididas:
impuestos en modo simplificado por defecto y formas de pago con habilitado y requisitos.

## Criterios De Aceptacion
- [x] Monedas: ABM completo; V12 agrega estado (baja logica) que faltaba en V1;
      precision decimal 0-6; unicidad de descripcion.
- [x] Impuestos: modo SIMPLIFICADO (parametro IMPUESTOS_MODO_AVANZADO='NO', decision del
      usuario): solo descripcion+porcentaje, factores y base gravada calculados solos
      (formulas de Gestion; exenta = factores 0/1). Modo avanzado edita factores y
      porcentaje_base_gravada (regimenes 20%/30%).
- [x] Formas de pago: habilitado separado de estado (V12, regla 1), "por defecto" unica
      (se apaga en las demas), pestana con los 14 requisitos del cobro (patron Gestion),
      unicidad de codigo.
- [x] Estandar completo en los tres (permisos por accion + enforcement en servicio,
      solo lectura, optimista, errores traducidos, auditoria por permiso).
