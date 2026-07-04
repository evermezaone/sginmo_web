# REQ-0001 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-04
**Rama:** main (commit inicial bec5aa2 + commit de este REQ)

## Manifiesto Minimo Para Codex

- REQ: REQ-0001
- Tipo de cambio: configuracion
- Riesgo: medio
- Archivos clave:
  - `Desarrollo/sginmo-web/pom.xml`: definicion Maven (Java 21, EE 11 provided, PrimeFaces 15 jakarta, war/surefire plugins)
  - `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/web.xml`: FacesServlet, PROJECT_STAGE, tema PF, session http-only
  - `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/beans.xml`: CDI bean-discovery-mode all
  - `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/faces-config.xml`: locale es
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/InicioBean.java`: bean CDI de la pagina de inicio
  - `Desarrollo/sginmo-web/src/main/webapp/index.xhtml`: pagina JSF con p:panel y p:commandButton AJAX
- Comandos probados:
  - `mvn.cmd -q package` (con `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`, JAVA_HOME=jdk-23): EXIT:0, genera `target/sginmo-web.war` (3,9 MB)
  - Arranque WildFly 40.0.0.Final (`standalone.bat` con `JBOSS_HOME` fijado al del proyecto): deploy scanner genera `sginmo-web.war.deployed`
  - `Invoke-WebRequest http://localhost:8080/sginmo-web/`: Status 200; el HTML contiene "SGInmo Web", recursos `primefaces` y la linea de stack "WildFly 40"
- Cambios de datos: no
- Cambios de entorno: si — WildFly 40.0.0.Final portable en `herramientas\wildfly-40.0.0.Final` (fuera de git); requiere `JBOSS_HOME` explicito al arrancar (hay un JBOSS_HOME global viejo en la estacion); `MAVEN_OPTS` con truststore Windows-ROOT por TLS interceptado en la red
- Impacto LLM/tokens: no
- Decision esperada: aprobar
- Notas para auditor: PostgreSQL/Flyway/datasource se activan en REQ-0003 (alcance ajustado y justificado en req.md/analysis.md — un jta-data-source inexistente romperia este deploy). No hay persistence.xml todavia, es deliberado.

## Resumen Funcional

Existe la aplicacion web base: WAR deployable en WildFly 40 con pagina de inicio JSF/PrimeFaces renderizando y AJAX operativo. Es el punto de partida de todos los modulos.

## Resumen Tecnico

Proyecto Maven WAR nuevo con estructura `py.com.pysistemas.sginmo` y webapp estandar Jakarta EE 11. Sin logica de negocio (correcto para este REQ).

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| pom.xml | nuevo |
| WEB-INF/web.xml, beans.xml, faces-config.xml | nuevos |
| web/InicioBean.java | nuevo |
| index.xhtml | nuevo |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

- `JAVA_HOME=C:\Program Files\Java\jdk-23` (compila con `--release 21`)
- `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (obligatorio en esta estacion)
- `JBOSS_HOME=<workspace>\herramientas\wildfly-40.0.0.Final` (obligatorio al arrancar WildFly)

## Pruebas Ejecutadas

Ver test-plan.md (T01-T03 con resultados reales).

## Pruebas Manuales Sugeridas

1. Abrir `http://localhost:8080/sginmo-web/` en el navegador y pulsar "Probar AJAX": debe aparecer el texto de confirmacion con la hora sin recargar la pagina.

## Riesgos Conocidos

- El servidor WildFly local corre sobre JDK 23 (unico moderno en la estacion); el target de compilacion es 21 y la VPS usara 21 LTS. Sin impacto en el codigo.
