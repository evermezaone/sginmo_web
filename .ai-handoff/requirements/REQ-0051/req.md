# REQ-0051 - Monitor de salud operativa y panel de administracion tecnica

**Numero:** REQ-0051
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer funcionalidades vendibles y configuracion para elevar el nivel del programa.

## Objetivo Funcional

Crear un panel protegido de salud operativa que muestre estado de aplicacion, base de datos, backups, version desplegada, migraciones Flyway y espacio en disco de la VPS.

## Criterios De Aceptacion

- [x] Existe pantalla administrativa "Salud del sistema" visible solo para usuarios autorizados.
- [x] Muestra estado de conexion a PostgreSQL y latencia aproximada.
- [x] Muestra version/build desplegado, fecha de despliegue y commit si esta disponible.
- [x] Muestra estado de Flyway: version actual y migraciones pendientes/fallidas.
- [x] Muestra ultimo backup valido y ultima prueba de restore si existen. (backup: leido del manifiesto REQ-0065; prueba de restore aparecera cuando exista REQ-0066 — criterio "si existen")
- [x] Muestra espacio libre en disco y alertas por umbrales configurables.
- [x] Muestra estado basico de WildFly y datasource. (memoria JVM + uptime; salud del datasource reflejada por el indicador de Base de datos)
- [x] Muestra semaforos claros: OK, Advertencia, Critico.
- [x] No expone secretos, rutas sensibles completas ni credenciales a usuarios sin permiso tecnico.

## Reglas De Negocio

- Debe ayudar a soporte a responder rapidamente si el sistema esta sano.
- Debe ser de solo lectura salvo acciones explicitamente aprobadas en REQ futuros.
- Las alertas criticas deben quedar registradas como evento operativo.

## Dependencias

- Depende de: REQ-0032, REQ-0048, REQ-0049.
- Requerido por: soporte, monitoreo y operacion comercial.

## Fuentes Y Trazabilidad

- Decision usuario 2026-07-11: funcionalidades vendibles y confiabilidad de base/servidor.
