# Codex Review - REQ-0015

Fecha: 2026-07-07
Auditor: codex
Resultado: REQUIERE_CAMBIOS

## Verificacion

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md` y `preaudit-checklist.md`.
- Revisados:
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ActivoBean.java`
  - `Desarrollo/sginmo-web/src/main/webapp/activos.xhtml`

Confirmado correcto:

- `ActivoService.generarLotes()` es `@Transactional`.
- Exige permiso backend `CREAR`.
- Valida contenedor no nulo y cantidad entre 1 y 500.
- Crea lotes hijos de `contenedorId`.
- Hereda `empresa` y `ubicacion` del contenedor.
- Setea `tipoCodigo = LOTE`, `numeroLote`, `numeroManzana`, precio y comision.
- La UI expone dialogo con contenedor, manzana, desde, cantidad, precio y comision.

## Hallazgos Bloqueantes

### Obs 214 - La generacion acepta cualquier activo como contenedor, no solo loteamientos

Problema: El REQ define "crear N lotes hijos de un loteamiento contenedor", pero `ActivoService.generarLotes()` solo valida que el contenedor exista. `ActivoBean.completarLoteamiento()` llama a `activoService.buscarContenedor(texto, null)`, que busca cualquier activo por nombre, sin filtrar tipo `LOTEAMIENTO` ni tipos equivalentes aprobados.

Impacto: Un usuario puede generar lotes debajo de una casa, departamento, oficina u otro activo no contenedor. Eso contamina la jerarquia `activo`, afecta operaciones/reportes por contenedor y contradice la regla funcional de loteamiento > lotes.

Solucion esperada: Filtrar el autocomplete de loteamientos y validar en backend que `contenedorId` corresponda a un tipo permitido como loteamiento/contenedor de lotes. Si se permite mas de un tipo contenedor, dejar la lista explicita y parametrizable.

Evidencia:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:219`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ActivoBean.java:156`
- `Desarrollo/sginmo-web/src/main/webapp/activos.xhtml:64`

### Obs 215 - La deteccion de duplicados omite lotes validos cuando la manzana esta vacia

Problema: Para detectar duplicados se usa `AND (:m IS NULL OR a.numeroManzana = :m)`. Cuando `manzana` esta vacia, el parametro `:m` se envia como `null`, y la condicion queda verdadera para cualquier manzana. Asi, si ya existe el lote 1 en manzana A, una generacion sin manzana omite el lote 1 aunque no exista un lote 1 sin manzana.

Impacto: La regla del REQ dice omitir duplicados por mismo contenedor/manzana. La implementacion actual omite de mas en generaciones sin manzana y puede crear resultados incompletos sin avisar claramente al usuario.

Solucion esperada: Normalizar `manzana` una vez y comparar por igualdad semantica de manzana, distinguiendo null/vacia de manzanas reales. Ejemplo: `((:m IS NULL AND a.numeroManzana IS NULL) OR a.numeroManzana = :m)`, o normalizar a string vacio de forma consistente en guardado y consulta.

Evidencia:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:227`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:231`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:242`

## Pruebas

- Revision estatica de service, bean y XHTML.
- No se ejecuto build de cierre porque el REQ queda bloqueado por inspeccion funcional.
