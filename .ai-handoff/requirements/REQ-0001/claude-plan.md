# REQ-0001 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-04

## Estrategia

1. Crear proyecto Maven WAR con pom minimo (Java 21, EE 11 api provided, PrimeFaces 15 jakarta).
2. Descriptores: web.xml, beans.xml, faces-config.xml.
3. Bean CDI + pagina index.xhtml con componentes PrimeFaces y boton AJAX.
4. Descargar WildFly 40.0.0.Final portable a `herramientas\`.
5. Build `mvn -q package` → deploy por scanner (`standalone\deployments`) → verificacion HTTP.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| `Desarrollo/sginmo-web/pom.xml` | nuevo |
| `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/web.xml` | nuevo |
| `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/beans.xml` | nuevo |
| `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/faces-config.xml` | nuevo |
| `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/InicioBean.java` | nuevo |
| `Desarrollo/sginmo-web/src/main/webapp/index.xhtml` | nuevo |

## Pruebas Previstas

- [x] `mvn -q package` EXIT:0 y genera `target/sginmo-web.war`
- [x] WildFly 40 arranca y aparece `sginmo-web.war.deployed`
- [x] GET `http://localhost:8080/sginmo-web/` → 200 con contenido esperado

## Riesgos

Ver analysis.md (TLS interceptado, JBOSS_HOME global).

## Cambios De Datos

Sin cambios.
