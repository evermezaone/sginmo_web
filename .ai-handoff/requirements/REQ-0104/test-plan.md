# REQ-0104 - Plan De Pruebas

**Fecha:** 2026-07-16

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | Build multi-modulo | Build OK | OK |
| T02 | Deploy a la VPS + render login | Redeploy OK + HTTP 200 | OK (curl login.xhtml = 200) |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Monto grande en KPI | Inicio con datos reales, tarjeta "Saldo por cobrar" (1.225.995.000) | Numero completo dentro del recuadro, sin desborde | pendiente (usuario) |
| M02 | KPI chico | Ver "Operaciones vigentes" (40), "Cuotas vencidas" (45) | Tamano legible, tope 1.6rem | pendiente (usuario) |
| M03 | Responsive | Achicar la ventana | El numero se escala al ancho de la tarjeta sin desbordar | pendiente (usuario) |

## Datos De Prueba

Datos reales de Pysistemas (tenant 1) cargados en REQ-0103. El "Saldo por cobrar" tras la carga es
838.225.769 (o 1.225.995.000 antes del cobro), ambos montos de 9-10 digitos utiles para verificar.
