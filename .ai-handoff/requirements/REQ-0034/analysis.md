# REQ-0034 F2 — Análisis de alcance (entidades JPA adaptadas al V26)

Relevamiento hecho el 2026-07-09 sobre `Desarrollo/sginmo-web` y `Desarrollo/onesystem-security`.
Insumo para desarrollar F2. El esquema destino está en `docs-migracion/14-esquema-multiempresa.sql`
y la migración en `V26__multiempresa_esquema.sql`.

## Módulos y paquetes
- Dominio de negocio: `py.com.pysistemas.sginmo.dominio.{catalogo,activo,operacion,persona}` (+ `.servicio`, `.web`).
- **Seguridad: módulo propio `Desarrollo/onesystem-security`** (dependencia maven `py.com.one:onesystem-security`) —
  MODIFICABLE. Mapea usuario, grupo, permiso_grupo, permiso_usuario, preferencia_usuario, usuario_grupo.

## Cambio 1 — RENAME empresa→tenant (repercute en 3 capas)
- **onesystem-security** `Usuario.java` L39-40: `@Column(name="empresa") Long empresa` → `tenant`.
  Getters `getEmpresa/setEmpresa` → `getTenant/setTenant`. Ripple de `getEmpresa()`:
  `ContextoEmpresa.cargar()` (`sesion.getUsuario().getEmpresa()`), `SeguridadService`/login (asigna empresa),
  cualquier query de usuario por empresa. Buscar TODOS los `.getEmpresa()` sobre Usuario.
- **dominio** entidades con `@Column(name="empresa")`: `Operacion` (L66, NOT NULL), `Activo` (L37, nullable→
  NOT NULL en BD), `Planilla` (L25), `IngresoEgreso` (L57). Renombrar campo+getters+setters+JPQL.
- **services** que filtran `empresa`: `IngresoEgresoService` (`ie.empresa=:emp`), `LiquidacionService`
  (`o.empresa=:emp` join Operacion), `CajaService` (`p.empresa=:emp`), `OperacionService` (valida),
  `ActivoService` (`setEmpresa`). El parámetro `Long empresaContexto` se pasa desde los beans web
  (`ContextoEmpresa.getEmpresa().getId()`).

## Cambio 2 — Entidad (catálogo): PK compuesta String → PK numérica
- `catalogo/Entidad.java` hoy: `@IdClass(EntidadId.class)`, `@Id entidad String`, `@Id codigo String`.
- V26: PK numérica `entidad bigint`, columna vieja `entidad`→`lista`, +`tenant`, UNIQUE(lista,codigo,tenant).
- Cambiar a: `@Id @GeneratedValue Long entidad`; `String lista`; `String codigo`; `Long tenant`.
  ELIMINAR `EntidadId` y `@IdClass`. Ripple: todo lookup por `new EntidadId(...)` / `em.find(Entidad.class, id)`.

## Cambio 3 — Referencias de catálogo: pares *Lista/*Codigo (String) → id (Long, FK a Entidad)
NO hay @Embeddable; son campos String sueltos. Por entidad JPA:
- `Persona`: tipoDocumentoLista/Codigo → `tipoDocumento Long`.
- `Operacion`: tipoContratoLista/Codigo, tipoFinanciacionLista/Codigo → `tipoContrato Long`, `tipoFinanciacion Long`.
- `Activo`: `tipoCodigo` → `tipo Long`.
- `IngresoEgreso`: `tipoImputacionCodigo` → `tipoImputacion Long`.
- `Liquidacion`: `motivoCodigo` → `motivo Long`.
- `Articulo`: categoria, unidad_medida, presentacion, marca, modelo, familia, grupo, subgrupo, procedencia → Long.
- `ArticuloPropiedad`: propiedad, referencia → Long.
- `UbicacionGeografica`: nivel → Long.
- `PersonaJuridica`: actividadLista/Codigo → MUEVE a PersonaEmpresa (ver cambio 4).
- `PersonaFisica`: estadoCivilCodigo → MUEVE a PersonaEmpresa.
Los combos/listas de la UI que hoy resuelven por (lista,codigo) pasan a resolver el id por
`(lista, codigo, tenant IN(-1,:t))` — eso es lógica de service (F4), pero el tipo de campo cambia aquí.

## Cambio 4 — persona reducida a identidad; NUEVA PersonaEmpresa (por tenant)
- Quitar de `Persona`: esContribuyente, clasificacionFiscal, direccion, telefono, email, ubicacion,
  ubicacionUrl, observacion. De `PersonaFisica`: estadoCivil, nacionalidad. De `PersonaJuridica`:
  nombreFantasia, representanteLegal, actividad.
- Crear entidad `PersonaEmpresa` (tabla persona_empresa) con esos campos + persona + tenant + estado + auditoría.
- Ripple: `PersonaService`, `EmpresaService`, beans JSF de personas/empresa, ABM de personas (la cartera pasa
  a ser por tenant vía persona_empresa; la identidad global se reutiliza por numero_documento).

## Cambio 5 — perfil SUPERADMIN
- `Usuario.perfil` admite 'SUPERADMIN' (el CHECK ya lo permite en V26). Ajustar enum/validaciones si las hay.

## Tablas SIN entidad JPA (native SQL) → NO son F2, son F3 (REQ-0035)
- `documento`, `rango_comprobante`, `cobro`, `anulacion`, `rescision` se crean/leen por SQL nativo en
  `OperacionService`/`CajaService` y por los SP del motor. Ahí: empresa→tenant, tipo_codigo→tipo directo,
  documento.tenant, y resolver catálogo por id. Se hace en F3 junto con los SP.

## Orden sugerido de F2
1. onesystem-security: Usuario (empresa→tenant, SUPERADMIN), Grupo (+tenant). Ajustar login/ContextoEmpresa.
2. catalogo: Entidad (PK numérica), Articulo/ArticuloPropiedad/UbicacionGeografica/otros (refs→Long, +tenant).
3. persona: Persona/PersonaFisica/PersonaJuridica (quitar comerciales) + nueva PersonaEmpresa + PersonaRol(+tenant, rol→Long).
4. operacion/activo: Operacion/Activo/Planilla/IngresoEgreso/Liquidacion (empresa→tenant, refs→Long).
5. Compilar (mvn portable). F2 debe compilar como unidad; el aislamiento por tenant real es F4.
NOTA: no deployar hasta que F3 (native SQL + SP) también esté hecho — V26+F2+F3 van juntos.
