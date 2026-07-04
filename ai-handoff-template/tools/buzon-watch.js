#!/usr/bin/env node
import { readFileSync, writeFileSync, existsSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { spawnSync } from 'child_process';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..');
const HANDOFF = join(ROOT, '.ai-handoff');
const STATE_FILE = join(HANDOFF, '.watcher-state.json');
const AUDIT_STATE_FILE = join(HANDOFF, '.codex-audit-state.json');
const INTERVAL_MS = parseInt(process.env.BUZON_INTERVAL_SECS ?? '30', 10) * 1000;
const MAX_BATCH = Number.parseInt(process.env.HANDOFF_MAX_BATCH ?? '5', 10);
const RUN_HANDOFF_CHECK_IN_WATCHER = process.env.WATCHER_RUN_HANDOFF_CHECK === '1';

const BUZON_FILES = {
  to_claude: join(HANDOFF, 'to_claude.md'),
  to_codex: join(HANDOFF, 'to_codex.md'),
};
const REGISTRY_FILE = join(HANDOFF, 'requirements', 'registry.jsonl');
const HANDOFF_CHECK = join(__dirname, 'handoff-check.js');
const HANDOFF_READY = join(__dirname, 'handoff-ready.js');

const ACTIONABLE = {
  to_claude: {
    APROBADO_POR_CODEX: req => `[CLAUDE]  ${req} aprobado por Codex. Leer to_claude.md y actuar.`,
    REQUIERE_CAMBIOS: req => `[CLAUDE]  ${req} requiere correcciones. Leer to_claude.md y codex-review.md.`,
    BLOQUEADO_POR_USUARIO: req => `[USUARIO] Decision requerida para ${req}.`,
    ESPERA_USUARIO: req => `[USUARIO] Respuesta requerida para ${req}.`,
    REQ_NUEVO: req => `[CLAUDE]  Nuevo REQ creado: ${req}. Leer to_claude.md y req.md.`,
  },
  to_codex: {
    LISTO_PARA_REVISION: req => `[CODEX]   ${req} listo para auditoria. Leer to_codex.md.`,
    BLOQUEADO_POR_USUARIO: req => `[USUARIO] Decision requerida para ${req}.`,
    ESPERA_USUARIO: req => `[USUARIO] Respuesta requerida para ${req}.`,
  },
};

function hhmm() {
  return new Date().toLocaleTimeString('es-PY', {
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  });
}

function parseBuzon(fileKey) {
  const filePath = BUZON_FILES[fileKey];
  if (!existsSync(filePath)) return { missing: true };
  const raw = readFileSync(filePath, 'utf8').replace(/^﻿/, '');
  const fields = {};
  for (const line of raw.split('\n')) {
    if (line.startsWith('#') || line.startsWith('---')) break;
    const m = line.match(/^([A-Z_]+):\s*(.*)$/);
    if (m) fields[m[1]] = m[2].trim();
  }
  if (!fields.ESTADO) return { error: `Formato incompleto en ${fileKey}.md` };
  return fields;
}

function reqNumber(req) {
  const m = String(req ?? '').match(/^REQ-(\d+)$/);
  return m ? Number(m[1]) : Number.POSITIVE_INFINITY;
}

function parseReqList(reqField) {
  return String(reqField ?? '')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean);
}

function readReqFile(req, fileName) {
  const file = join(HANDOFF, 'requirements', req, fileName);
  if (!existsSync(file)) return null;
  return readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
}

function hasMeaningfulBody(raw) {
  if (!raw) return false;
  const meaningful = raw
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean)
    .filter(line => !line.startsWith('#'))
    .filter(line => !/^[-*]\s*\[\s*\]\s*Prueba manual$/i.test(line));
  return meaningful.length > 0;
}

