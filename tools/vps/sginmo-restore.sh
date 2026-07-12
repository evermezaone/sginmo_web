#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# REQ-0066 — Restore probado / simulacro de recuperacion de SGInmo
# ───────────────────────────────────────────────────────────────────────────
# Restaura un backup .dump (formato custom) en una base TEMPORAL (nunca produccion,
# salvo confirmacion doble y explicita) y ejecuta validaciones de integridad:
# conexion, Flyway, y conteos de tablas criticas (con app.tenant=-1 = SUPERADMIN,
# que bajo la RLS ve TODOS los tenants). Emite un reporte de simulacro JSON.
#
# Uso:
#   sginmo-restore.sh <archivo.dump> [base_temporal]        # plan (no ejecuta)
#   sginmo-restore.sh <archivo.dump> [base_temporal] --yes  # ejecuta el restore
#   sginmo-restore.sh --latest --yes                        # usa el ultimo daily
# Opciones:
#   --yes            Ejecuta de verdad (sin esto, solo muestra el plan).
#   --latest         Toma el .dump mas nuevo de $SGINMO_BACKUP_ROOT/daily.
#   --keep           No borra la base temporal al terminar (default: la borra).
#   --recreate       Si la base temporal existe, la elimina antes de crear.
#   --prod-confirm=SI_ESTOY_SEGURO   Permite apuntar a la BD de produccion (peligroso).
#
# Config: mismo archivo protegido que el backup ($SGINMO_BACKUP_ENV, default
#         /home/edm/sginmo/backup.env). NUNCA se imprime la clave.
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail

BACKUP_ENV="${SGINMO_BACKUP_ENV:-/home/edm/sginmo/backup.env}"
if [[ -f "$BACKUP_ENV" ]]; then set -a; . "$BACKUP_ENV"; set +a; fi

PGHOST="${PGHOST:-localhost}"; PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-sginmo}"; PGUSER="${PGUSER:-sginmo}"
BACKUP_ROOT="${SGINMO_BACKUP_ROOT:-/home/edm/backups}"
REPORT_LATEST="$BACKUP_ROOT/latest-restore.json"
REPORT_HIST="$BACKUP_ROOT/restore-report.jsonl"
LOG="$BACKUP_ROOT/restore.log"

DUMP=""; TARGET="sginmo_restore_test"; DO_RUN=0; KEEP=0; RECREATE=0; PROD_OK=""
for a in "$@"; do
  case "$a" in
    --yes) DO_RUN=1 ;;
    --latest) DUMP="$(ls -1t "$BACKUP_ROOT"/daily/*.dump 2>/dev/null | head -1 || true)" ;;
    --keep) KEEP=1 ;;
    --recreate) RECREATE=1 ;;
    --prod-confirm=*) PROD_OK="${a#*=}" ;;
    --*) echo "Opcion desconocida: $a" >&2; exit 2 ;;
    *) if [[ -z "$DUMP" ]]; then DUMP="$a"; else TARGET="$a"; fi ;;
  esac
done

