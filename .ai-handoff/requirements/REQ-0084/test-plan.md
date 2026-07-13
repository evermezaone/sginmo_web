# REQ-0084 (Fase 2) - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` (con pdfbox) | BUILD OK | OK |
| T02 | Deploy VPS + Flyway V57 + smoke | transferencias 200; 37/37; schema v57 | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | OCR de PDF de texto | Informar transferencia con comprobante PDF -> abrir en bandeja | Seccion OCR con importe/fecha/nro/banco leidos + confianza | pendiente |
| M02 | Imagen sin tesseract | Informar con JPG/PNG | OCR "sin lectura automatica" (degrada), adjunto visible | pendiente |
| M03 | Tras instalar tesseract (ops) | Informar con imagen | Se extrae texto por Tesseract (motor TESSERACT) | pendiente (ops) |

## Datos De Prueba

Un comprobante de transferencia en PDF de texto.
