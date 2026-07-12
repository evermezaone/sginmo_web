# REQ-0044 - Quitar el campo Clasificacion fiscal del ABM de Persona

**Numero:** REQ-0044
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** baja

## Texto Original

Quitar el campo Clasificacion fiscal del ABM de Persona (no se usa en Paraguay).

## Objetivo Funcional

El formulario de alta/edicion de Persona ya no debe mostrar ni pedir el campo
"Clasificacion fiscal", porque no aplica al regimen tributario de Paraguay y solo agrega
ruido al ABM. El resto del ABM de Persona debe seguir funcionando sin cambios.

## Criterios De Aceptacion

- [x] El campo/label "Clasificacion fiscal" ya no aparece en el ABM de Persona (`personas.xhtml`).
- [x] Se elimina el `p:inputText` vinculado a `datosEmpresa.clasificacionFiscal` de la vista.
- [x] No se rompe el alta ni la edicion de Persona (fisica o juridica) tras quitar el campo.
- [x] No se pierde informacion: la columna `persona_empresa.clasificacion_fiscal` queda deprecada en BD (no se dropea), sin migracion de datos.
- [x] La pantalla `personas` sigue renderizando correctamente (smoke-test 19/19 render OK).

## Reglas De Negocio

- La clasificacion fiscal no se usa en Paraguay; su presencia confundia al operador.
- No se elimina el dato historico en BD; solo se retira de la interfaz.

## Dependencias

- Depende de: ninguna.
- Requerido por: limpieza del ABM de Persona (junto a REQ-0043/0045).
