# REQ-0034 - Multiempresa F2: entidades JPA adaptadas al esquema V26

**Estado:** implementado; `mvn package` del WAR completo en verde (2026-07-09)

## Objetivo Funcional
Adaptar TODA la capa JPA/DTO/UI al esquema multiempresa V26 (REQ-0033), sin cambiar aun
la logica de aislamiento por tenant (eso es F4). Alcance:
- **empresa -> tenant** en las entidades transaccionales: `Usuario` (onesystem-security),
  `Operacion`, `Activo` (NOT NULL), `Planilla`, `IngresoEgreso`. Ripple en services/beans/JPQL
  y en `ContextoEmpresa`. El objeto de negocio "empresa" (PersonaJuridica) NO cambia.
- **Catalogo `Entidad`**: de PK compuesta String (`@IdClass EntidadId`) a **PK numerica**
  (`entidad bigint`) + `lista`/`codigo`/`tenant`. Se elimina `EntidadId`.
- **Referencias de catalogo por id**: TODOS los pares `*_lista/*_codigo` (String) pasan a UNA
  columna `Long` FK a `entidad` — Activo.tipo, Operacion.tipo_contrato/financiacion,
  IngresoEgreso.tipo_imputacion, Liquidacion.motivo, Persona.tipo_documento, PersonaRol.rol,
  UbicacionGeografica.nivel, Articulo (9), ArticuloPropiedad.propiedad. Resolver id<->codigo en
  `CatalogoService` (idOpcion/codigoOpcion/descripcionOpcion); la logica de negocio por codigo
  (loteamiento, rol EMPRESA) se resuelve con el resolver. Combos: `itemValue` por id.
- **persona reducida a identidad** + NUEVA entidad `PersonaEmpresa`: los datos comerciales
  (direccion, telefono, email, es_contribuyente, clasif. fiscal, ubicacion, observacion,
  estado_civil, nacionalidad, nombre_fantasia, representante_legal, actividad) pasan a
  `persona_empresa` POR TENANT. Los ABMs de personas y empresas atan esos campos a un
  `PersonaEmpresa` del tenant del contexto (upsert por (persona, tenant); la empresa es su
  propio tenant).
- **perfil SUPERADMIN**: el CHECK ya lo admite (V26); `Usuario.perfil` documentado.

## Criterios De Aceptacion
- [x] Todas las entidades JPA reflejan el esquema V26 (0 pares `*_lista/*_codigo`; 0 columnas
      `empresa` fuera de documento; Entidad con id numerico; PersonaEmpresa creada).
- [x] `mvn -pl sginmo-web -am package` del WAR completo compila en verde (EXIT 0).
- [x] Sin bindings JSF muertos a propiedades removidas (verificado por grep).
- [x] La resolucion id<->codigo preserva las reglas de negocio basadas en el codigo.

## Bloqueo Formal Documentado
NO se aplica V26 ni se deploya todavia: falta F3 (REQ-0035, SQL nativo de documento/cobro/
rango/anulacion + SP del motor). V26+F2+F3 se despliegan como unidad. El aislamiento por
tenant real (filtros tenant IN(-1,:t) / =:t, pertenencia) es F4 (REQ-0036). La app desplegada
sigue en V25 y no se toca.