function uncheckedCriteria(req, raw) {
  if (!raw) return [];
  const lines = raw.split(/\r?\n/);
  const pending = [];
  let inCriteria = false;
  for (const line of lines) {
    if (/^##\s+Criterios\b/i.test(line)) {
      inCriteria = true;
      continue;
    }
    if (inCriteria && /^##\s+/.test(line)) break;
    if (inCriteria && /^[-*]\s+\[\s\]\s+/.test(line.trim())) {
      pending.push(line.trim().replace(/^[-*]\s+\[\s\]\s+/, ''));
    }
  }
  return pending.map(item => `${req}: criterio pendiente en req.md: ${item}`);
}

function checkReadyReq(req) {
  const issues = [];
  const reqMd = readReqFile(req, 'req.md');
  if (!reqMd) {
    issues.push(`${req}: falta req.md`);
  } else {
    issues.push(...uncheckedCriteria(req, reqMd));
  }

  const testPlan = readReqFile(req, 'test-plan.md');
  if (!hasMeaningfulBody(testPlan)) {
    issues.push(`${req}: test-plan.md vacio o sin evidencia de pruebas`);
  }

  const implementation = readReqFile(req, 'claude-implementation.md');
  if (!hasMeaningfulBody(implementation)) {
    issues.push(`${req}: claude-implementation.md vacio`);
    return issues;
  }
  if (!/Manifiesto\s+M[iÃ­]nimo\s+Para\s+Codex/i.test(implementation)) {
    issues.push(`${req}: claude-implementation.md no incluye Manifiesto Minimo Para Codex`);
  }
  if (!/Comandos?\s+(probados|verificados)|npm\s+run|tsc|pytest|EXIT\s*:\s*0/i.test(implementation)) {
    issues.push(`${req}: claude-implementation.md no documenta comandos probados`);
  }
  return issues;
}

function sameReqSet(a, b) {
  const aa = [...new Set(a)].sort();
  const bb = [...new Set(b)].sort();
  return aa.length === bb.length && aa.every((v, i) => v === bb[i]);
}

function readRegistryRows() {
  if (!existsSync(REGISTRY_FILE)) return [];
  const rows = [];
  const invalid = [];
  readFileSync(REGISTRY_FILE, 'utf8')
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean)
    .forEach((line, index) => {
      try {
        rows.push(JSON.parse(line));
      } catch (error) {
        invalid.push({ line: index + 1, error: error.message });
      }
    });
  if (invalid.length) {
    console.warn(`[${hhmm()}] ADVERTENCIA: registry.jsonl tiene ${invalid.length} linea(s) JSON invalida(s). Ejecutar npm run handoff:check.`);
  }
  return rows;
}

function writeRegistryRows(rows) {
  const content = rows.map(row => JSON.stringify(row)).join('\n') + '\n';
  writeFileSync(REGISTRY_FILE, content, 'utf8');
}

function readRegistryBacklog() {
  return readRegistryRows()
    .filter(row => row?.estado === 'REQUIERE_CAMBIOS' || row?.estado === 'PRECHECK_FAIL')
    .map(row => row.req)
    .filter(Boolean)
    .sort((a, b) => reqNumber(a) - reqNumber(b) || String(a).localeCompare(String(b)));
}

function readRegistryClaudeWork() {
  return readRegistryRows()
    .filter(row => row?.estado === 'REQUIERE_CAMBIOS' || row?.estado === 'PRECHECK_FAIL')
    .map(row => row.req)
    .filter(Boolean)
    .sort((a, b) => reqNumber(a) - reqNumber(b) || String(a).localeCompare(String(b)));
}

function readRegistryReadyForCodex() {
  return readRegistryRows()
    .filter(row => row?.estado === 'LISTO_PARA_REVISION')
    .map(row => row.req)
    .filter(Boolean)
    .sort((a, b) => reqNumber(a) - reqNumber(b) || String(a).localeCompare(String(b)));
}

function utcNow() {
  return new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');
}

function reconcileBacklog() {
  const claude = parseBuzon('to_claude');
  if (claude.missing || claude.error) return false;

  const reqs = readRegistryBacklog();
  if (!reqs.length) return false;
  const currentReqs = parseReqList(claude.REQ);
  if (claude.ESTADO === 'REQUIERE_CAMBIOS' && sameReqSet(currentReqs, reqs)) return false;

  const ts = utcNow();
  const content = `ESTADO: REQUIERE_CAMBIOS
REQ: ${reqs.join(', ')}
TS: ${ts}
AGENTE: watcher
MENSAJE: Reconciliacion automatica: registry.jsonl es la fuente de verdad y aun tiene REQs pendientes para Claude. Claude debe corregirlos antes de continuar con REQs mayores.
---
# Estados validos (Codex escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO

## Reconciliacion automatica

Estos REQs siguen pendientes para Claude en \`registry.jsonl\`: ${reqs.join(', ')}.

Claude debe corregir en orden numerico, actualizar la documentacion de entrega y luego usar \`npm run handoff:ready -- REQ-XXXX\` para reenviar solo el menor REQ pendiente que quede listo.
`;
  writeFileSync(BUZON_FILES.to_claude, content, 'utf8');
  return true;
}

