-- V5 (REQ-0004): parametros de login, empresa propietaria y usuario administrador inicial.
-- La contrasena inicial se comunica por canal privado y debe cambiarse cuando exista
-- el ABM de usuarios (cambio de contrasena).

INSERT INTO parametro_sistema (clave, valor, descripcion, usuario_creacion, fecha_creacion) VALUES
  ('LOGIN_MAX_INTENTOS',    '5',  'Intentos fallidos de login antes de bloquear al usuario', 'sistema', now()),
  ('LOGIN_BLOQUEO_MINUTOS', '15', 'Minutos de bloqueo tras superar los intentos fallidos',   'sistema', now())
ON CONFLICT (clave) DO NOTHING;

-- Empresa propietaria del sistema (persona juridica placeholder: se edita en el ABM de personas)
INSERT INTO persona (tipo_personeria, nombre, numero_documento, es_contribuyente, usuario_creacion, fecha_creacion)
VALUES ('JURIDICA', 'Pysistemas', '80000000', true, 'sistema', now());

INSERT INTO persona_juridica (persona, razon_social, usuario_creacion, fecha_creacion)
SELECT persona, 'Pysistemas', 'sistema', now()
FROM persona WHERE numero_documento = '80000000';

-- Usuario administrador inicial (hash bcrypt costo 10)
INSERT INTO usuario (codigo_usuario, password_hash, perfil, empresa, usuario_creacion, fecha_creacion)
SELECT 'admin', '$2a$10$fS0uA42ULHhVlT3f2nJGducqdMrklKZJRB1q2bS8bFMFELzsCXGou', 'ADMINISTRADOR', persona, 'sistema', now()
FROM persona WHERE numero_documento = '80000000';
