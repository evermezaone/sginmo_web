-- ============================================================================
-- V35 - Firma y estado documental de documentos generados (REQ-0054)
-- Extiende documento_generado (REQ-0041, ya con RLS V29) con un estado operativo
-- independiente del archivo fisico, fechas del ciclo, version firmada y anulacion.
-- ============================================================================

-- Contexto SUPERADMIN para registrar la pantalla global (la tabla ya tiene RLS; el
-- ALTER es DDL y no depende de RLS, pero el INSERT en entidad si).
SELECT set_config('app.tenant', '-1', true);

ALTER TABLE documento_generado
  ADD COLUMN IF NOT EXISTS estado_documental varchar(12) NOT NULL DEFAULT 'GENERADO'
    CHECK (estado_documental IN ('GENERADO','IMPRESO','ENVIADO','FIRMADO','OBSERVADO','ANULADO','ARCHIVADO')),
  ADD COLUMN IF NOT EXISTS fecha_impresion timestamptz,
  ADD COLUMN IF NOT EXISTS fecha_envio     timestamptz,
  ADD COLUMN IF NOT EXISTS fecha_firma     timestamptz,
  ADD COLUMN IF NOT EXISTS fecha_archivo   timestamptz,
  ADD COLUMN IF NOT EXISTS adjunto_firmado bigint REFERENCES documento_adjunto(documento_adjunto),
  ADD COLUMN IF NOT EXISTS motivo_anulacion  varchar(250),
  ADD COLUMN IF NOT EXISTS usuario_anulacion varchar(20),
  ADD COLUMN IF NOT EXISTS fecha_anulacion   timestamptz;

CREATE INDEX IF NOT EXISTS ix_documento_generado_estado
  ON documento_generado (tenant, estado_documental);

INSERT INTO entidad (lista, codigo, tenant, descripcion, usuario_creacion, fecha_creacion)
VALUES ('PANTALLAS', 'documentos-generados', -1, 'Documentos generados', 'sistema', now())
ON CONFLICT (lista, codigo, tenant) DO NOTHING;
