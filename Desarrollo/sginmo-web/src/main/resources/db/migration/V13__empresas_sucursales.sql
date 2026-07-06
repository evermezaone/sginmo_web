-- V13 (REQ-0009): empresas y sucursales. IDEMPOTENTE (obs 208 de Codex): reproducible
-- sobre BD parcialmente migrada / re-ejecucion.
-- sucursal: le faltaba estado (regla 13: toda tabla maestra tiene baja logica).
ALTER TABLE sucursal ADD COLUMN IF NOT EXISTS estado varchar(10) NOT NULL DEFAULT 'ACTIVO'
  CHECK (estado IN ('ACTIVO','INACTIVO'));

INSERT INTO entidad (entidad, codigo, descripcion, usuario_creacion, fecha_creacion)
SELECT 'PANTALLAS', 'empresas', 'Empresas y sucursales', 'sistema', now()
WHERE NOT EXISTS (SELECT 1 FROM entidad e WHERE e.entidad = 'PANTALLAS' AND e.codigo = 'empresas');

-- La empresa placeholder del seed V5 recibe su rol EMPRESA (regla: empresa del sistema =
-- persona juridica con rol EMPRESA activo) y una casa matriz para el selector de contexto.
INSERT INTO persona_rol (persona, rol_codigo, usuario_creacion, fecha_creacion)
SELECT persona, 'EMPRESA', 'sistema', now() FROM persona WHERE numero_documento = '80000000'
  AND NOT EXISTS (SELECT 1 FROM persona_rol pr WHERE pr.persona = persona.persona AND pr.rol_codigo = 'EMPRESA');

INSERT INTO sucursal (persona_juridica, descripcion, direccion, telefono, por_defecto, usuario_creacion, fecha_creacion)
SELECT persona, 'Casa matriz', 'Asunción', '-', true, 'sistema', now()
FROM persona WHERE numero_documento = '80000000'
  AND NOT EXISTS (SELECT 1 FROM sucursal s WHERE s.persona_juridica = persona.persona);
