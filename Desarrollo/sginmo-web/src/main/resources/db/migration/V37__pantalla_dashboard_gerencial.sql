-- REQ-0056: registra la pantalla "dashboard-gerencial" (KPIs gerenciales, solo lectura).
-- La tabla 'entidad' tiene RLS (V28): se fija app.tenant=-1 para insertar la fila global.
SELECT set_config('app.tenant', '-1', true);

INSERT INTO entidad (lista, codigo, tenant, descripcion, usuario_creacion, fecha_creacion)
VALUES ('PANTALLAS', 'dashboard-gerencial', -1, 'Dashboard gerencial', 'sistema', now())
ON CONFLICT (lista, codigo, tenant) DO NOTHING;
