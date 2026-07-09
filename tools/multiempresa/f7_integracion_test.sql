-- ============================================================================
-- REQ-0039 F7 — Verificacion integral multiempresa con 2 empresas (RLS real).
-- Corre DESPUES de V26+V27+V28 dentro de una unica transaccion; se descarta con ROLLBACK.
-- Conectado como el rol de la app (sginmo): NO superuser y NO bypassrls, y V28 usa FORCE
-- ROW LEVEL SECURITY, asi que las politicas aplican incluso al owner de las tablas.
-- Cualquier assert fallido -> RAISE EXCEPTION -> psql EXIT != 0 (ON_ERROR_STOP).
-- Ids negativos de prueba (no chocan con las secuencias); el ROLLBACK los descarta.
-- ============================================================================

-- ── Provisiona 2 empresas como lo hace EmpresaService.altaEmpresa (contexto SUPERADMIN) ──
SELECT set_config('app.tenant', '-1', true);

-- Empresa A (-9001) y Empresa B (-9002): persona + persona_juridica (post-V26 persona reducida).
INSERT INTO persona (persona, tipo_personeria, nombre, numero_documento,
                     estado, usuario_creacion, fecha_creacion) VALUES
  (-9001, 'JURIDICA', 'Empresa A F7', '80900001-1', 'ACTIVO', 'f7', now()),
  (-9002, 'JURIDICA', 'Empresa B F7', '80900002-1', 'ACTIVO', 'f7', now());
INSERT INTO persona_juridica (persona, razon_social, usuario_creacion, fecha_creacion) VALUES
  (-9001, 'Empresa A F7', 'f7', now()),
  (-9002, 'Empresa B F7', 'f7', now());

-- Rol EMPRESA (identitario, vive en tenant -1) para cada una.
INSERT INTO persona_rol (persona_rol, persona, rol, tenant, estado, usuario_creacion, fecha_creacion)
  SELECT -9001, -9001, e.entidad, -1, 'ACTIVO', 'f7', now() FROM entidad e
    WHERE e.lista='ROLES_PERSONA' AND e.codigo='EMPRESA' AND e.tenant=-1;
INSERT INTO persona_rol (persona_rol, persona, rol, tenant, estado, usuario_creacion, fecha_creacion)
  SELECT -9002, -9002, e.entidad, -1, 'ACTIVO', 'f7', now() FROM entidad e
    WHERE e.lista='ROLES_PERSONA' AND e.codigo='EMPRESA' AND e.tenant=-1;

-- Sucursal por defecto de cada empresa (transaccional: tenant = la empresa).
INSERT INTO sucursal (sucursal, persona_juridica, tenant, descripcion, direccion, telefono,
                      por_defecto, estado, usuario_creacion, fecha_creacion) VALUES
  (-9101, -9001, -9001, 'Central A', 'Dir A', '-', true, 'ACTIVO', 'f7', now()),
  (-9102, -9002, -9002, 'Central B', 'Dir B', '-', true, 'ACTIVO', 'f7', now());

-- Usuario ADMINISTRADOR inicial de cada empresa (tenant = la empresa).
INSERT INTO usuario (usuario, codigo_usuario, password_hash, perfil, estado, tenant,
                     intentos_fallidos, debe_cambiar_password, usuario_creacion, fecha_creacion) VALUES
  (-9001, 'adminA_f7', 'x', 'ADMINISTRADOR', 'ACTIVO', -9001, 0, true, 'f7', now()),
  (-9002, 'adminB_f7', 'x', 'ADMINISTRADOR', 'ACTIVO', -9002, 0, true, 'f7', now());

-- Catalogo de negocio (entidad) con una opcion global -1 y una propia por tenant.
INSERT INTO entidad (entidad, lista, codigo, descripcion, tenant, estado, usuario_creacion, fecha_creacion) VALUES
  (-9200, 'F7_TEST', 'G', 'Global',    -1,    'ACTIVO', 'f7', now()),
  (-9201, 'F7_TEST', 'A', 'Solo A',    -9001, 'ACTIVO', 'f7', now()),
  (-9202, 'F7_TEST', 'B', 'Solo B',    -9002, 'ACTIVO', 'f7', now());

