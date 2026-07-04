#!/usr/bin/env node
/**
 * Crea un nuevo REQ numerado con todos los archivos de documentación.
 * Uso: node tools/new-req.js <titulo del requerimiento>
 */
import { readFileSync, writeFileSync, mkdirSync, readdirSync, existsSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const HANDOFF     = join(__dirname, '..', '.ai-handoff');
const REQUIREMENTS = join(HANDOFF, 'requirements');
const TEMPLATES   = join(REQUIREMENTS, '_templates');
const REGISTRY    = join(REQUIREMENTS, 'registry.jsonl');

const title = process.argv.slice(2).join(' ').trim();
if (!title) {
  console.error('Uso: node tools/new-req.js <titulo del requerimiento>');
  console.error('Ejemplo: node tools/new-req.js Filtro de audios por cliente');
  process.exit(1);
}

// Calcular el siguiente número de REQ
let maxNum = 0;
if (existsSync(REGISTRY)) {
  for (const line of readFileSync(REGISTRY, 'utf8').split('\n')) {
    try {
      const m = JSON.parse(line).req?.match(/REQ-(\d+)/);
      if (m) maxNum = Math.max(maxNum, parseInt(m[1], 10));
    } catch { /* línea vacía o malformada */ }
  }
}
const reqId  = `REQ-${String(maxNum + 1).padStart(4, '0')}`;
const reqDir = join(REQUIREMENTS, reqId);
const ts     = new Date().toISOString();
const date   = ts.slice(0, 10);

if (existsSync(reqDir)) {
  console.error(`Error: la carpeta ${reqDir} ya existe.`);
  process.exit(1);
}

// Crear carpeta y copiar plantillas, reemplazando marcadores
mkdirSync(reqDir, { recursive: true });
for (const file of readdirSync(TEMPLATES)) {
  const content = readFileSync(join(TEMPLATES, file), 'utf8')
    .replace(/REQ-XXXX/g, reqId)
    .replace(/YYYY-MM-DD/g, date)
    .replace(/\[Titulo corto\]/g, title);
  writeFileSync(join(reqDir, file), content);
}

// Agregar entrada al registry
const entry = JSON.stringify({
  req: reqId,
  titulo: title,
  estado: 'NUEVO',
  rama: 'pendiente',
  ts_creado: ts,
  ts_actualizado: ts,
});
const existing = existsSync(REGISTRY) ? readFileSync(REGISTRY, 'utf8').trimEnd() : '';
writeFileSync(REGISTRY, (existing ? existing + '\n' : '') + entry + '\n');

// Agregar evento inicial a events.jsonl
const event = JSON.stringify({
  ts,
  actor: 'sistema',
  event: 'REQ_CREADO',
  status: 'NUEVO',
  nota: title,
});
writeFileSync(join(reqDir, 'events.jsonl'), event + '\n');

console.log(`✓ Creado ${reqId} — "${title}"`);
console.log(`  Carpeta: ${reqDir}`);
console.log(`  Siguiente paso: completar ${reqId}/req.md y ${reqId}/analysis.md`);
