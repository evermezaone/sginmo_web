-- V4: columna de control de concurrencia optimista (JPA @Version) en TODAS las tablas.
-- Regla del estandar ABM: si dos usuarios editan el mismo registro, el segundo NO pisa
-- en silencio; recibe "El registro fue modificado por otro usuario".
-- Toda tabla nueva debe nacer con esta columna.
-- NOTA para los motores en BD (documento/cobro): los procedimientos que actualicen filas
-- tambien administradas por un ABM de JPA deben hacer SET version = version + 1.

ALTER TABLE parametro_sistema    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE entidad              ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ubicacion_geografica ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE moneda               ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE forma_pago           ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE impuesto             ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE articulo             ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE articulo_propiedad   ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE articulo_costo       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE persona              ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE persona_fisica       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE persona_juridica     ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE persona_rol          ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sucursal             ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE usuario              ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE activo               ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE atributo             ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE atributo_por_tipo    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE activo_atributo      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE activo_propietario   ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE operacion            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rescision            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rango_comprobante    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE documento            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE documento_detalle    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cronograma_cuota     ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE planilla             ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cobro                ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cobro_detalle        ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE dato_cobro           ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE detalle_afectado     ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE anulacion            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ingreso_egreso       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE liquidacion          ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE liquidacion_detalle  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE archivo_adjunto      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
