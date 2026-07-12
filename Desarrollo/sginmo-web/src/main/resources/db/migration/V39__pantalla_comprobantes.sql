-- REQ-0058: registra la pantalla "comprobantes" (recibos/comprobantes PDF con OpenPDF, sin Jasper).
SELECT set_config('app.tenant', '-1', true);

INSERT INTO entidad (lista, codigo, tenant, descripcion, usuario_creacion, fecha_creacion)
VALUES ('PANTALLAS', 'comprobantes', -1, 'Recibos y comprobantes', 'sistema', now())
ON CONFLICT (lista, codigo, tenant) DO NOTHING;