function reconcileClaudeWork() {
  const claude = parseBuzon('to_claude');
  if (claude.missing || claude.error) return false;

  const reqs = readRegistryClaudeWork();
  if (!reqs.length) return false;
  const currentReqs = parseReqList(claude.REQ);
  if (claude.ESTADO === 'REQUIERE_CAMBIOS' && sameReqSet(currentReqs, reqs)) return false;

  const ts = utcNow();
  const content = `ESTADO: REQUIERE_CAMBIOS
REQ: ${reqs.join(', ')}
TS: ${ts}
AGENTE: watcher
MENSAJE: Reconciliacion automatica: registry.jsonl es la fuente de verdad y aun tiene REQs que requieren accion de Claude.
---
# Estados validos (Codex escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO

## Reconciliacion automatica

Estos REQs siguen del lado de Claude en \`registry.jsonl\`: ${reqs.join(', ')}.

Claude debe corregirlos en orden numerico. Si estan en \`REQUIERE_CAMBIOS\`, leer el \`codex-review.md\` de cada REQ. Si estan en \`PRECHECK_FAIL\`, ejecutar \`npm run handoff:check\` y corregir la evidencia/documentacion antes de reenviar a Codex.
`;
  writeFileSync(BUZON_FILES.to_claude, content, 'utf8');
  return true;
}

function reconcileCodexWork() {
  const codex = parseBuzon('to_codex');
  if (codex.missing || codex.error) return false;
  if (codex.ESTADO === 'LISTO_PARA_REVISION') return false;
  if (readRegistryClaudeWork().length) return false;

  const ready = readRegistryReadyForCodex();
  if (!ready.length) return false;

  const reqs = ready.slice(0, MAX_BATCH);
  const ts = utcNow();
  const content = `ESTADO: LISTO_PARA_REVISION
REQ: ${reqs.join(',')}
TS: ${ts}
AGENTE: watcher
MENSAJE: Reconciliacion automatica: registry.jsonl tiene REQs listos para Codex.
---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

## Reconciliacion automatica

Estos REQs estan en \`LISTO_PARA_REVISION\` en \`registry.jsonl\` y no habia senal activa para Codex: ${reqs.join(', ')}.
`;
  writeFileSync(BUZON_FILES.to_codex, content, 'utf8');
  return true;
}

function markPrecheckFail(reqs, message) {
  const wanted = new Set(reqs);
  if (!wanted.size) return false;

  const rows = readRegistryRows();
  const ts = utcNow();
  let changed = false;
  for (const row of rows) {
    if (wanted.has(row?.req) && row.estado === 'LISTO_PARA_REVISION') {
      row.estado = 'PRECHECK_FAIL';
      row.ts_actualizado = ts;
      row.precheck = message;
      changed = true;
    }
  }
  if (changed) writeRegistryRows(rows);
  if (!changed) return false;

  const content = `ESTADO: REQUIERE_CAMBIOS
REQ: ${reqs.join(', ')}
TS: ${ts}
AGENTE: watcher
MENSAJE: Precheck fallido antes de Codex. Ejecutar npm run handoff:check y corregir la evidencia/documentacion. ${message}
---
# Estados validos (Codex escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO

## Precheck fallido

Codex no debe auditar este batch todavia. Claude debe ejecutar \`npm run handoff:check\`, corregir los puntos reportados y recien entonces volver a poner \`to_codex.md\` en \`LISTO_PARA_REVISION\`.
`;
  writeFileSync(BUZON_FILES.to_claude, content, 'utf8');
  return changed;
}

function key(fields) {
  return `${fields.REQ ?? '-'}:${fields.ESTADO}:${fields.TS ?? '-'}`;
}

function auditKey(fields) {
  const req = (fields.REQ ?? '-')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
    .join(',');
  return `${req}|${fields.ESTADO ?? '-'}|${fields.TS ?? '-'}`;
}

