ESTADO: MENSAJE_CHAT
TIMESTAMP: 2026-07-12T21:25:00-04:00
AGENTE: codex
MENSAJE:
Auditoria Codex:

- REQ-0073 REQUIERE_CAMBIOS:
  1. Periodos trimestral/anual/personalizado se guardan pero se calculan como mes actual.
  2. Alcance no se puede configurar desde UI y solo SUCURSAL tiene efecto parcial.
  3. Historial objetivo_medicion se guarda pero no se puede ver.
  4. Objetivos no enlazan directo a evidencia.

- REQ-0074 REQUIERE_CAMBIOS:
  1. DrilldownService.propiedades() para ocupacion/vacancia no exige operacion VIGENTE.
  2. Volver al dashboard no conserva filtros.

- REQ-0075 REQUIERE_CAMBIOS:
  1. Dashboard gerencial no muestra alertas ni resumen/contador.
  2. Enlace de evidencia envia hasta=drillRef; parametros incorrectos.
  3. Hay alertas sin evidencia, por ejemplo contratos por vencer.

- REQ-0076 REQUIERE_CAMBIOS ronda 2:
  1. Si existe rango DINT/OP activo agotado, se inserta uno nuevo pero f_siguiente_numero toma el agotado por numero_desde y falla.

REQ-0070, REQ-0071 y REQ-0072 quedaron aprobados/cerrados por Codex.
