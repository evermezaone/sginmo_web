-- ============================================================================
-- SGInmo Web — V2: seed basico (REQ-0003)
-- Valores reales relevados de la BD legada (docs-migracion/07-datos-reales.md)
-- + listas nuevas del diseño aprobado. Todo editable luego por el ADMINISTRADOR.
-- Convencion: los codigos conservan los del legado (el ETL migra por codigo).
-- ============================================================================

-- ── Parametros del sistema (valores REALES de produccion, doc 07 §2) ───────

INSERT INTO parametro_sistema (clave, valor, descripcion, usuario_creacion, fecha_creacion) VALUES
 ('MONTO_MORA_DEFECTO',          '50000',   'Monto de mora por dia (Gs.)',                    'sistema', now()),
 ('DIAS_GRACIA_VENCIMIENTO',     '3',       'Dias de gracia despues del vencimiento',         'sistema', now()),
 ('PORCENTAJE_COMISION_ALQUILER','50',      'Porcentaje de comision por alquiler',            'sistema', now()),
 ('PORCENTAJE_COMISION_VENTA',   '5',       'Porcentaje de comision por venta',               'sistema', now()),
 ('SUELDO_MINIMO',               '2112562', 'Sueldo minimo vigente (Gs.)',                    'sistema', now()),
 ('JORNAL_MINIMO',               '81252',   'Jornal minimo (sueldo/26) (Gs.)',                'sistema', now()),
 ('IMPUESTOS_MODO_AVANZADO',     'NO',      'SI = ABM de impuestos muestra base imponible parcial
ON CONFLICT (clave) DO NOTHING; NO = modo simplificado', 'sistema', now());

-- ── Listas configurables (tabla entidad) ───────────────────────────────────

INSERT INTO entidad (entidad, codigo, descripcion, usuario_creacion, fecha_creacion) VALUES
 -- Niveles de la jerarquia geografica
 ('NIVELES_UBICACION','PAIS','Pais','sistema',now()),
 ('NIVELES_UBICACION','DEPARTAMENTO','Departamento','sistema',now()),
 ('NIVELES_UBICACION','CIUDAD','Ciudad/Distrito','sistema',now()),
 ('NIVELES_UBICACION','BARRIO','Barrio/Localidad','sistema',now()),
 -- Tipos de activo (une TIPOS_ENTIDADES_INMOBILIARIAS + TIPOS_PROPIEDADES del legado, doc 07 §3)
 ('TIPOS_ACTIVO','EDIFICIO','Edificio','sistema',now()),
 ('TIPOS_ACTIVO','COMPLEJO','Complejo','sistema',now()),
 ('TIPOS_ACTIVO','LOTEAMIENTO','Loteamiento','sistema',now()),
 ('TIPOS_ACTIVO','BARRIO_CERRADO','Barrio cerrado','sistema',now()),
 ('TIPOS_ACTIVO','SALONES_COMERCIALES','Salones comerciales (complejo)','sistema',now()),
 ('TIPOS_ACTIVO','CASA','Casa','sistema',now()),
 ('TIPOS_ACTIVO','DEPARTAMENTO','Departamento','sistema',now()),
 ('TIPOS_ACTIVO','DUPLEX','Duplex','sistema',now()),
 ('TIPOS_ACTIVO','LOTE','Lote','sistema',now()),
 ('TIPOS_ACTIVO','OFICINA','Oficina','sistema',now()),
 ('TIPOS_ACTIVO','PIEZA','Pieza','sistema',now()),
 ('TIPOS_ACTIVO','SALONES','Salon comercial','sistema',now()),
 ('TIPOS_ACTIVO','ESTACIONAMIENTO','Estacionamiento','sistema',now()),
 ('TIPOS_ACTIVO','AREA_COMUN','Area comun','sistema',now()),
 -- Contratos y financiacion (doc 07 §3)
 ('TIPOS_CONTRATOS','PRIVADO','Contrato privado','sistema',now()),
 ('TIPOS_CONTRATOS','PUBLICO','Escritura publica','sistema',now()),
 ('TIPOS_FINANCIACIONES','FINANCIACION_PROPIA','Financiacion propia','sistema',now()),
 ('TIPOS_FINANCIACIONES','FINANCIACION_BANCARIA','Financiacion bancaria','sistema',now()),
 -- Documentos de identidad (doc 07 §3)
 ('TIPOS_DOCUMENTOS_IDENTIDAD','CI','Cedula de identidad','sistema',now()),
 ('TIPOS_DOCUMENTOS_IDENTIDAD','RUC','Registro unico del contribuyente','sistema',now()),
 ('TIPOS_DOCUMENTOS_IDENTIDAD','DOCEX','Documento extranjero','sistema',now()),
 ('TIPOS_DOCUMENTOS_IDENTIDAD','OTROS','Otros','sistema',now()),
 -- Roles de persona (Obs 2)
 ('ROLES_PERSONA','CLIENTE','Cliente','sistema',now()),
 ('ROLES_PERSONA','PROVEEDOR','Proveedor','sistema',now()),
 ('ROLES_PERSONA','EMPLEADO','Empleado','sistema',now()),
 ('ROLES_PERSONA','SOCIO_NEGOCIO','Socio de negocios','sistema',now()),
 ('ROLES_PERSONA','PROPIETARIO','Propietario','sistema',now()),
 ('ROLES_PERSONA','INQUILINO','Inquilino','sistema',now()),
 ('ROLES_PERSONA','VENDEDOR','Vendedor','sistema',now()),
 ('ROLES_PERSONA','PORTERO','Portero','sistema',now()),
 ('ROLES_PERSONA','ADMINISTRADOR','Administrador de inmueble','sistema',now()),
 ('ROLES_PERSONA','EMPRESA','Empresa operadora del sistema','sistema',now()),
 ('ROLES_PERSONA','INMOBILIARIA','Inmobiliaria (persona juridica del rubro)','sistema',now()),
 -- Tipos de comprobante (codigos al estilo Gestion)
 ('TIPOS_DOCUMENTO','FAC','Factura','sistema',now()),
 ('TIPOS_DOCUMENTO','RECI','Recibo','sistema',now()),
 ('TIPOS_DOCUMENTO','NTCR','Nota de credito','sistema',now()),
 ('TIPOS_DOCUMENTO','NTDB','Nota de debito','sistema',now()),
 ('TIPOS_DOCUMENTO','DINT','Documento interno','sistema',now()),
 -- Motivos
 ('MOTIVOS_LIQUIDACION','FIN_CONTRATO','Fin de contrato','sistema',now()),
 ('MOTIVOS_LIQUIDACION','RESCISION','Rescision anticipada','sistema',now()),
 ('MOTIVOS_LIQUIDACION','OTRO','Otro motivo','sistema',now()),
 ('MOTIVOS_ANULACION','ERROR_CARGA','Error de carga','sistema',now()),
 ('MOTIVOS_ANULACION','DUPLICADO','Documento duplicado','sistema',now()),
 ('MOTIVOS_ANULACION','DEVOLUCION','Devolucion','sistema',now()),
 ('MOTIVOS_ANULACION','OTRO','Otro motivo','sistema',now()),
 ('MOTIVOS_RECHAZO','SIN_FONDOS','Cheque sin fondos','sistema',now()),
 ('MOTIVOS_RECHAZO','FIRMA_DIFIERE','Firma difiere','sistema',now()),
 ('MOTIVOS_RECHAZO','CUENTA_CERRADA','Cuenta cerrada','sistema',now()),
 ('MOTIVOS_RECHAZO','OTRO','Otro motivo','sistema',now()),
 -- Gastos e imputaciones (doc 07 §3
ON CONFLICT (entidad, codigo) DO NOTHING; codigos del legado para el ETL)
 ('TIPOS_GASTOS','FIJO','Fijo','sistema',now()),
 ('TIPOS_GASTOS','VARIABLE','Variable','sistema',now()),
 ('TIPOS_IMPUTACION','ADMINISTRADOR','Administrador','sistema',now()),
 ('TIPOS_IMPUTACION','ENTIDAD_INMOBILIARIA','Activo contenedor (edificio/loteamiento)','sistema',now()),
 ('TIPOS_IMPUTACION','INQUILINO','Cliente','sistema',now()),
 ('TIPOS_IMPUTACION','PROPIEDAD','Activo (unidad)','sistema',now()),
 ('TIPOS_IMPUTACION','PROPIETARIO','Propietario','sistema',now()),
 ('TIPOS_IMPUTACION','VENDEDOR','Vendedor','sistema',now()),
 -- Grupos de atributos de activos (Obs 9 del diseño)
 ('GRUPOS_ATRIBUTOS','GENERAL','General','sistema',now()),
 ('GRUPOS_ATRIBUTOS','ESTRUCTURA','Estructura','sistema',now()),
 ('GRUPOS_ATRIBUTOS','PARTE_ELECTRICA','Parte electrica','sistema',now()),
 ('GRUPOS_ATRIBUTOS','AGUA','Agua y sanitarios','sistema',now()),
 ('GRUPOS_ATRIBUTOS','CONFORT','Confort','sistema',now()),
 ('GRUPOS_ATRIBUTOS','SERVICIOS','Servicios','sistema',now()),
 -- Estado civil y actividades (modelo persona)
 ('ESTADOS_CIVILES','SOLTERO','Soltero/a','sistema',now()),
 ('ESTADOS_CIVILES','CASADO','Casado/a','sistema',now()),
 ('ESTADOS_CIVILES','DIVORCIADO','Divorciado/a','sistema',now()),
 ('ESTADOS_CIVILES','VIUDO','Viudo/a','sistema',now()),
 ('ESTADOS_CIVILES','UNION_DE_HECHO','Union de hecho','sistema',now()),
 ('ACTIVIDADES_ECONOMICAS','INMOBILIARIA','Actividad inmobiliaria','sistema',now()),
 ('ACTIVIDADES_ECONOMICAS','OTRA','Otra actividad','sistema',now()),
 -- Listas del maestro articulo (minimas; el administrador amplia)
 ('TIPOS_ARTICULO','SERVICIO_INMOBILIARIO','Servicio inmobiliario','sistema',now()),
 ('TIPOS_ARTICULO','GASTO','Gasto','sistema',now()),
 ('TIPOS_ARTICULO','DESCUENTO','Descuento','sistema',now()),
 ('UNIDADES_MEDIDA','UNIDAD','Unidad','sistema',now()),
 ('UNIDADES_MEDIDA','MES','Mes','sistema',now()),
 ('UNIDADES_MEDIDA','DIA','Dia','sistema',now()),
 ('UNIDADES_MEDIDA','M2','Metro cuadrado','sistema',now()),
 ('PROPIEDADES_ARTICULO','COLOR','Color','sistema',now()),
 ('PROPIEDADES_ARTICULO','TALLE','Talle','sistema',now()),
 ('PROPIEDADES_ARTICULO','CODIGO_BARRA_VARIANTE','Codigo de barra de variante','sistema',now());

-- ── Monedas (las 4 del legado, doc 07) ──────────────────────────────────────

INSERT INTO moneda (moneda, descripcion, simbolo, tipo_moneda, precision_decimales, usuario_creacion, fecha_creacion) VALUES
 (1, 'Guaranies',           'Gs.', 'LOCAL',      0, 'sistema', now()),
 (2, 'Dolares americanos',  'USD', 'EXTRANJERA', 2, 'sistema', now()),
 (3, 'Reales',              'R$',  'EXTRANJERA', 2, 'sistema', now()),
 (4, 'Euros',               'EUR', 'EXTRANJERA', 2, 'sistema', now());
SELECT setval(pg_get_serial_sequence('moneda','moneda'), 4, true);

-- ── Impuestos (regimen IVA Paraguay; el administrador ajusta) ───────────────

INSERT INTO impuesto (impuesto, descripcion, porcentaje_impuesto, factor_discriminado, factor_impuesto, porcentaje_base_gravada, usuario_creacion, fecha_creacion) VALUES
 (1, 'IVA 10%', 10, 11, 1.10, 100, 'sistema', now()),
 (2, 'IVA 5%',   5, 21, 1.05, 100, 'sistema', now()),
 (3, 'Exenta',   0,  0, 1.00, 100, 'sistema', now());
SELECT setval(pg_get_serial_sequence('impuesto','impuesto'), 3, true);
-- Ejemplos de base reducida (los crea el administrador en modo avanzado cuando los necesite):
--   'IVA 10% base 20%' -> porcentaje 10, factores del 10%, base_gravada 20
--   'IVA 10% base 30% (venta inmuebles)' -> porcentaje 10, factores del 10%, base_gravada 30

-- ── Formas de pago (flags requiere_* segun el medio) ───────────────────────

INSERT INTO forma_pago (forma_pago, descripcion, codigo, por_defecto,
  requiere_emisor, requiere_procesador, requiere_numero, requiere_serie, requiere_vencimiento,
  requiere_cuenta, requiere_referencia, usuario_creacion, fecha_creacion) VALUES
 (1, 'Efectivo',          'EFE', true,  false, false, false, false, false, false, false, 'sistema', now()),
 (2, 'Debito',            'DB',  false, true,  true,  true,  false, false, false, true,  'sistema', now()),
 (3, 'Transferencia',     'TRF', false, true,  false, false, false, false, true,  true,  'sistema', now()),
 (4, 'Cheque',            'CHQ', false, true,  false, true,  true,  true,  true,  false, 'sistema', now()),
 (5, 'Tarjeta de credito','TC',  false, true,  true,  true,  false, true,  false, true,  'sistema', now());
SELECT setval(pg_get_serial_sequence('forma_pago','forma_pago'), 5, true);

-- ── Articulos de servicio (conceptos de dinero; aplicaciones que usa el codigo, doc 02 §1.13) ──
-- IVA por defecto: alquiler de vivienda 5%, servicios 10%; EL ADMINISTRADOR AJUSTA segun regimen real.

INSERT INTO articulo (articulo, codigo, descripcion, tipo, impuesto,
  categoria_lista, categoria_codigo, unidad_medida_lista, unidad_medida_codigo,
  tipo_movimiento, modifica_estado, aplicacion, usuario_creacion, fecha_creacion) VALUES
 (1,  'ALQ',   'Cuota de alquiler',            'SERVICIO', 2, 'TIPOS_ARTICULO','SERVICIO_INMOBILIARIO','UNIDADES_MEDIDA','MES',    'INGRESO',  true,  'ALQUILER',             'sistema', now()),
 (2,  'MORA',  'Mora por atraso',              'SERVICIO', 3, 'TIPOS_ARTICULO','SERVICIO_INMOBILIARIO','UNIDADES_MEDIDA','DIA',    'INGRESO',  false, 'MORA',                 'sistema', now()),
 (3,  'DESC',  'Descuento',                    'SERVICIO', 3, 'TIPOS_ARTICULO','DESCUENTO',            'UNIDADES_MEDIDA','UNIDAD', 'DESCUENTO',false, 'DESCUENTO',            'sistema', now()),
 (4,  'COMV',  'Comision por venta',           'SERVICIO', 1, 'TIPOS_ARTICULO','SERVICIO_INMOBILIARIO','UNIDADES_MEDIDA','UNIDAD', 'EGRESO',   false, 'COMISION_VENTA',       'sistema', now()),
 (5,  'COMA',  'Comision por alquiler',        'SERVICIO', 1, 'TIPOS_ARTICULO','SERVICIO_INMOBILIARIO','UNIDADES_MEDIDA','UNIDAD', 'EGRESO',   false, 'COMISION_ALQUILER',    'sistema', now()),
 (6,  'GARA',  'Deposito de garantia',         'SERVICIO', 3, 'TIPOS_ARTICULO','SERVICIO_INMOBILIARIO','UNIDADES_MEDIDA','UNIDAD', 'INGRESO',  false, 'DEPOSITO_GARANTIA',    'sistema', now()),
 (7,  'VENTA', 'Venta de inmueble',            'SERVICIO', 3, 'TIPOS_ARTICULO','SERVICIO_INMOBILIARIO','UNIDADES_MEDIDA','UNIDAD', 'INGRESO',  true,  'VENTA_INMUEBLE',       'sistema', now()),
 (8,  'ANDE',  'Energia electrica (ANDE)',     'SERVICIO', 1, 'TIPOS_ARTICULO','GASTO',                'UNIDADES_MEDIDA','UNIDAD', 'EGRESO',   false, 'ANDE',                 'sistema', now()),
 (9,  'ESSAP', 'Agua (ESSAP)',                 'SERVICIO', 1, 'TIPOS_ARTICULO','GASTO',                'UNIDADES_MEDIDA','UNIDAD', 'EGRESO',   false, 'AGUA',                 'sistema', now()),
 (10, 'MATE',  'Materiales',                   'SERVICIO', 1, 'TIPOS_ARTICULO','GASTO',                'UNIDADES_MEDIDA','UNIDAD', 'EGRESO',   false, 'MATERIALES',           'sistema', now()),
 (11, 'MOBRA', 'Mano de obra',                 'SERVICIO', 1, 'TIPOS_ARTICULO','GASTO',                'UNIDADES_MEDIDA','UNIDAD', 'EGRESO',   false, 'MANO_OBRA',            'sistema', now()),
 (12, 'CERRA', 'Cerrajeria',                   'SERVICIO', 1, 'TIPOS_ARTICULO','GASTO',                'UNIDADES_MEDIDA','UNIDAD', 'EGRESO',   false, 'CERRAJERIA',           'sistema', now()),
 (13, 'LIMP',  'Limpieza',                     'SERVICIO', 1, 'TIPOS_ARTICULO','GASTO',                'UNIDADES_MEDIDA','UNIDAD', 'EGRESO',   false, 'LIMPIEZA',             'sistema', now()),
 (14, 'ADMIN', 'Gastos administrativos',       'SERVICIO', 1, 'TIPOS_ARTICULO','GASTO',                'UNIDADES_MEDIDA','UNIDAD', 'EGRESO',   false, 'ADMINISTRATIVOS',      'sistema', now()),
 (15, 'ALQPE', 'Alquileres pendientes (liq.)', 'SERVICIO', 3, 'TIPOS_ARTICULO','SERVICIO_INMOBILIARIO','UNIDADES_MEDIDA','UNIDAD', 'INGRESO',  false, 'ALQUILERES_PENDIENTES','sistema', now());
SELECT setval(pg_get_serial_sequence('articulo','articulo'), 15, true);

-- ── Ubicaciones geograficas ─────────────────────────────────────────────────
-- El seed geografico completo va en V3__ubicaciones_paraguay.sql, GENERADO desde
-- los archivos oficiales del INE 2022 (departamentos, distritos, barrios/localidades)
-- con codigo_oficial para futuras actualizaciones por upsert.