function readAuditState() {
  try {
    const parsed = JSON.parse(readFileSync(AUDIT_STATE_FILE, 'utf8'));
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function isAlreadyAudited(fields) {
  if (fields.ESTADO !== 'LISTO_PARA_REVISION') return false;
  const reviewed = readAuditState().reviewed ?? {};
  return Boolean(reviewed[auditKey(fields)]);
}

function precheckReadyForCodex(reqs) {
  const rows = readRegistryRows();
  const ready = new Set(rows.filter(row => row?.estado === 'LISTO_PARA_REVISION').map(row => row.req));
  const minReq = Math.min(...reqs.map(reqNumber));
  const lowerPending = rows
    .filter(row => row?.estado === 'REQUIERE_CAMBIOS' || row?.estado === 'PRECHECK_FAIL')
    .filter(row => reqNumber(row.req) < minReq)
    .map(row => row.req);
  const issues = [];
  if (lowerPending.length) {
    issues.push(`Prioridad: hay REQs menores pendientes (${lowerPending.join(', ')})`);
  }
  for (const req of reqs) {
    if (!ready.has(req)) {
      issues.push(`${req}: registry no esta LISTO_PARA_REVISION`);
      continue;
    }
    issues.push(...checkReadyReq(req));
  }
  if (!issues.length) return { ok: true };
  return { ok: false, message: issues.slice(0, 3).join(' ') };
}

function runHandoffCheck() {
  const result = spawnSync(process.execPath, [HANDOFF_CHECK], {
    cwd: ROOT,
    encoding: 'utf8',
  });
  if (result.status === 0) return { ok: true };
  const output = `${result.error?.message ?? ''}\n${result.stderr ?? ''}\n${result.stdout ?? ''}`
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(line => line && !line.startsWith('>') && !line.includes('dotenv'))
    .slice(0, 8)
    .join(' ');
  return { ok: false, message: output || 'handoff-check fallo' };
}

function readState() {
  try { return JSON.parse(readFileSync(STATE_FILE, 'utf8')); } catch { return {}; }
}

function writeState(state) {
  writeFileSync(STATE_FILE, JSON.stringify(state, null, 2) + '\n');
}

function check(fileKey, state, updateState) {
  const fields = parseBuzon(fileKey);
  if (fields.missing) return false;
  if (fields.error) {
    console.warn(`[${hhmm()}] ADVERTENCIA: ${fields.error}`);
    return false;
  }
  const msgFn = ACTIONABLE[fileKey]?.[fields.ESTADO];
  if (!msgFn) return false;
  const k = key(fields);
  if (fileKey === 'to_codex' && isAlreadyAudited(fields)) return false;
  if (state[fileKey] === k) {
    if (!(fileKey === 'to_codex' && fields.ESTADO === 'LISTO_PARA_REVISION')) return false;
  }
  if (fileKey === 'to_codex' && fields.ESTADO === 'LISTO_PARA_REVISION') {
    const backlog = readRegistryBacklog();
    const codexReqs = parseReqList(fields.REQ);
    const minBacklog = Math.min(...backlog.map(reqNumber));
    const minCodex = Math.min(...codexReqs.map(reqNumber));
    const codexContainsBacklog = backlog.some(req => codexReqs.includes(req));
    const localPrecheck = precheckReadyForCodex(codexReqs);
    const fullPrecheck = localPrecheck.ok && RUN_HANDOFF_CHECK_IN_WATCHER ? runHandoffCheck() : localPrecheck;
    if (!fullPrecheck.ok) {
      markPrecheckFail(codexReqs, fullPrecheck.message);
      console.log(`[${hhmm()}] [CLAUDE]  Precheck fallido para ${fields.REQ ?? '?'}. Ejecutar npm run handoff:check. ${fullPrecheck.message}`);
      return true;
    }
    if (backlog.length && minBacklog < minCodex && !codexContainsBacklog) {
      console.log(`[${hhmm()}] [CLAUDE]  Hay REQs menores pendientes (${backlog.join(', ')}). Diferida auditoria de ${fields.REQ ?? '?'} hasta corregirlos.`);
      return true;
    }
  }
  console.log(`[${hhmm()}] ${msgFn(fields.REQ ?? '?')}`);
  if (updateState) {
    state[fileKey] = k;
    writeState(state);
  }
  return true;
}

function tick(once) {
  reconcileBacklog();
  reconcileClaudeWork();
  reconcileCodexWork();
  const state = readState();
  let acted = false;
  for (const fileKey of Object.keys(BUZON_FILES)) {
    if (check(fileKey, state, !once)) acted = true;
  }
  if (once) {
    if (!acted) console.log('[buzon] Sin senales accionables en los buzones.');
    process.exit(0);
  }
}

const once = process.argv.includes('--once');
tick(once);

if (!once) {
  console.log(`[buzon] Monitoreando cada ${INTERVAL_MS / 1000}s. Ctrl+C para detener.`);
  setInterval(() => tick(false), INTERVAL_MS);
}
