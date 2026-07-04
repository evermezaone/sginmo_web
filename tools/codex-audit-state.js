#!/usr/bin/env node
import { existsSync, readFileSync, writeFileSync } from 'fs';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const HANDOFF = join(__dirname, '..', '.ai-handoff');
const TO_CODEX = join(HANDOFF, 'to_codex.md');
const STATE_FILE = join(HANDOFF, '.codex-audit-state.json');

function parseBuzon(filePath) {
  if (!existsSync(filePath)) throw new Error(`No existe ${filePath}`);
  const raw = readFileSync(filePath, 'utf8').replace(/^\uFEFF/, '');
  const fields = {};
  for (const line of raw.split('\n')) {
    if (line.startsWith('#') || line.startsWith('---')) break;
    const match = line.match(/^([A-Z_]+):\s*(.*)$/);
    if (match) fields[match[1]] = match[2].trim();
  }
  if (!fields.ESTADO) throw new Error('to_codex.md no tiene ESTADO');
  return fields;
}

function auditKey(fields) {
  const req = (fields.REQ ?? '-')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
    .join(',');
  return `${req}|${fields.ESTADO ?? '-'}|${fields.TS ?? '-'}`;
}

function readState() {
  try {
    const parsed = JSON.parse(readFileSync(STATE_FILE, 'utf8'));
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function writeState(state) {
  writeFileSync(STATE_FILE, JSON.stringify(state, null, 2) + '\n', 'utf8');
}

const command = process.argv[2] ?? 'check-current';
const fields = parseBuzon(TO_CODEX);
const key = auditKey(fields);
const state = readState();
state.reviewed ??= {};

if (command === 'check-current') {
  console.log(state.reviewed[key] ? 'already-reviewed' : 'new-signal');
  process.exit(0);
}

if (command === 'mark-current') {
  state.reviewed[key] = {
    req: fields.REQ ?? '-',
    estado: fields.ESTADO ?? '-',
    signalTs: fields.TS ?? '-',
    reviewedAt: new Date().toISOString(),
  };
  writeState(state);
  console.log(`marked ${key}`);
  process.exit(0);
}

console.error('Uso: node tools/codex-audit-state.js [check-current|mark-current]');
process.exit(1);
