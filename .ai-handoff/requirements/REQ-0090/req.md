# REQ-0090 - Bug portal: ClassCastException LocalDate en resumen de cuenta; hardening de lecturas date en queries nativas (Hibernate 7)

**Numero:** REQ-0090
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** alta (bug bloqueante del portal)

## Texto Original

Reporte del usuario al entrar al portal del socio tras validar el OTP:

```
WELD-000049: Unable to invoke public void py.com.pysistemas.sginmo.web.PortalBean.iniciar()
Caused by: java.lang.ClassCastException: class java.time.LocalDate cannot be cast to class java.sql.Date
    at py.com.pysistemas.sginmo.servicio.PortalService.resumen(PortalService.java:57)
    at py.com.pysistemas.sginmo.web.PortalBean.iniciar(PortalBean.java:56)
```

## Objetivo Funcional

El portal del socio (`portal/inicio.xhtml`) debe cargar sin error. La causa raiz es el mismo defecto de
REQ-0080: con Hibernate 7 / el driver actual una columna `date` de una consulta nativa vuelve como
`java.time.LocalDate`, pero el codigo la casteaba de forma dura a `java.sql.Date`. Se elimina ese patron
en todos los servicios que aun lo tenian, con un helper defensivo.

## Criterios De Aceptacion

- [x] `portal/inicio.xhtml` carga sin ClassCastException (resumen de cuenta del socio).
- [x] No queda ningun cast duro `(java.sql.Date)` en lecturas de queries nativas (0 en el codigo).
- [x] Las pantallas admin que usaban esos casts (mora/cobranza, objetivos, transferencias) renderizan (smoke HTTP 200).
- [x] Sin cambios de esquema (no hay migracion nueva).

## Dependencias

- Depende de: ninguna
- Requerido por: ninguno (relacionado con REQ-0080, mismo defecto de tipo LocalDate)
