-- Reusable operational snapshot for agent loops.
-- Use this instead of uploading temporary scripts for recurring mailbox/combo reads.

CREATE OR REPLACE VIEW vw_handoff_reqs_activos AS
SELECT
    p.Codigo AS Proyecto,
    r.Codigo AS Req,
    CAST(SUBSTRING(r.Codigo, 5) AS UNSIGNED) AS Numero,
    r.Titulo,
    r.Estado,
    r.Responsable,
    r.ActualizadoEn,
    (
        SELECT r2.Codigo
        FROM REQ r2
        WHERE r2.IdProyecto = r.IdProyecto
          AND r2.Estado NOT IN ('CERRADO', 'CANCELADO')
          AND CAST(SUBSTRING(r2.Codigo, 5) AS UNSIGNED) < CAST(SUBSTRING(r.Codigo, 5) AS UNSIGNED)
        ORDER BY CAST(SUBSTRING(r2.Codigo, 5) AS UNSIGNED)
        LIMIT 1
    ) AS BloqueadoPorReqMenor
FROM REQ r
JOIN PROYECTO p ON p.IdProyecto = r.IdProyecto
WHERE r.Estado NOT IN ('CERRADO', 'CANCELADO');

DELIMITER //
DROP PROCEDURE IF EXISTS sp_handoff_estado_combo//
CREATE PROCEDURE sp_handoff_estado_combo(
    IN p_project VARCHAR(50),
    IN p_agente VARCHAR(50)
)
BEGIN
    SELECT
        COUNT(*) AS TotalActivos,
        SUM(CASE WHEN Responsable = 'claude' THEN 1 ELSE 0 END) AS PendientesClaude,
        SUM(CASE WHEN Responsable = 'codex' THEN 1 ELSE 0 END) AS PendientesCodex,
        SUM(CASE WHEN Responsable = p_agente THEN 1 ELSE 0 END) AS PendientesDelAgente,
        SUM(CASE WHEN BloqueadoPorReqMenor IS NULL THEN 1 ELSE 0 END) AS Libres,
        SUM(CASE WHEN BloqueadoPorReqMenor IS NOT NULL THEN 1 ELSE 0 END) AS Bloqueados,
        CASE WHEN COUNT(*) = 0 THEN 1 ELSE 0 END AS PuedeDetenerLoop
    FROM vw_handoff_reqs_activos
    WHERE Proyecto = p_project;

    SELECT
        Responsable,
        Estado,
        COUNT(*) AS Cantidad
    FROM vw_handoff_reqs_activos
    WHERE Proyecto = p_project
    GROUP BY Responsable, Estado
    ORDER BY Responsable, Estado;

    SELECT
        Proyecto,
        Req,
        Numero,
        Titulo,
        Estado,
        Responsable,
        ActualizadoEn,
        CASE
            WHEN BloqueadoPorReqMenor IS NULL THEN 'LIBRE'
            ELSE CONCAT('BLOQUEADO_POR_', BloqueadoPorReqMenor)
        END AS EstadoOperativo
    FROM vw_handoff_reqs_activos
    WHERE Proyecto = p_project
      AND (
          BloqueadoPorReqMenor IS NULL
          OR Responsable = p_agente
          OR Estado = 'LISTO_PARA_REVISION'
      )
    ORDER BY Numero;

    SELECT
        p.Codigo AS Proyecto,
        r.Codigo AS Req,
        o.IdObservacion,
        o.Auditor,
        o.Ronda,
        o.Categoria,
        o.Subcategoria,
        o.Severidad,
        o.Resumen,
        o.Archivo,
        o.CreadoEn
    FROM AUDITORIA_OBSERVACION o
    JOIN REQ r ON r.IdReq = o.IdReq
    JOIN PROYECTO p ON p.IdProyecto = r.IdProyecto
    WHERE p.Codigo = p_project
      AND o.Estado = 'abierta'
    ORDER BY CAST(SUBSTRING(r.Codigo, 5) AS UNSIGNED), o.IdObservacion;
END//
DELIMITER ;
