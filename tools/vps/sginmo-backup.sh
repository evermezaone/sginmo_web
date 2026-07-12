#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# REQ-0065 — Backup automatico de SGInmo (PostgreSQL + archivos + config)
# ───────────────────────────────────────────────────────────────────────────
# - Vuelca la BD PostgreSQL con pg_dump en formato custom (.dump), compatible
#   con RLS multiempresa (--enable-row-security + app.tenant=-1).
# - Respalda el repositorio documental (adjuntos/documentos generados) si existe.
# - Escribe un manifiesto (latest.json + manifest.jsonl) con fecha, tamano,
#   SHA-256, duracion y resultado. Lo consume el panel de Salud (REQ-0051).
# - Retencion por clases diaria/semanal/mensual, sin borrar el ultimo valido.
# - SIN secretos hardcodeados: la clave se lee de un archivo protegido/entorno.
#
# Uso:   sginmo-backup.sh            # backup normal (respeta clases del dia)
#        sginmo-backup.sh --dry-run  # muestra que haria, no ejecuta pg_dump
#
# Config (archivo protegido, NO versionado). Por defecto:
#   ${SGINMO_BACKUP_ENV:-/home/edm/sginmo/backup.env}
# Variables reconocidas (todas con default sano):
#   PGHOST=localhost PGPORT=5432 PGDATABASE=sginmo PGUSER=sginmo PGPASSWORD=...
#   SGINMO_BACKUP_ROOT=/home/edm/backups
#   SGINMO_ARCHIVOS_DIR=/home/edm/sginmo/archivos
#   SGINMO_KEEP_DAILY=7  SGINMO_KEEP_WEEKLY=4  SGINMO_KEEP_MONTHLY=6
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail

# ── Carga de configuracion (archivo protegido con la clave; nunca se versiona) ──
BACKUP_ENV="${SGINMO_BACKUP_ENV:-/home/edm/sginmo/backup.env}"
if [[ -f "$BACKUP_ENV" ]]; then
  # shellcheck disable=SC1090
  set -a; . "$BACKUP_ENV"; set +a
fi

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-sginmo}"
PGUSER="${PGUSER:-sginmo}"
BACKUP_ROOT="${SGINMO_BACKUP_ROOT:-/home/edm/backups}"
ARCHIVOS_DIR="${SGINMO_ARCHIVOS_DIR:-/home/edm/sginmo/archivos}"
KEEP_DAILY="${SGINMO_KEEP_DAILY:-7}"
KEEP_WEEKLY="${SGINMO_KEEP_WEEKLY:-4}"
KEEP_MONTHLY="${SGINMO_KEEP_MONTHLY:-6}"
DRY_RUN=0
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=1

LOG="$BACKUP_ROOT/backup.log"
LATEST="$BACKUP_ROOT/latest.json"
HISTORY="$BACKUP_ROOT/manifest.jsonl"
TS_HUMAN="$(date +%Y-%m-%dT%H:%M:%S%z)"
STAMP="$(date +%Y%m%d_%H%M%S)"
DOW="$(date +%u)"   # 1..7 (7=domingo)
DOM="$(date +%d)"   # 01..31
START_EPOCH="$(date +%s)"

mkdir -p "$BACKUP_ROOT/daily" "$BACKUP_ROOT/weekly" "$BACKUP_ROOT/monthly"

log() { echo "[$(date +%Y-%m-%dT%H:%M:%S%z)] $*" >>"$LOG"; }

