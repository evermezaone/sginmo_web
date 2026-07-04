# REQ-0001 - Plan De Pruebas

**Fecha:** 2026-07-04

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn.cmd -q package` en `Desarrollo\sginmo-web` (JAVA_HOME=jdk-23, MAVEN_OPTS truststore Windows-ROOT) | EXIT:0 y `target/sginmo-web.war` generado | **OK** — EXIT:0, WAR 3,9 MB |
| T02 | Arrancar WildFly 40.0.0.Final con `JBOSS_HOME` del proyecto y WAR en `standalone\deployments` | aparece `sginmo-web.war.deployed` sin `.failed` | **OK** — marker `.deployed` presente |
| T03 | `GET http://localhost:8080/sginmo-web/` | HTTP 200; HTML contiene "SGInmo Web", recursos `primefaces` y "WildFly 40" | **OK** — 200, 3600 bytes, las 3 verificaciones true |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | AJAX JSF+PrimeFaces | Abrir la pagina y pulsar "Probar AJAX (JSF + PrimeFaces)" | El panel `resultado` muestra "Render 0.1.0-SNAPSHOT — AJAX OK a las HH:mm:ss" sin recarga completa | pendiente (usuario) — el boton existe en `index.xhtml` y el bean expone `hora` |

## Datos De Prueba

No aplica (sin persistencia en este REQ).

## Incidencias detectadas y resueltas durante la prueba

1. PKIX (TLS interceptado en la red) al bajar dependencias → resuelto con `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`.
2. `standalone.bat` arranco un WildFly 8 ajeno por `JBOSS_HOME` global de la estacion → resuelto fijando `JBOSS_HOME` al WildFly 40 del proyecto (documentado en analysis/implementation).
