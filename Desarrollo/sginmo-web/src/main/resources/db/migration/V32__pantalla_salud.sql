-- REQ-0051: registra la pantalla "salud" (Salud del sistema) en el catalogo de pantallas.
-- Corre despues de V28 (RLS): la tabla 'entidad' tiene RLS forzada, asi que fijamos
-- app.tenant=-1 (SET LOCAL) para poder insertar la fila GLOBAL (tenant -1).
-- El perfil ADMINISTRADOR ve la pantalla sin sembrar permisos (puede() cortocircuita);
-- otros usuarios/grupos requieren permiso explicito 'salud:VER' desde la UI de permisos.
SELECT set_config('app.tenant', '-1', true);

INSERT INTO entidad (lista, codigo, descripcion, tenant, estado, usuario_creacion, fecha_creacion)
  SELECT 'PANTALLAS', 'salud', 'Salud del sistema', -1, 'ACTIVO', 'sistema', now()
  WHERE NOT EXISTS (
      SELECT 1 FROM entidad e WHERE e.lista='PANTALLAS' AND e.codigo='salud' AND e.tenant=-1);