log() { echo "[$(date +%Y-%m-%dT%H:%M:%S%z)] $*" | tee -a "$LOG"; }
json_escape() { local s=${1//\\/\\\\}; s=${s//\"/\\\"}; s=${s//$'\n'/ }; printf '%s' "$s"; }

[[ -z "$DUMP" ]] && { echo "ERROR: falta el archivo .dump (o usar --latest)"; exit 2; }
[[ -r "$DUMP" ]] || { echo "ERROR: no se puede leer el dump: $DUMP"; exit 2; }

# ── Guardia anti-produccion ──────────────────────────────────────────────────
if [[ "$TARGET" == "$PGDATABASE" ]]; then
  if [[ "$PROD_OK" != "SI_ESTOY_SEGURO" ]]; then
    echo "ABORTADO: '$TARGET' es la BD de PRODUCCION. Para restaurar sobre prod (peligroso)"
    echo "hay que pasar --prod-confirm=SI_ESTOY_SEGURO. Normalmente se restaura en una base temporal."
    exit 3
  fi
  echo "!!! ADVERTENCIA: vas a restaurar SOBRE PRODUCCION ($TARGET) !!!"
fi

echo "Plan de restore:"
echo "  dump    : $DUMP"
echo "  destino : $TARGET (temporal)"
echo "  validar : Flyway + conteos de tablas criticas (app.tenant=-1)"
echo "  limpieza: $([[ $KEEP -eq 1 ]] && echo 'conserva la base' || echo 'elimina la base al terminar')"
if [[ $DO_RUN -eq 0 ]]; then
  echo "(plan solamente; agrega --yes para ejecutar)"; exit 0
fi

START_EPOCH="$(date +%s)"
TS_HUMAN="$(date +%Y-%m-%dT%H:%M:%S%z)"
ERR=""; RESULT="OK"
PSQL=(psql -v ON_ERROR_STOP=1 -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$TARGET")
PSQL_ADMIN=(psql -v ON_ERROR_STOP=1 -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres)

report() {
  local counts_json="$1"
  local dur=$(( $(date +%s) - START_EPOCH ))
  local json
  json=$(cat <<JSON
{"timestamp":"$TS_HUMAN","dump":"$(json_escape "$DUMP")","destino":"$(json_escape "$TARGET")","resultado":"$RESULT","error":$( [[ -z "$ERR" ]] && echo null || printf '"%s"' "$(json_escape "$ERR")"),"conteos":$counts_json,"duracion_seg":$dur}
JSON
)
  printf '%s\n' "$json" > "$REPORT_LATEST"
  printf '%s\n' "$json" >> "$REPORT_HIST"
  echo "$json"
}

fail() { ERR="$1"; RESULT="FAIL"; log "ERROR: $1"; report '{}' >/dev/null; exit 1; }
trap 'fail "fallo en linea ${BASH_LINENO[0]:-?}"' ERR

log "═══ Restore/simulacro iniciado -> base temporal '$TARGET' ═══"

# ── 1) (Re)crear base temporal ───────────────────────────────────────────────
if PGPASSWORD="${PGPASSWORD:-}" "${PSQL_ADMIN[@]}" -tAc \
     "SELECT 1 FROM pg_database WHERE datname='$TARGET'" | grep -q 1; then
  if [[ $RECREATE -eq 1 ]]; then
    log "La base '$TARGET' existe; se elimina (--recreate)"
    PGPASSWORD="${PGPASSWORD:-}" "${PSQL_ADMIN[@]}" -c "DROP DATABASE \"$TARGET\"" >/dev/null
  else
    fail "la base temporal '$TARGET' ya existe (usar --recreate para reemplazarla)"
  fi
fi
PGPASSWORD="${PGPASSWORD:-}" "${PSQL_ADMIN[@]}" -c "CREATE DATABASE \"$TARGET\"" >/dev/null
log "Base temporal creada: $TARGET"

# ── 2) Restaurar el dump (custom) ────────────────────────────────────────────
# Obs 268: pg_restore devuelve exit != 0 cuando hubo ERRORES (no meras advertencias). Un restore
# parcial NO puede terminar en OK: si el codigo de salida es distinto de 0, se falla el simulacro.
set +e
PGPASSWORD="${PGPASSWORD:-}" pg_restore --enable-row-security --no-owner --no-privileges \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$TARGET" "$DUMP" 2> >(tee -a "$LOG" >&2)
RESTORE_RC=$?
set -e
if [[ $RESTORE_RC -ne 0 ]]; then
  fail "pg_restore devolvio error (rc=$RESTORE_RC): restore parcial/invalido, no se marca OK"
fi
log "pg_restore finalizado (rc=0)"

# ── 3) Validaciones de integridad (con app.tenant=-1 = ve todos los tenants) ──
q() { PGPASSWORD="${PGPASSWORD:-}" PGOPTIONS='-c app.tenant=-1' "${PSQL[@]}" -tAc "$1" 2>/dev/null || echo "ERR"; }

FLYWAY="$(q "SELECT max(version) FROM flyway_schema_history WHERE success")"
[[ "$FLYWAY" == "ERR" || -z "$FLYWAY" ]] && fail "no se pudo leer flyway_schema_history en la base restaurada"
log "Flyway en la base restaurada: V$FLYWAY"

# Tablas criticas (existen desde V26/negocio). Se cuentan con visibilidad SUPERADMIN.
declare -a TABLAS=(usuario grupo persona operacion planilla cobro ingreso_egreso documento parametro_sistema entidad)
COUNTS="{"; SEP=""
for t in "${TABLAS[@]}"; do
  c="$(q "SELECT count(*) FROM public.$t")"
  # Obs 268: si una tabla critica no se puede consultar (faltante/corrupta), el simulacro FALLA;
  # no se degrada a null ni se marca OK.
  if [[ "$c" == "ERR" ]]; then
    fail "no se pudo consultar la tabla critica '$t' en la base restaurada (restore incompleto)"
  fi
  COUNTS="$COUNTS$SEP\"$t\":$c"; SEP=","
  log "conteo $t = $c"
done
COUNTS="$COUNTS,\"flyway\":\"$FLYWAY\"}"

# ── 4) Limpieza de la base temporal ──────────────────────────────────────────
if [[ $KEEP -eq 0 && "$TARGET" != "$PGDATABASE" ]]; then
  PGPASSWORD="${PGPASSWORD:-}" "${PSQL_ADMIN[@]}" -c "DROP DATABASE \"$TARGET\"" >/dev/null
  log "Base temporal eliminada: $TARGET"
fi

# ── 5) Reporte de simulacro ──────────────────────────────────────────────────
report "$COUNTS"
log "═══ Simulacro de restore OK en $(( $(date +%s) - START_EPOCH ))s ═══"
