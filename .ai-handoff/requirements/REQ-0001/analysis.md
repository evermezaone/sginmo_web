# REQ-0001 - Analisis

**Estado:** EN_ANALISIS
**Fecha:** 2026-07-04
**Analista:** Implementador (Claude)

## Analisis Funcional

Base del proyecto web: sin este esqueleto no hay donde implementar ningun modulo. Debe probar el ciclo completo build → deploy → render en el stack decidido (doc 06): Maven WAR + WildFly 40 (Jakarta EE 11) + JSF/Faces 4.1 + PrimeFaces 15 sobre Java 21.

## Analisis Tecnico

- Proyecto Maven WAR `sginmo-web` en `Desarrollo\sginmo-web\`.
- Dependencias: `jakarta.jakartaee-api:11.0.0` (provided, las implementa WildFly 40), `org.primefaces:primefaces:15.0.0` classifier `jakarta` (va dentro del WAR), JUnit 5 para tests futuros.
- `web.xml` (FacesServlet + PROJECT_STAGE Development + tema saga + session config), `beans.xml` (CDI discovery all), `faces-config.xml` (locale es).
- Bean CDI `InicioBean` (@Named @RequestScoped) + `index.xhtml` con `p:panel`/`p:commandButton` AJAX para probar CDI+JSF+PF juntos.
- WildFly 40.0.0.Final portable en `herramientas\` (fuera de git).

### Ajuste de alcance respecto del titulo del backlog

PostgreSQL/datasource/Flyway se activan en REQ-0003: un `persistence.xml` con `jta-data-source` inexistente rompe el deploy del esqueleto. Este REQ deja pom/estructura listos para agregarlos.

### Particularidades del entorno detectadas (documentadas para repetibilidad)

1. La red tiene interceptacion TLS: Maven falla con PKIX salvo `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (usa el almacen de certificados de Windows).
2. La estacion tiene `JBOSS_HOME` global apuntando a un WildFly 8 viejo (`Instaladores\JBOSS`); `standalone.bat` lo respeta, asi que SIEMPRE fijar `JBOSS_HOME` al WildFly 40 del proyecto antes de arrancar.

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| Version PrimeFaces 15 + Faces 4.1 incompatibles | baja | medio | classifier jakarta oficial; probado con render y AJAX |
| Arrancar el WildFly equivocado por JBOSS_HOME global | media | medio | fijar JBOSS_HOME por comando (documentado); considerar script de arranque propio |
| OneDrive lockea .git/target durante builds | media | bajo | reintentar; target/ esta gitignoreado |

**Semaforo Codex:** medio (configuracion base de todo el proyecto, sin datos ni reglas de negocio)

## Preguntas Abiertas

- [x] Ninguna

## Impacto En Costos / LLM

- Aumenta tokens por mensaje: no
- Agrega llamadas extra al LLM: no
- Puede resolverse sin IA: si

## Impacto En Datos

- Requiere migracion: no
- Tablas/colecciones afectadas: ninguna

## Recomendacion

**Desarrollar**