# ── JSON string escaper minimo (para rutas/errores en el manifiesto) ──
json_escape() { local s=${1//\\/\\\\}; s=${s//\"/\\\"}; s=${s//$'\n'/ }; printf '%s' "$s"; }

# ── Manifiesto de resultado. Escribe latest.json y agrega linea a manifest.jsonl ──
write_manifest() {
  local resultado="$1" error="$2" db_file="$3" db_bytes="$4" db_sha="$5" \
        files_file="$6" files_bytes="$7" files_sha="$8" dur="$9"
  local json
  json=$(cat <<JSON
{"timestamp":"$TS_HUMAN","base":"$(json_escape "$PGDATABASE")","resultado":"$resultado","error":$( [[ -z "$error" ]] && echo null || printf '"%s"' "$(json_escape "$error")"),"db":{"archivo":$( [[ -z "$db_file" ]] && echo null || printf '"%s"' "$(json_escape "$db_file")"),"bytes":${db_bytes:-0},"sha256":$( [[ -z "$db_sha" ]] && echo null || printf '"%s"' "$db_sha")},"archivos":{"archivo":$( [[ -z "$files_file" ]] && echo null || printf '"%s"' "$(json_escape "$files_file")"),"bytes":${files_bytes:-0},"sha256":$( [[ -z "$files_sha" ]] && echo null || printf '"%s"' "$files_sha")},"duracion_seg":${dur:-0},"retencion":{"daily":$KEEP_DAILY,"weekly":$KEEP_WEEKLY,"monthly":$KEEP_MONTHLY}}
JSON
)
  if [[ $DRY_RUN -eq 1 ]]; then echo "$json"; return; fi
  printf '%s\n' "$json" >"$LATEST"
  printf '%s\n' "$json" >>"$HISTORY"
}

# ── En cualquier fallo, deja manifiesto FAIL y log claro, y sale != 0 ──
on_error() {
  local exit_code=$?
  local msg="Fallo en linea ${BASH_LINENO[0]:-?} (exit $exit_code)"
  log "ERROR: $msg"
  write_manifest "FAIL" "$msg" "" 0 "" "" 0 "" "$(( $(date +%s) - START_EPOCH ))" || true
  exit "$exit_code"
}
trap on_error ERR

# ── Retencion: conserva los N mas nuevos de un dir; nunca borra si queda 1 ──
prune_dir() {
  local dir="$1" keep="$2" f count=0
  # Lista por fecha de modificacion desc; borra a partir del (keep+1)
  local files=()
  while IFS= read -r f; do files+=("$f"); done < <(ls -1t "$dir"/*.dump 2>/dev/null || true)
  count=${#files[@]}
  (( count <= keep )) && return 0
  local i
  for (( i=keep; i<count; i++ )); do
    # Nunca borrar si por alguna razon quedaria vacio el dir
    (( count - 1 < 1 )) && break
    if [[ $DRY_RUN -eq 1 ]]; then
      log "DRY: borraria ${files[$i]}"
    else
      rm -f "${files[$i]}" "${files[$i]}.sha256" && count=$((count-1))
      log "Retencion: elimino ${files[$i]}"
    fi
  done
}

log "═══ Backup SGInmo iniciado (dry_run=$DRY_RUN) ═══"

# ── 1) Volcado de la BD (custom format, compatible con RLS) ──
DB_FILE="$BACKUP_ROOT/daily/${PGDATABASE}_${STAMP}.dump"
if [[ $DRY_RUN -eq 1 ]]; then
  log "DRY: pg_dump -> $DB_FILE"
  DB_FILE=""; DB_BYTES=0; DB_SHA=""
else
  PGPASSWORD="${PGPASSWORD:-}" PGOPTIONS='-c app.tenant=-1' \
    pg_dump --enable-row-security -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" \
            -d "$PGDATABASE" -Fc -f "$DB_FILE"
  DB_BYTES="$(stat -c%s "$DB_FILE")"
  DB_SHA="$(sha256sum "$DB_FILE" | awk '{print $1}')"
  echo "$DB_SHA  $(basename "$DB_FILE")" >"$DB_FILE.sha256"
  log "BD OK: $DB_FILE ($DB_BYTES bytes)"
fi

# ── 2) Respaldo del repositorio documental (si existe y tiene contenido) ──
FILES_FILE=""; FILES_BYTES=0; FILES_SHA=""
if [[ -d "$ARCHIVOS_DIR" ]] && [[ -n "$(ls -A "$ARCHIVOS_DIR" 2>/dev/null || true)" ]]; then
  FILES_FILE="$BACKUP_ROOT/daily/archivos_${STAMP}.tgz"
  if [[ $DRY_RUN -eq 1 ]]; then
    log "DRY: tar $ARCHIVOS_DIR -> $FILES_FILE"; FILES_FILE=""
  else
    tar czf "$FILES_FILE" -C "$(dirname "$ARCHIVOS_DIR")" "$(basename "$ARCHIVOS_DIR")"
    FILES_BYTES="$(stat -c%s "$FILES_FILE")"
    FILES_SHA="$(sha256sum "$FILES_FILE" | awk '{print $1}')"
    echo "$FILES_SHA  $(basename "$FILES_FILE")" >"$FILES_FILE.sha256"
    log "Archivos OK: $FILES_FILE ($FILES_BYTES bytes)"
  fi
else
  log "Sin repositorio documental en $ARCHIVOS_DIR (aun no configurado); se omite"
fi

# ── 3) Promocion a semanal (domingo) y mensual (dia 01) por copia ──
if [[ $DRY_RUN -eq 0 && -n "$DB_FILE" ]]; then
  if [[ "$DOW" == "7" ]]; then
    cp -f "$DB_FILE" "$BACKUP_ROOT/weekly/" && cp -f "$DB_FILE.sha256" "$BACKUP_ROOT/weekly/" 2>/dev/null || true
    log "Promocion semanal (domingo)"
  fi
  if [[ "$DOM" == "01" ]]; then
    cp -f "$DB_FILE" "$BACKUP_ROOT/monthly/" && cp -f "$DB_FILE.sha256" "$BACKUP_ROOT/monthly/" 2>/dev/null || true
    log "Promocion mensual (dia 01)"
  fi
fi

# ── 4) Retencion por clase (nunca borra el ultimo valido) ──
prune_dir "$BACKUP_ROOT/daily"   "$KEEP_DAILY"
prune_dir "$BACKUP_ROOT/weekly"  "$KEEP_WEEKLY"
prune_dir "$BACKUP_ROOT/monthly" "$KEEP_MONTHLY"

# ── 5) Manifiesto OK ──
DUR="$(( $(date +%s) - START_EPOCH ))"
write_manifest "OK" "" "$DB_FILE" "$DB_BYTES" "$DB_SHA" "$FILES_FILE" "$FILES_BYTES" "$FILES_SHA" "$DUR"
log "═══ Backup OK en ${DUR}s ═══"