-- ── ASSERT 1: SUPERADMIN (app.tenant=-1) ve las 2 sucursales de prueba ──
SELECT set_config('app.tenant', '-1', true);
DO $$ BEGIN
  IF (SELECT count(*) FROM sucursal WHERE sucursal IN (-9101,-9102)) <> 2 THEN
    RAISE EXCEPTION 'A1 FALLA: SUPERADMIN no ve ambas sucursales';
  END IF;
END $$;

-- ── ASSERT 2: Empresa A solo ve LO SUYO (sucursal propia + catalogo global, no lo de B) ──
SELECT set_config('app.tenant', '-9001', true);
DO $$ BEGIN
  IF (SELECT count(*) FROM sucursal WHERE sucursal=-9101) <> 1 THEN
    RAISE EXCEPTION 'A2 FALLA: A no ve su propia sucursal';
  END IF;
  IF (SELECT count(*) FROM sucursal WHERE sucursal=-9102) <> 0 THEN
    RAISE EXCEPTION 'A2 FALLA: A ve la sucursal de B (RLS no aisla)';
  END IF;
  -- catalogo F7_TEST visible para A = global(G) + propia(A) = 2 (no la B)
  IF (SELECT count(*) FROM entidad WHERE lista='F7_TEST') <> 2 THEN
    RAISE EXCEPTION 'A2 FALLA: A no ve exactamente global+propia en el catalogo';
  END IF;
  IF EXISTS (SELECT 1 FROM entidad WHERE lista='F7_TEST' AND codigo='B') THEN
    RAISE EXCEPTION 'A2 FALLA: A ve la opcion de catalogo de B';
  END IF;
END $$;

-- ── ASSERT 3: Empresa B analogamente solo ve lo suyo ──
SELECT set_config('app.tenant', '-9002', true);
DO $$ BEGIN
  IF (SELECT count(*) FROM sucursal WHERE sucursal=-9102) <> 1
     OR (SELECT count(*) FROM sucursal WHERE sucursal=-9101) <> 0 THEN
    RAISE EXCEPTION 'A3 FALLA: B no queda aislada de A';
  END IF;
  IF EXISTS (SELECT 1 FROM entidad WHERE lista='F7_TEST' AND codigo='A') THEN
    RAISE EXCEPTION 'A3 FALLA: B ve la opcion de catalogo de A';
  END IF;
END $$;

-- ── ASSERT 4: INSERT cross-tenant NEGADO por RLS (A intenta crear fila de B) ──
DO $$ BEGIN
  PERFORM set_config('app.tenant', '-9001', true);
  BEGIN
    INSERT INTO sucursal (sucursal, persona_juridica, tenant, descripcion, direccion, telefono,
                          por_defecto, estado, usuario_creacion, fecha_creacion, version)
      VALUES (-9199, -9002, -9002, 'Intrusa', 'x', '-', false, 'ACTIVO', 'f7', now(), 0);
    RAISE EXCEPTION 'A4 FALLA: RLS permitio INSERT cross-tenant';
  EXCEPTION
    WHEN insufficient_privilege OR check_violation THEN NULL;  -- esperado: RLS lo niega
  END;
END $$;

-- ── ASSERT 5: UPDATE cross-tenant = no-op (la fila de B es invisible para A) ──
SELECT set_config('app.tenant', '-9001', true);
DO $$ DECLARE afectadas int; BEGIN
  UPDATE sucursal SET descripcion='hackeada' WHERE sucursal=-9102;
  GET DIAGNOSTICS afectadas = ROW_COUNT;
  IF afectadas <> 0 THEN
    RAISE EXCEPTION 'A5 FALLA: UPDATE cross-tenant afecto % filas (RLS no aisla)', afectadas;
  END IF;
END $$;

-- ── ASSERT 6: "operar como" (SUPERADMIN fija app.tenant=A) acota igual que un admin de A ──
SELECT set_config('app.tenant', '-9001', true);
DO $$ BEGIN
  IF (SELECT count(*) FROM sucursal WHERE sucursal IN (-9101,-9102)) <> 1 THEN
    RAISE EXCEPTION 'A6 FALLA: operar-como no acota al tenant elegido';
  END IF;
END $$;

SELECT 'F7 INTEGRACION OK — 2 empresas aisladas, superadmin total, cross-tenant negado' AS resultado;
