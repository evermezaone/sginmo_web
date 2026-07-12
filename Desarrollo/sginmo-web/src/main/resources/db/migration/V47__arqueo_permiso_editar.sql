-- ============================================================================
-- V47 - Obs 263 (REQ-0059): unificar permisos de arqueo a la pantalla arqueo/*.
-- El servicio y la UI ahora exigen arqueo/EDITAR (cierre) y arqueo/EXPORTAR, pero la
-- plantilla CAJA solo sembraba arqueo/VER + arqueo/EXPORTAR -> el perfil Caja podia ver
-- el arqueo pero no cerrarlo. Se agrega arqueo/EDITAR a la plantilla CAJA y se concede a
-- los grupos existentes que ya tienen arqueo/VER (para que el cierre funcione en prod sin
-- re-aplicar la plantilla). La REAPERTURA (arqueo/REACTIVAR) queda como privilegio aparte,
-- NO sembrado en la plantilla base (se concede explicitamente).
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

-- 1) Plantilla base CAJA: agregar arqueo/EDITAR (idempotente).
INSERT INTO rol_plantilla_permiso (rol_plantilla, pantalla, accion)
  SELECT rp.rol_plantilla, 'arqueo', 'EDITAR'
  FROM rol_plantilla rp
  WHERE rp.tenant=-1 AND rp.codigo='CAJA'
    AND NOT EXISTS (SELECT 1 FROM rol_plantilla_permiso pp
                    WHERE pp.rol_plantilla=rp.rol_plantilla AND pp.pantalla='arqueo' AND pp.accion='EDITAR');

-- 2) Grupos reales que ya pueden ver el arqueo -> concederles cerrar (idempotente).
INSERT INTO permiso_grupo (grupo, pantalla, accion, usuario_creacion, fecha_creacion)
  SELECT pg.grupo, 'arqueo', 'EDITAR', 'sistema', now()
  FROM permiso_grupo pg
  WHERE pg.pantalla='arqueo' AND pg.accion='VER'
    AND NOT EXISTS (SELECT 1 FROM permiso_grupo x
                    WHERE x.grupo=pg.grupo AND x.pantalla='arqueo' AND x.accion='EDITAR');
