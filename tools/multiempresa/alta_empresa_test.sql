-- Batería de verificación del ALTA DE EMPRESA como unidad (F6, REQ-0038).
-- Reproduce, a nivel SQL, los inserts que EmpresaService.altaEmpresa hace en una tx
-- como SUPERADMIN (app.tenant = -1). No persiste: correr entre BEGIN ... ROLLBACK.
--
-- Uso (cuando haya acceso a la BD como rol app):
--   BEGIN;
--   \i V26__multiempresa_esquema.sql
--   \i V27__multiempresa_sps.sql
--   \i V28__multiempresa_rls.sql
--   SET ROLE sginmo;                 -- para que RLS aplique (postgres->sginmo, no bypass)
--   \i alta_empresa_test.sql
--   ROLLBACK;
-- Esperado: EXIT 0 (todos los asserts pasan).

\set ON_ERROR_STOP on

-- Contexto SUPERADMIN: puede provisionar cualquier tenant.
SELECT set_config('app.tenant', '-1', true);

-- 1) La persona sentinel -1 (destino del FK del rol EMPRESA) DEBE existir tras V26.
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM persona_juridica WHERE persona = -1) THEN
    RAISE EXCEPTION 'FALLA: no existe persona_juridica(-1); el rol EMPRESA (tenant=-1) violaria el FK';
  END IF;
END $$;

-- 2) Alta: persona + persona_juridica + rol EMPRESA(tenant=-1) + sucursal(tenant=empresa).
--    (id explicito negativo de prueba para no chocar con la secuencia; ROLLBACK lo descarta.)
INSERT INTO persona (persona, tipo_personeria, nombre, numero_documento, estado,
                     usuario_creacion, fecha_creacion)
  VALUES (-999001, 'JURIDICA', 'Empresa Demo F6', '80099001-1', 'ACTIVO', 'test', now());
INSERT INTO persona_juridica (persona, razon_social) VALUES (-999001, 'Empresa Demo F6');
INSERT INTO persona_rol (persona, rol, tenant, estado, usuario_creacion, fecha_creacion)
  SELECT -999001, e.entidad, -1, 'ACTIVO', 'test', now()
  FROM entidad e WHERE e.lista = 'ROLES_PERSONA' AND e.codigo = 'EMPRESA' AND e.tenant = -1;
INSERT INTO sucursal (persona_juridica, tenant, descripcion, direccion, telefono, por_defecto,
                      estado, usuario_creacion, fecha_creacion)
  VALUES (-999001, -999001, 'Casa Central', 'Direccion demo', '-', true, 'ACTIVO', 'test', now());

-- 3) El rol EMPRESA quedo en el tenant global -1.
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM persona_rol WHERE persona = -999001 AND tenant = -1) THEN
    RAISE EXCEPTION 'FALLA: el rol EMPRESA no quedo en tenant -1';
  END IF;
END $$;

-- 4) Aislamiento: la empresa recien creada ve SU sucursal cuando entra a su propio tenant,
--    y NO la ve otro tenant distinto.
SELECT set_config('app.tenant', '-999001', true);
DO $$ BEGIN
  IF (SELECT count(*) FROM sucursal WHERE persona_juridica = -999001) <> 1 THEN
    RAISE EXCEPTION 'FALLA: la empresa no ve su propia sucursal bajo su tenant';
  END IF;
END $$;

SELECT set_config('app.tenant', '1', true);
DO $$ BEGIN
  IF (SELECT count(*) FROM sucursal WHERE persona_juridica = -999001) <> 0 THEN
    RAISE EXCEPTION 'FALLA: otro tenant ve la sucursal de la empresa demo (RLS no aisla)';
  END IF;
END $$;

SELECT 'alta_empresa_test OK' AS resultado;
