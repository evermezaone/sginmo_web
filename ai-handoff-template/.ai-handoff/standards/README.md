# Estandares De Desarrollo - Gestion ONE Web

Estos estandares consolidan reglas detectadas durante la auditoria Codex de la migracion VB6 + Oracle a FastAPI + React + MariaDB.

Objetivo: que cada nuevo REQ se implemente con criterios consistentes, verificables y alineados con la logica heredada del sistema VB6.

## Uso obligatorio

- Claude debe leer esta carpeta antes de implementar REQs nuevos o corregir REQs existentes.
- Codex debe usar estos documentos como checklist de auditoria.
- Si un REQ necesita apartarse de un estandar, debe justificarlo en `analysis.md` y `claude-implementation.md`.
- Si una regla de negocio proviene de VB6, debe citar el formulario `.frm`, modulo `.bas` o procedimiento SQL usado como fuente.

## Documentos

- `backend-api.md`: reglas para FastAPI, validaciones, permisos y contratos.
- `frontend-ui.md`: reglas para React, rutas, React Query, Ant Design y flujos UI.
- `database.md`: reglas para nombres de tablas/columnas, migraciones y compatibilidad MariaDB.
- `security-permissions.md`: reglas de autenticacion, admin, permisos por grupo/usuario.
- `source-traceability.md`: como documentar reglas contra una fuente de verdad, exista o no codigo legado.
- `vb6-business-rules.md`: guia especifica para sistemas VB6 heredados; usar solo cuando aplique.
- `audit-checklist.md`: checklist minimo antes de enviar a Codex.
- `workflow-priority.md`: regla transversal para cerrar primero el menor REQ pendiente y aplicar aprendizajes a los REQs restantes.

## Principios base

1. Backend manda en reglas de negocio. La UI puede ayudar, pero no reemplaza validaciones backend.
2. Las rutas declaradas en UI deben existir en `App.tsx` y menu.
3. Los nombres de tablas/columnas deben cotejarse contra schema real antes de crear SQL.
4. Los permisos deben proteger backend, no solo esconder botones.
5. Todo cambio que toque datos, precios, numeracion, permisos, cobros o documentos requiere revision estricta.
6. Las reglas criticas deben trazarse contra una fuente de verdad: codigo, SQL, documentacion, sistema productivo o decision explicita del usuario.
7. El flujo debe cerrar primero el menor REQ pendiente. No saltar a REQs mayores salvo excepcion explicita y documentada.
8. Todo hallazgo repetible debe transformarse en estandar y aplicarse a los REQs pendientes antes de reenviarlos.
