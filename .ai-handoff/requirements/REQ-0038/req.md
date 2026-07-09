# REQ-0038 - Multiempresa F6: SUPERADMIN, alta de empresas y aislamiento en UI

**Estado:** implementado; `mvn package` verde; xhtml bien formados (2026-07-09)

## Objetivo Funcional
Cerrar la experiencia multiempresa sobre las capas F1-F5:
- **Alta de empresa como UNIDAD (SUPERADMIN):** en una sola transaccion crea la persona
  juridica con rol EMPRESA (en -1), sus datos comerciales, la sucursal por defecto y el
  usuario ADMINISTRADOR inicial (tenant = la nueva empresa, con cambio de clave forzado).
  Disponible desde la pantalla de Empresas cuando el SUPERADMIN crea una empresa nueva.
- **ADMINISTRADOR aislado:** ABM de usuarios, grupos y catalogos SOLO de su tenant; los
  grupos plantilla -1 son visibles y asignables pero no editables (solo el SUPERADMIN los toca).
- **Selector de tenant de soporte:** el SUPERADMIN puede "operar como" una empresa; mientras
  dura, TODO el sistema (services + RLS via el interceptor F5) queda acotado a ese tenant;
  "Global" restaura su vision total.
- **tenant invisible en UI:** el discriminador no se muestra ni se edita en ninguna pantalla.

## Criterios De Aceptacion
- [x] Alta de empresa provisiona pj+rol+sucursal+usuario admin atomica (rollback si algo falla).
- [x] Un ADMINISTRADOR no ve ni edita usuarios/grupos/catalogos de otra empresa (lecturas Y
      escrituras, incluidas asignaciones de grupo/permiso; usuario/grupo estan fuera de RLS).
- [x] Las plantillas de grupo -1 son de solo lectura para el ADMINISTRADOR y asignables a sus usuarios.
- [x] El SUPERADMIN puede cambiar el tenant activo (soporte) y el contexto entero lo respeta.
- [x] tenant nunca aparece como campo en la UI.

## Bloqueo Formal Documentado
La validacion runtime (render JSF, login real de 2 empresas, aislamiento efectivo con RLS
activa) es la verificacion integral F7 (REQ-0039), tras aplicar V26+V27+V28 como unidad.
Este REQ se evidencia con build verde + XML bien formado + analisis de aislamiento.
