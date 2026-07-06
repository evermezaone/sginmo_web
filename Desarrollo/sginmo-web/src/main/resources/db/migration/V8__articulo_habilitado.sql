-- V8 (estandar ABM, regla 1 del estudio): vigencia logica (estado) separada de la
-- disponibilidad para operaciones nuevas (habilitado). Un articulo ACTIVO pero NO
-- habilitado conserva historial y reportes, pero no aparece en combos de operacion.
ALTER TABLE articulo ADD COLUMN habilitado boolean NOT NULL DEFAULT true;
