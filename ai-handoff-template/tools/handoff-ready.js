#!/usr/bin/env node
import { existsSync, readFileSync, writeFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { config as loadEnv } from 'dotenv';
import mysql from 'mysql2/promise';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..');
const HANDOFF = join(ROOT, '.ai-handoff');
const REGISTRY = join(HANDOFF, 'requirements', 'registry.jsonl');
const TO_CODEX = join(HANDOFF, 'to_codex.md');
const TO_CLAUDE = join(HANDOFF, 'to_claude.md');
const MAX_BATCH = Number.parseInt(process.env.HANDOFF_MAX_BATCH ?? '5', 10);
const BATCH_OVERRIDE_TOKEN = 'BATCH_GRANDE_APROBADO_POR_USUARIO';
const OBSERVATIONS_OPTIONAL = process.env.HANDOFF_OBSERVATIONS_OPTIONAL === '1';

const checkOnly = process.argv.includes('--check-only');
const reqs = process.argv
  .slice(2)
  .filter(arg => arg !== '--check-only')
  .map(arg => arg.trim())
  .filter(Boolean);
const issues = [];

function utcNow() {
  return new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');
}

function reqNumber(req) {
  const match = String(req ?? '').match(/^REQ-(\d+)$/);
  return match ? Number(match[1]) : Number.POSITIVE_INFINITY;
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

function uncheckedChecklistItems(req, raw) {
  if (!raw) return [`${req}: falta preaudit-checklist.md completado por Claude`];
  return raw
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(line => /^[-*]\s+\[\s\]\s+/.test(line))
    .map(line => `${req}: checklist preauditoria pendiente: ${line.replace(/^[-*]\s+\[\s\]\s+/, '')}`);
}

function checkReadyReq(req) {
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
    return;
  }
  if (!/Manifiesto\s+M[iÃ­]nimo\s+Para\s+Codex/i.test(implementation)) {
    issues.push(`${req}: claude-implementation.md no incluye Manifiesto Minimo Para Codex`);
  }
  if (!/Comandos?\s+(probados|verificados)|npm\s+run|tsc|pytest|EXIT\s*:\s*0/i.test(implementation)) {
    issues.push(`${req}: claude-implementation.md no documenta comandos probados`);
  }

  const checklist = readReqFile(req, 'preaudit-checklist.md');
  issues.push(...uncheckedChecklistItems(req, checklist));
  if (checklist && !/Responsable:\s*Claude/i.test(checklist)) {
    issues.push(`${req}: preaudit-checklist.md no tiene firma Responsable: Claude`);
  }
}

function parseRegistry() {
  if (!existsSync(REGISTRY)) {
    issues.push('registry.jsonl: no existe');
    return [];
  }
  return readFileSync(REGISTRY, 'utf8')
    .split(/\r?\n/)
    .map((line, index) => ({ line: line.trim(), index }))
    .filter(item => item.line)
    .map(item => {
      try {
        return JSON.parse(item.line);
      } catch (error) {
        issues.push(`registry.jsonl:${item.index + 1}: JSON invalido (${error.message})`);
        return null;
      }
    })
    .filter(Boolean);
}

function writeRegistry(rows) {
  writeFileSync(REGISTRY, rows.map(row => JSON.stringify(row)).join('\n') + '\n', 'utf8');
}

function parseMailboxReqs(file) {
  if (!existsSync(file)) return [];
  const raw = readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
  const match = raw.match(/^REQ:\s*(.*)$/m);
  if (!match) return [];
  return match[1].split(',').map(item => item.trim()).filter(Boolean);
}

function writeToCodex(targetReqs, ts, message) {
  const content = `ESTADO: LISTO_PARA_REVISION
REQ: ${targetReqs.join(',')}
TS: ${ts}
AGENTE: handoff-ready
MENSAJE: ${message}
---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

## Reenvio seguro

Batch generado por \`npm run handoff:ready\`.
`;
  writeFileSync(TO_CODEX, content, 'utf8');
}

function writeToClaudeIfNeeded(rows, targetReqs, ts) {
  const pending = rows
    .filter(row => !targetReqs.includes(row.req))
    .filter(row => row.estado === 'REQUIERE_CAMBIOS' || row.estado === 'PRECHECK_FAIL')
    .map(row => row.req)
    .filter(Boolean)
    .sort((a, b) => reqNumber(a) - reqNumber(b) || String(a).localeCompare(String(b)));

  if (!pending.length) return;
  const current = parseMailboxReqs(TO_CLAUDE);
  const same = pending.length === current.length && pending.every((req, index) => req === current[index]);
  if (same) return;

  const content = `ESTADO: REQUIERE_CAMBIOS
REQ: ${pending.join(', ')}
TS: ${ts}
AGENTE: handoff-ready
MENSAJE: Pendientes restantes para Claude despues de reenviar ${targetReqs.join(', ')}.
---
# Estados validos (Codex escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO

## Pendientes restantes

Claude debe seguir corrigiendo en orden numerico: ${pending.join(', ')}.
`;
  writeFileSync(TO_CLAUDE, content, 'utf8');
}

async function pendingObservationsByReq(targetReqs) {
  loadEnv({ path: join(ROOT, '.env') });
  const hasDbConfig = process.env.DB_HOST && process.env.DB_USER && process.env.DB_NAME;
  if (!hasDbConfig) {
    if (OBSERVATIONS_OPTIONAL) return new Map();
    throw new Error('faltan variables DB_HOST, DB_USER o DB_NAME para consultar AUDITORIA_OBSERVACION');
  }

  const connection = await mysql.createConnection({
    host: process.env.DB_HOST,
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
  });

  try {
    const [rows] = await connection.query(
      `SELECT Req, Categoria, Subcategoria, Severidad, Archivo, Resumen
       FROM AUDITORIA_OBSERVACION
       WHERE Estado = 'pendiente' AND Req IN (?)
       ORDER BY Req, Severidad DESC, Categoria, Subcategoria`,
      [targetReqs],
    );
    const byReq = new Map();
    for (const row of rows) {
      const list = byReq.get(row.Req) ?? [];
      list.push(row);
      byReq.set(row.Req, list);
    }
    return byReq;
  } finally {
    await connection.end();
  }
}

async function main() {
  if (!reqs.length) {
    console.error('Uso: npm run handoff:ready -- REQ-XXXX [REQ-YYYY]');
    process.exit(1);
  }

  const uniqueReqs = [...new Set(reqs)]
    .sort((a, b) => reqNumber(a) - reqNumber(b) || String(a).localeCompare(String(b)));

  if (uniqueReqs.length > MAX_BATCH && !process.env.HANDOFF_BATCH_OVERRIDE?.includes(BATCH_OVERRIDE_TOKEN)) {
    issues.push(`batch de ${uniqueReqs.length} REQs supera el maximo ${MAX_BATCH}; usar HANDOFF_BATCH_OVERRIDE=${BATCH_OVERRIDE_TOKEN} solo con aprobacion explicita del usuario`);
  }

  const registry = parseRegistry();
  const byReq = new Map(registry.map(row => [row.req, row]));
  for (const req of uniqueReqs) {
    if (!byReq.has(req)) {
      issues.push(`${req}: no existe en registry.jsonl`);
      continue;
    }
    checkReadyReq(req);
  }

  try {
    const pendingByReq = await pendingObservationsByReq(uniqueReqs);
    for (const req of uniqueReqs) {
      const pending = pendingByReq.get(req) ?? [];
      for (const obs of pending) {
        const sub = obs.Subcategoria ? `/${obs.Subcategoria}` : '';
        const file = obs.Archivo ? ` (${obs.Archivo})` : '';
        issues.push(`${req}: observacion pendiente en AUDITORIA_OBSERVACION ${obs.Categoria}${sub} [${obs.Severidad}]${file}`);
      }
    }
  } catch (error) {
    issues.push(`AUDITORIA_OBSERVACION: no se pudo consultar observaciones pendientes (${error.message})`);
  }

  const minTarget = Math.min(...uniqueReqs.map(reqNumber));
  const lowerPending = registry
    .filter(row => row.estado === 'REQUIERE_CAMBIOS' || row.estado === 'PRECHECK_FAIL')
    .filter(row => reqNumber(row.req) < minTarget)
    .map(row => row.req)
    .filter(req => !uniqueReqs.includes(req));
  if (lowerPending.length) {
    issues.push(`prioridad: hay REQs menores pendientes antes del batch: ${lowerPending.join(', ')}`);
  }

  if (issues.length) {
    console.error(checkOnly ? 'HANDOFF READY CHECK: FAIL' : 'HANDOFF READY: FAIL');
    for (const issue of issues) console.error(`- ${issue}`);
    process.exit(1);
  }

  if (checkOnly) {
    console.log(`HANDOFF READY CHECK: OK ${uniqueReqs.join(', ')}`);
    process.exit(0);
  }

  const ts = utcNow();
  for (const req of uniqueReqs) {
    const row = byReq.get(req);
    row.estado = 'LISTO_PARA_REVISION';
    row.ts_actualizado = ts;
    delete row.precheck;
  }

  writeRegistry(registry);
  writeToCodex(uniqueReqs, ts, `REQs validados y reenviados a Codex: ${uniqueReqs.join(', ')}.`);
  writeToClaudeIfNeeded(registry, uniqueReqs, ts);

  console.log(`HANDOFF READY: OK ${uniqueReqs.join(', ')}`);
}

main().catch(error => {
  console.error('HANDOFF READY: FAIL');
  console.error(`- ${error.message}`);
  process.exit(1);
});
