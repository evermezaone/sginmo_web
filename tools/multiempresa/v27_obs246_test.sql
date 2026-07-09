-- Obs 246: f_cobrar_documento debe ABORTAR si un codigo (emisor/procesador/motivo_rechazo)
-- no vacio no existe para tenant IN(-1, tenant del documento), y preferir la opcion propia
-- del tenant sobre la global. Corre DESPUES de V26+V27+V28, dentro de BEGIN...ROLLBACK.
-- Se ejecuta como SUPERADMIN (app.tenant=-1) para que el setup pueda sembrar cualquier tenant.

SELECT set_config('app.tenant', '-1', true);

-- ── Fixture minimo (tenant de prueba = -9001) ──
INSERT INTO persona (persona, tipo_personeria, nombre, numero_documento, estado, usuario_creacion, fecha_creacion) VALUES
  (-9001, 'JURIDICA', 'Empresa Cobro F3', '80930001-1', 'ACTIVO', 't', now()),
  (-9500, 'FISICA',   'Cliente F3',       '9300500',    'ACTIVO', 't', now());
INSERT INTO persona_juridica (persona, razon_social, usuario_creacion, fecha_creacion) VALUES
  (-9001, 'Empresa Cobro F3', 't', now());
INSERT INTO sucursal (sucursal, persona_juridica, tenant, descripcion, direccion, telefono, por_defecto, estado, usuario_creacion, fecha_creacion) VALUES
  (-9101, -9001, -9001, 'Central', 'Dir', '-', true, 'ACTIVO', 't', now());
INSERT INTO moneda (moneda, descripcion, simbolo, tipo_moneda, precision_decimales, tenant, estado, usuario_creacion, fecha_creacion) VALUES
  (-9300, 'Guarani F3', 'Gs', 'LOCAL', 0, -1, 'ACTIVO', 't', now());
-- forma de pago que NO exige nada (la resolucion se dispara por emisor no vacio, no por requiere_*).
INSERT INTO forma_pago (forma_pago, descripcion, codigo, por_defecto,
  requiere_emisor, requiere_procesador, requiere_numero, requiere_serie, requiere_vencimiento,
  requiere_cuenta, requiere_referencia, requiere_cobrador, requiere_fecha_deposito,
  requiere_numero_deposito, requiere_estado_deposito, requiere_motivo_rechazo, requiere_nota_credito,
  tenant, estado, habilitado, usuario_creacion, fecha_creacion) VALUES
  (-9400, 'Tarjeta F3', 'TARJ_F3', false,
   false,false,false,false,false,false,false,false,false,false,false,false,false,
   -9001, 'ACTIVO', true, 't', now());

-- EMISORES: misma clave 'VISA' en global (-1) y en el tenant (-9001) -> debe preferir la del tenant.
INSERT INTO entidad (entidad, lista, codigo, descripcion, tenant, estado, usuario_creacion, fecha_creacion) VALUES
  (-9701, 'EMISORES', 'VISA', 'Visa global', -1,    'ACTIVO', 't', now()),
  (-9702, 'EMISORES', 'VISA', 'Visa tenant', -9001, 'ACTIVO', 't', now());

-- documento con saldo y planilla ABIERTA, ambos del tenant/sucursal -9001/-9101.
-- documento conserva 'empresa' (V26) y ademas gana 'tenant'.
INSERT INTO documento (documento, empresa, tenant, sucursal, tipo, serie, numero, fecha, moneda, cotizacion,
                       total, saldo, direccion_dinero, estado, usuario_creacion, fecha_creacion) VALUES
  (-9600, -9001, -9001, -9101, 'FACV', '001', '0000001', current_date, -9300, 1, 100000, 100000, 'ENTRADA', 'PENDIENTE', 't', now());
INSERT INTO planilla (planilla, tenant, sucursal, usuario_apertura, fecha_apertura, hora_apertura,
                      monto_apertura, monto_cobro, estado, usuario_creacion, fecha_creacion) VALUES
  (-9800, -9001, -9101, 't', current_date, now(), 0, 0, 'ABIERTA', 't', now());

-- ── TEST 1: emisor inexistente 'NOPE' -> DEBE abortar (antes: dato_cobro.emisor NULL en silencio) ──
DO $$ BEGIN
  BEGIN
    PERFORM f_cobrar_documento(-9600, -9800, -9400, -9500, 1000, -9300, current_date, 't', 'NOPE');
    RAISE EXCEPTION 'T1 FALLA: f_cobrar_documento acepto un emisor inexistente';
  EXCEPTION WHEN raise_exception THEN
    IF SQLERRM LIKE 'T1 FALLA%' THEN RAISE;             -- re-lanza el fallo del test
    END IF;                                             -- caso esperado: el RAISE de la funcion
  END;
END $$;

-- ── TEST 2: emisor valido 'VISA' -> cobra y resuelve al id del TENANT (-9702), no al global (-9701) ──
DO $$ DECLARE v_cobro bigint; v_emisor bigint; BEGIN
  v_cobro := f_cobrar_documento(-9600, -9800, -9400, -9500, 1000, -9300, current_date, 't', 'VISA');
  SELECT emisor INTO v_emisor FROM dato_cobro WHERE cobro = v_cobro;
  IF v_emisor IS DISTINCT FROM -9702 THEN
    RAISE EXCEPTION 'T2 FALLA: emisor resuelto = % (esperado -9702, la opcion del tenant)', v_emisor;
  END IF;
END $$;

SELECT 'obs246 OK — codigo invalido aborta; codigo valido prefiere la opcion del tenant' AS resultado;
