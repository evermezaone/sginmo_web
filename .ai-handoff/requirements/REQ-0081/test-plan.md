# REQ-0081 - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | BUILD OK | OK |
| T02 | Deploy VPS + smoke render | caja 200; 36/36 | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Grilla en pantalla ancha | Abrir caja con cobros | #, Fecha, Cliente, Monto completo, Forma, Estado, Acciones sin truncar | pendiente |
| M02 | Pantalla angosta | Reducir ancho de ventana | Paneles se apilan; grilla scrollea si es necesario, sin romper la pagina | pendiente |

## Datos De Prueba

Caja abierta con >= 1 cobro.
