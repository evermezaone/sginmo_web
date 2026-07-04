#!/usr/bin/env node
import { existsSync, readFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { config as loadEnv } from 'dotenv';
import mysql from 'mysql2/promise';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..');
const HANDOFF = join(ROOT, '.ai-handoff');
const REGISTRY = join(HANDOFF, 'requirements', 'registry.jsonl');
const MAX_BATCH = Number.parseInt(process.env.HANDOFF_MAX_BATCH ?? '5', 10);
const BATCH_OVERRIDE_TOKEN = 'BATCH_GRANDE_APROBADO_POR_USUARIO';
const OBSERVATIONS_OPTIONAL = process.env.HANDOFF_OBSERVATIONS_OPTIONAL === '1';

const issues = [];

function parseMailbox(name) {
  const file = join(HANDOFF, name);
  if (!existsSync(file)) {
    issues.push(`${name}: no existe`);
    return {};
  }
  const raw = readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
  const fields = {};
  for (const line of raw.split(/\r?\n/)) {
    if (line.startsWith('#') || line.startsWith('---')) break;
    const m = line.match(/^([A-Z_]+):\s*(.*)$/);
    if (m) fields[m[1]] = m[2].trim();
  }
  if (!fields.ESTADO) issues.push(`${name}: falta ESTADO`);
  return fields;
}

function reqNumber(req) {
  const m = String(req ?? '').match(/^REQ-(\d+)$/);
  return m ? Number(m[1]) : Number.POSITIVE_INFINITY;
}

function reqs(value) {
  return String(value ?? '')
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
  if (!/Manifiesto\s+M[ií]nimo\s+Para\s+Codex/i.test(implementation)) {
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
  const rows = [];
  readFileSync(REGISTRY, 'utf8')
    .split(/\r?\n/)
    .forEach((line, index) => {
      const trimmed = line.trim();
      if (!trimmed) return;
      try {
        rows.push(JSON.parse(trimmed));
      } catch (error) {
        issues.push(`registry.jsonl:${index + 1}: JSON invalido (${error.message})`);
      }
    });
  return rows;
}

function checkEvents(req) {
  const file = join(HANDOFF, 'requirements', req, 'events.jsonl');
  if (!existsSync(file)) {
    issues.push(`${req}: falta events.jsonl`);
    return;
  }
  readFileSync(file, 'utf8')
    .split(/\r?\n/)
    .forEach((line, index) => {
      const trimmed = line.trim();
      if (!trimmed) return;
      try {
        JSON.parse(trimmed);
      } catch (error) {
        issues.push(`${req}/events.jsonl:${index + 1}: JSON invalido (${error.message})`);
      }
    });
}

async function checkPendingObservations(targetReqs) {
  if (!targetReqs.length) return;
  loadEnv({ path: join(ROOT, '.env') });
  const hasDbConfig = process.env.DB_HOST && process.env.DB_USER && process.env.DB_NAME;
  if (!hasDbConfig) {
    if (OBSERVATIONS_OPTIONAL) return;
    issues.push('AUDITORIA_OBSERVACION: faltan variables DB_HOST, DB_USER o DB_NAME');
    return;
  }

  let connection;
  try {
    connection = await mysql.createConnection({
      host: process.env.DB_HOST,
      port: Number(process.env.DB_PORT || 3306),
      user: process.env.DB_USER,
      password: process.env.DB_PASS,
      database: process.env.DB_NAME,
    });
    const projectCode = process.env.PROJECT_CODE;
    if (!projectCode) {
      issues.push('AUDITORIA_OBSERVACION: falta PROJECT_CODE en .env');
      return;
    }
    const [rows] = await connection.query(
      `SELECT r.Codigo AS Req, o.Categoria, o.Subcategoria, o.Severidad, o.Archivo
       FROM AUDITORIA_OBSERVACION o
       JOIN REQ r ON r.IdReq = o.IdReq
       JOIN PROYECTO p ON p.IdProyecto = r.IdProyecto
       WHERE o.Estado = 'abierta' AND p.Codigo = ? AND r.Codigo IN (?)
       ORDER BY r.Codigo, o.Severidad DESC, o.Categoria, o.Subcategoria`,
      [projectCode, targetReqs],
    );
    for (const row of rows) {
      const sub = row.Subcategoria ? `/${row.Subcategoria}` : '';
      const file = row.Archivo ? ` (${row.Archivo})` : '';
      issues.push(`${row.Req}: observacion ABIERTA en AUDITORIA_OBSERVACION ${row.Categoria}${sub} [${row.Severidad}]${file}`);
    }
  } catch (error) {
    issues.push(`AUDITORIA_OBSERVACION: no se pudo consultar observaciones pendientes (${error.message})`);
  } finally {
    if (connection) await connection.end();
  }
}

async function main() {
const registry = parseRegistry();
for (const row of registry) {
  if (row?.req) checkEvents(row.req);
}

const backlog = registry
  .filter(row => row?.estado === 'REQUIERE_CAMBIOS')
  .map(row => row.req)
  .filter(Boolean)
  .sort((a, b) => reqNumber(a) - reqNumber(b) || String(a).localeCompare(String(b)));

const precheckFail = registry
  .filter(row => row?.estado === 'PRECHECK_FAIL')
  .map(row => row.req)
  .filter(Boolean)
  .sort((a, b) => reqNumber(a) - reqNumber(b) || String(a).localeCompare(String(b)));

const claudeWork = [...new Set([...backlog, ...precheckFail])]
  .sort((a, b) => reqNumber(a) - reqNumber(b) || String(a).localeCompare(String(b)));

const listo = registry
  .filter(row => row?.estado === 'LISTO_PARA_REVISION')
  .map(row => row.req)
  .filter(Boolean)
  .sort((a, b) => reqNumber(a) - reqNumber(b) || String(a).localeCompare(String(b)));

const toClaude = parseMailbox('to_claude.md');
const toCodex = parseMailbox('to_codex.md');
const codexReqs = reqs(toCodex.REQ);

if (toCodex.ESTADO === 'LISTO_PARA_REVISION') {
  if (codexReqs.length > MAX_BATCH && !String(toCodex.MENSAJE ?? '').includes(BATCH_OVERRIDE_TOKEN)) {
    issues.push(`to_codex.md: batch de ${codexReqs.length} REQs supera el maximo ${MAX_BATCH}; dividir o incluir ${BATCH_OVERRIDE_TOKEN} en MENSAJE con aprobacion explicita del usuario`);
  }

  const registryReady = new Set(listo);
  for (const req of codexReqs) {
    if (!registryReady.has(req)) {
      issues.push(`${req}: esta en to_codex.md pero registry no esta LISTO_PARA_REVISION`);
      continue;
    }
    checkReadyReq(req);
  }
  await checkPendingObservations(codexReqs);
}

if (claudeWork.length) {
  const claudeReqs = reqs(toClaude.REQ);
  const missing = claudeWork.filter(req => !claudeReqs.includes(req));
  if (toClaude.ESTADO !== 'REQUIERE_CAMBIOS') {
    issues.push(`to_claude.md: debe estar en REQUIERE_CAMBIOS porque registry tiene trabajo para Claude: ${claudeWork.join(', ')}`);
  } else if (missing.length) {
    issues.push(`to_claude.md: no notifica trabajo pendiente para Claude: ${missing.join(', ')}`);
  }

  for (const req of backlog) {
    const review = join(HANDOFF, 'requirements', req, 'codex-review.md');
    if (!existsSync(review) || !readFileSync(review, 'utf8').trim()) {
      issues.push(`${req}: falta codex-review.md con la auditoria`);
    }
  }

  const minClaudeWork = Math.min(...claudeWork.map(reqNumber));
  const higherReady = listo.filter(req => reqNumber(req) > minClaudeWork);
  if (higherReady.length) {
    issues.push(`Prioridad: hay REQs mayores LISTO_PARA_REVISION (${higherReady.join(', ')}) mientras ${claudeWork[0]} sigue pendiente para Claude`);
  }

  if (toCodex.ESTADO === 'LISTO_PARA_REVISION') {
    const minCodex = Math.min(...codexReqs.map(reqNumber));
    const containsClaudeWork = claudeWork.some(req => codexReqs.includes(req));
    if (minClaudeWork < minCodex && !containsClaudeWork) {
      issues.push(`to_codex.md: propone auditar ${codexReqs.join(', ')} pero hay trabajo menor pendiente para Claude: ${claudeWork.join(', ')}`);
    }
  }
}

if (issues.length) {
  console.error('HANDOFF CHECK: FAIL');
  for (const issue of issues) console.error(`- ${issue}`);
  process.exit(1);
}

console.log('HANDOFF CHECK: OK');
if (backlog.length) console.log(`REQUIERE_CAMBIOS: ${backlog.join(', ')}`);
if (precheckFail.length) console.log(`PRECHECK_FAIL: ${precheckFail.join(', ')}`);
if (listo.length) console.log(`LISTO_PARA_REVISION: ${listo.join(', ')}`);
}

main().catch(error => {
  console.error('HANDOFF CHECK: FAIL');
  console.error(`- ${error.message}`);
  process.exit(1);
});
