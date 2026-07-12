-- ============================================================================
-- V41 - Parametrizacion avanzada por empresa (REQ-0060)
-- Extiende parametro_sistema (clave/valor por tenant; -1 = default global) con
-- metadatos: tipo de dato, grupo y valor por defecto. Siembra los parametros
-- iniciales documentados como defaults globales (tenant=-1).
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

ALTER TABLE parametro_sistema
  ADD COLUMN IF NOT EXISTS tipo          varchar(12) NOT NULL DEFAULT 'STRING'
    CHECK (tipo IN ('STRING','ENTERO','DECIMAL','BOOLEAN','FECHA')),
  ADD COLUMN IF NOT EXISTS grupo         varchar(40),
  ADD COLUMN IF NOT EXISTS valor_defecto varchar(120);

-- Parametros iniciales (globales, tenant=-1). Cada empresa puede sobrescribir su valor.
INSERT INTO parametro_sistema (tenant, clave, valor, descripcion, tipo, grupo, valor_defecto, usuario_creacion, fecha_creacion)
  SELECT -1, v.clave, v.valor, v.descripcion, v.tipo, v.grupo, v.valor, 'sistema', now()
  FROM (VALUES
    ('MORA_DIAS_GRACIA',     '5',    'Dias de gracia antes de aplicar mora',        'ENTERO',  'Cobranza'),
    ('MORA_MONTO_DIA',       '0',    'Monto de mora por dia (si aplica)',           'DECIMAL', 'Cobranza'),
    ('CAJA_OBLIGATORIA',     'false','Exigir planilla de caja abierta para cobrar', 'BOOLEAN', 'Caja'),
    ('CONTRATO_DIAS_ALERTA', '30',   'Dias de anticipacion para alertar vencimiento de contrato', 'ENTERO', 'Agenda'),
    ('AGENDA_DIAS_ALERTA',   '30',   'Ventana (dias) para generar vencimientos en la agenda',     'ENTERO', 'Agenda'),
    ('EXPORT_LIMITE_FILAS',  '1000', 'Limite de filas para exportaciones',          'ENTERO',  'Reportes'),
    ('COMPROBANTE_PIE',      '',     'Texto de pie en comprobantes/recibos',        'STRING',  'Comprobantes'),
    ('EMPRESA_LOGO',         '',     'Ruta/URL del logo de la empresa',             'STRING',  'General'),
    ('DOCUMENTO_POLITICA',   '',     'Politica documental (texto)',                 'STRING',  'Documentos')
  ) AS v(clave, valor, descripcion, tipo, grupo)
  WHERE NOT EXISTS (
    SELECT 1 FROM parametro_sistema p WHERE p.clave = v.clave AND p.tenant = -1);

-- Completa metadatos de parametros ya existentes (sin grupo) para agruparlos en la UI.
UPDATE parametro_sistema SET grupo = COALESCE(grupo, 'General'),
       valor_defecto = COALESCE(valor_defecto, valor)
 WHERE grupo IS NULL;
