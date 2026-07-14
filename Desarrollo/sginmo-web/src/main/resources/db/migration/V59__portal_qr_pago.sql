-- ============================================================================
-- V59 - Portal: parametros del pago por QR (REQ-0093, Fase 1 - QR EMVCo estatico)
-- Siembra como defaults globales (tenant=-1) los parametros del QR de pago. El QR
-- queda DESHABILITADO hasta que la empresa cargue su cuenta destino y lo active.
-- La empresa puede sobrescribir cualquiera de estos valores desde Parametros.
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

INSERT INTO parametro_sistema (tenant, clave, valor, descripcion, tipo, grupo, valor_defecto, usuario_creacion, fecha_creacion)
  SELECT -1, v.clave, v.valor, v.descripcion, v.tipo, v.grupo, v.valor, 'sistema', now()
  FROM (VALUES
    ('PORTAL_QR_HABILITADO', 'false',     'Habilita el pago por QR en el portal del socio',                'BOOLEAN', 'Portal QR'),
    ('PORTAL_QR_GUI',        '',          'QR: identificador del esquema/banco (tag 26 sub 00, dato del banco/SIPAP)', 'STRING', 'Portal QR'),
    ('PORTAL_QR_CUENTA',     '',          'QR: cuenta/alias del comercio para recibir (tag 26 sub 01)',   'STRING', 'Portal QR'),
    ('PORTAL_QR_MERCHANT',   '',          'QR: nombre del comercio (tag 59)',                              'STRING', 'Portal QR'),
    ('PORTAL_QR_CIUDAD',     'Asuncion',  'QR: ciudad del comercio (tag 60)',                              'STRING', 'Portal QR'),
    ('PORTAL_QR_MCC',        '0000',      'QR: Merchant Category Code (tag 52)',                           'STRING', 'Portal QR'),
    ('PORTAL_QR_MONEDA',     '600',       'QR: moneda ISO 4217 (tag 53; 600 = PYG)',                       'STRING', 'Portal QR'),
    ('PORTAL_QR_PAIS',       'PY',        'QR: pais ISO 3166-1 alfa-2 (tag 58)',                           'STRING', 'Portal QR')
  ) AS v(clave, valor, descripcion, tipo, grupo)
  WHERE NOT EXISTS (
    SELECT 1 FROM parametro_sistema p WHERE p.clave = v.clave AND p.tenant = -1);
