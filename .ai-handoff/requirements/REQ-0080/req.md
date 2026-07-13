# REQ-0080 - BUG comprobantes: ClassCastException LocalDate a java.sql.Date en cobrosRecientes rompe la pantalla

**Numero:** REQ-0080
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Bug detectado por smoke-test durante REQ-0079: la pantalla `comprobantes` devuelve HTTP 500 cuando existen cobros.

## Objetivo Funcional

La pantalla de comprobantes/recibos debe renderizar y listar los cobros recientes sin error, aunque el driver JDBC/Hibernate devuelva las columnas `date` como `java.time.LocalDate`.

## Causa Raiz

`ComprobanteService.cobrosRecientes()` (linea 66) y `reciboCobro()` (linea 95) casteaban `cobro.fecha` a `java.sql.Date`. Hibernate 7 / el driver de PostgreSQL devuelven una columna `date` como `java.time.LocalDate`, por lo que el cast lanza `ClassCastException`. Bug latente: solo se disparaba al existir al menos un cobro en la ventana (con datos vacios el loop no ejecutaba el cast).

## Criterios De Aceptacion

- [x] `comprobantes` renderiza HTTP 200 con cobros existentes (smoke 36/36).
- [x] `cobrosRecientes()` y `reciboCobro()` convierten la fecha de forma defensiva (LocalDate o java.sql.Date o Timestamp).
- [x] No se introduce dependencia de un tipo de fecha concreto del driver.

## Dependencias

- Depende de: REQ-0058 (comprobantes/recibos).
- Requerido por: pantalla comprobantes operativa.
