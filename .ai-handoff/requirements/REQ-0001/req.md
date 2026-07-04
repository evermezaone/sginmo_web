# REQ-0001 - Esqueleto Maven + WildFly 40 + PostgreSQL + Flyway

**Numero:** REQ-0001
**Fecha de creacion:** 2026-07-04
**Estado inicial:** NUEVO
**Prioridad:** alta (base de todo el desarrollo)

## Texto Original

Backlog doc 08, Fase 1: "Esqueleto del proyecto: Maven WAR + WildFly 40 + datasource PostgreSQL + Flyway operativo — compila `mvn package` y deploya página base".

## Objetivo Funcional

Proyecto `sginmo-web` compilable y deployable: un desarrollador (o la VPS) puede construir el WAR con Maven y verlo corriendo en WildFly 40 con una página JSF/PrimeFaces renderizando.

## Alcance ajustado (documentado en analysis.md)

La provisión de PostgreSQL y la activación de Flyway/datasource se hacen en REQ-0003 (esquema inicial), porque un `persistence.xml` apuntando a un datasource inexistente rompería el deploy de este esqueleto. Este REQ deja la estructura y las dependencias listas.

## Criterios De Aceptacion

- [x] Proyecto Maven WAR en `Desarrollo\sginmo-web\` con estructura de paquetes `py.com.pysistemas.sginmo` (dominio/servicio/web) y `src/main/webapp`.
- [x] `pom.xml`: Java 21 (`--release 21`), Jakarta EE 11 api (provided), PrimeFaces 15 (classifier jakarta), war plugin; `mvn -q package` termina EXIT:0.
- [x] WildFly 40.0.0.Final instalado localmente (portable, en `herramientas/`), arranca sin errores.
- [x] El WAR deploya en WildFly 40 y `http://localhost:8080/sginmo-web/` renderiza una página JSF con componente PrimeFaces (recursos PF cargan).
- [x] `web.xml` (FacesServlet, welcome-file, PROJECT_STAGE), `beans.xml` (CDI) y `faces-config.xml` presentes y válidos.
- [x] Sin credenciales ni secretos en el código.

## Dependencias

- Depende de: REQ-0000 (git — parte git ya completada).
- Requerido por: todos los REQs siguientes.
