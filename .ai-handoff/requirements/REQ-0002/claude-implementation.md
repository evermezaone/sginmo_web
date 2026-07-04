# REQ-0002 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-04
**Rama:** main

## Manifiesto Minimo Para Codex

- REQ: REQ-0002
- Tipo de cambio: backend
- Riesgo: medio
- Archivos clave:
  - `src/main/java/py/com/pysistemas/sginmo/dominio/base/Auditable.java`: @MappedSuperclass con auditoria (columnas snake_case NOT NULL segun doc 02); setters package-private para que solo el listener escriba
  - `src/main/java/py/com/pysistemas/sginmo/dominio/base/AuditoriaListener.java`: @PrePersist/@PreUpdate; usuario via CDI (UsuarioActual) con fallback "sistema"
  - `src/main/java/py/com/pysistemas/sginmo/dominio/base/UsuarioActual.java`: contrato CDI del usuario autenticado (impl. real en REQ-0004/0005)
  - `src/main/java/py/com/pysistemas/sginmo/dominio/enums/` (20 archivos): estados y tipos con valores EXACTOS del doc 07 §3 y etiqueta legible
  - `src/test/java/py/com/pysistemas/sginmo/dominio/DominioBaseTest.java`: 12 tests que fijan los valores contra la especificacion + comportamiento del listener
- Comandos probados:
  - `mvn.cmd -q package` (MAVEN_OPTS truststore Windows-ROOT, JAVA_HOME jdk-23): EXIT:0
  - Surefire: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` (DominioBaseTest)
- Cambios de datos: no
- Cambios de entorno: no
- Impacto LLM/tokens: no
- Decision esperada: aprobar
- Notas para auditor: (1) los NOMBRES de los enums replican los codigos del legado (PERFIS/PERJUR, SALONES, DOCEX…) a proposito — el ETL REQ-0031 migra por nombre; las etiquetas llevan la forma legible. (2) MOTIVO_LIQUIDACION no se tipifico como enum (un solo valor generico en la BD real); decision documentada en analysis.md. (3) EstadoCuota.CANCELADO conserva la semantica del legado (= pagada) con etiqueta "Pagada".

## Resumen Funcional

Fundacion del dominio: auditoria automatica en toda entidad y tipos de negocio tipados (fin de los strings magicos del legado).

## Resumen Tecnico

`dominio.base` (3 clases) + `dominio.enums` (20 enums) + test de especificacion. Trazabilidad: cada enum cita su dominio fuente en Javadoc.

## Archivos Modificados

Ver manifiesto (24 archivos nuevos, 0 modificados).

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios (aplican las de REQ-0001).

## Pruebas Ejecutadas

Ver test-plan.md.

## Pruebas Manuales Sugeridas

1. Ninguna pantalla nueva; la auditoria se vera funcionando con las primeras entidades persistentes (REQ-0003).

## Riesgos Conocidos

- La resolucion CDI del usuario se valida end-to-end recien cuando exista sesion (REQ-0004/0005); mientras tanto el fallback "sistema" esta cubierto por test.
