-- REQ-0062: registra la pantalla "reportes" (reportes exportables PDF/CSV, sin Jasper).
SELECT set_config('app.tenant', '-1', true);

INSERT INTO entidad (lista, codigo, tenant, descripcion, usuario_creacion, fecha_creacion)
VALUES ('PANTALLAS', 'reportes', -1, 'Reportes exportables', 'sistema', now())
ON CONFLICT (lista, codigo, tenant) DO NOTHING;
