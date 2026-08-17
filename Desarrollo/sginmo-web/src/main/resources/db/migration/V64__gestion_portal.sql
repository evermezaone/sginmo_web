-- ============================================================================
-- V64 - Pantalla interna "Gestion del portal": hub del back-office del portal
-- (reclamos + conciliaciones). Usuario del login de sginmo-web con permiso, NO el login del cliente.
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

INSERT INTO entidad (lista, codigo, tenant, descripcion, usuario_creacion, fecha_creacion)
VALUES ('PANTALLAS', 'gestion_portal', -1, 'Gestion del portal (reclamos y conciliaciones)', 'sistema', now())
ON CONFLICT (lista, codigo, tenant) DO NOTHING;
