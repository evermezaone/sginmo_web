-- V9 (REQ-0004, pedido del usuario): permiso agrupador OPERAR.
-- OPERAR = trabajo normal del ABM (VER, CREAR, EDITAR, INACTIVAR, REACTIVAR, EXPORTAR)
-- en un solo permiso. VER_AUDITORIA queda SIEMPRE fuera: se otorga aparte.
ALTER TABLE permiso_usuario DROP CONSTRAINT permiso_usuario_accion_check;
ALTER TABLE permiso_usuario ADD CONSTRAINT permiso_usuario_accion_check CHECK (accion IN
  ('OPERAR','VER','CREAR','EDITAR','INACTIVAR','REACTIVAR','EXPORTAR','VER_AUDITORIA'));
