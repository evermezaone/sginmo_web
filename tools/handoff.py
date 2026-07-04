# -*- coding: utf-8 -*-
"""Compuerta de handoff project-aware (BD-first) — SGInmo Web (SGI).

Uso:
  python tools/handoff.py check <PROJ> <REQ>
  python tools/handoff.py ready <PROJ> <REQ> ["resumen para Codex"]

- Valida los artefactos locales de .ai-handoff/requirements/<REQ>/ (como exige CLAUDE.md).
- Consulta la BD real (AUDITORIA_OBSERVACION) y bloquea si hay observaciones 'abierta' del REQ.
- 'ready': si la validacion pasa, deriva el REQ a LISTO_PARA_REVISION (codex) con sp_derivar_req
  y actualiza el mirror local requirements/registry.jsonl y to_codex.md.
La BD es la fuente de verdad; los archivos locales son la evidencia/mirror.

Adaptado de la version FLUX: en este workspace los REQ viven directamente en
.ai-handoff/requirements/REQ-XXXX (sin subcarpeta por proyecto).
"""
import os, sys, re, json, pymysql

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..')
REQS = os.path.join(ROOT, '.ai-handoff', 'requirements')


def load_env(path):
    env = {}
    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#') or '=' not in line:
                continue
            k, v = line.split('=', 1); v = v.strip()
            if len(v) >= 2 and v[0] == v[-1] and v[0] in '"\'':
                v = v[1:-1]
            env[k.strip()] = v
    return env


def read(reqdir, name):
    p = os.path.join(reqdir, name)
    if not os.path.exists(p):
        return None
    return open(p, encoding='utf-8').read().replace('﻿', '')


def unchecked_in_section(text, section_re):
    """Items '- [ ]' dentro de una seccion '## ...'."""
    out, inside = [], False
    for line in (text or '').splitlines():
        if re.match(r'^##\s+', line):
            inside = bool(re.match(section_re, line, re.I))
            continue
        if inside and re.match(r'^[-*]\s+\[\s\]\s+', line.strip()):
            out.append(line.strip())
    return out


def meaningful(text):
    if not text:
        return False
    for line in text.splitlines():
        s = line.strip()
        if s and not s.startswith('#') and not s.startswith('|---') and '[escenario]' not in s and '[esperado]' not in s:
            return True
    return False


def validate_local(proj, req):
    issues = []
    reqdir = os.path.join(REQS, req)
    if not os.path.isdir(reqdir):
        return [f'{proj}/{req}: no existe la carpeta {reqdir}'], reqdir

    reqmd = read(reqdir, 'req.md')
    if reqmd is None:
        issues.append(f'{req}: falta req.md')
    else:
        for it in unchecked_in_section(reqmd, r'^##\s+Criterios'):
            issues.append(f'{req}: criterio pendiente en req.md: {it}')

    impl = read(reqdir, 'claude-implementation.md')
    if not meaningful(impl):
        issues.append(f'{req}: claude-implementation.md vacio')
    else:
        if not re.search(r'Manifiesto\s+M[ií]nimo\s+Para\s+Codex', impl, re.I):
            issues.append(f'{req}: claude-implementation.md sin Manifiesto Minimo Para Codex')
        if not re.search(r'Comandos?\s+(probados|verificados)|mvn|maven|pytest|EXIT\s*:\s*0', impl, re.I):
            issues.append(f'{req}: claude-implementation.md sin comandos probados documentados')

    if not meaningful(read(reqdir, 'test-plan.md')):
        issues.append(f'{req}: test-plan.md vacio o sin evidencia')

    chk = read(reqdir, 'preaudit-checklist.md')
    if chk is None:
        issues.append(f'{req}: falta preaudit-checklist.md')
    else:
        if not re.search(r'Responsable:\s*Claude', chk, re.I):
            issues.append(f'{req}: preaudit-checklist.md sin firma "Responsable: Claude"')
        for line in chk.splitlines():
            if re.match(r'^[-*]\s+\[\s\]\s+', line.strip()):
                issues.append(f'{req}: checklist preauditoria pendiente: ' + line.strip()[5:].strip())

    ev = read(reqdir, 'events.jsonl')
    if ev is None:
        issues.append(f'{req}: falta events.jsonl')
    else:
        for i, line in enumerate(ev.splitlines(), 1):
            if line.strip():
                try:
                    json.loads(line)
                except Exception as e:
                    issues.append(f'{req}/events.jsonl:{i}: JSON invalido ({e})')
    return issues, reqdir


def open_observations(env, proj, req):
    c = pymysql.connect(host=env['PROJECT_DB_HOST'], port=int(env['PROJECT_DB_PORT']),
                        user=env['PROJECT_DB_USER'], password=env['PROJECT_DB_PASS'],
                        database=env['PROJECT_DB_NAME'], connect_timeout=10)
    cur = c.cursor()
    cur.execute("""SELECT o.IdObservacion, o.Categoria, o.Subcategoria, o.Severidad
                   FROM AUDITORIA_OBSERVACION o
                   JOIN REQ r ON r.IdReq = o.IdReq
                   JOIN PROYECTO p ON p.IdProyecto = r.IdProyecto
                   WHERE p.Codigo=%s AND r.Codigo=%s AND o.Estado='abierta'
                   ORDER BY o.IdObservacion""", (proj, req))
    rows = cur.fetchall()
    c.close()
    return rows


def run_check(env, proj, req):
    issues, reqdir = validate_local(proj, req)
    print(f'Carpeta validada: {reqdir}')
    for ob in open_observations(env, proj, req):
        issues.append(f'{req}: observacion ABIERTA en BD: Obs {ob[0]} {ob[1]}/{ob[2]} [{ob[3]}]')
    if issues:
        print('HANDOFF CHECK: FAIL')
        for i in issues:
            print('  - ' + i)
        return False
    print('HANDOFF CHECK: OK')
    return True


def run_ready(env, proj, req, resumen):
    if not run_check(env, proj, req):
        print('No se deriva: la validacion fallo.')
        return False
    c = pymysql.connect(host=env['PROJECT_DB_HOST'], port=int(env['PROJECT_DB_PORT']),
                        user=env['PROJECT_DB_USER'], password=env['PROJECT_DB_PASS'],
                        database=env['PROJECT_DB_NAME'], connect_timeout=10)
    cur = c.cursor()
    cur.callproc('sp_derivar_req', (proj, req, 'LISTO_PARA_REVISION', 'codex', 'claude', resumen))
    c.commit()
    cur.execute("""SELECT r.Estado, r.Responsable FROM REQ r JOIN PROYECTO p ON p.IdProyecto=r.IdProyecto
                   WHERE p.Codigo=%s AND r.Codigo=%s""", (proj, req))
    estado, resp = cur.fetchone()
    c.close()
    # mirror local
    reg = os.path.join(REQS, 'registry.jsonl')
    if os.path.exists(reg):
        lines = []
        for ln in open(reg, encoding='utf-8').read().splitlines():
            if not ln.strip():
                continue
            o = json.loads(ln)
            if o.get('req') == req:
                o['estado'] = 'LISTO_PARA_REVISION'
            lines.append(json.dumps(o))
        open(reg, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
    # mirror buzon: to_codex.md (algunos flujos de Codex lo leen como cola de auditoria).
    write_to_codex(env, proj, req, resumen)
    print(f'DERIVADO: {proj}/{req} -> Estado={estado} Responsable={resp}')
    return True


def pending_for_codex(env, proj):
    """Codigos de REQ del proyecto en LISTO_PARA_REVISION (cola real de auditoria de Codex)."""
    try:
        c = pymysql.connect(host=env['PROJECT_DB_HOST'], port=int(env['PROJECT_DB_PORT']),
                            user=env['PROJECT_DB_USER'], password=env['PROJECT_DB_PASS'],
                            database=env['PROJECT_DB_NAME'], connect_timeout=10)
        cur = c.cursor()
        cur.execute("""SELECT r.Codigo FROM REQ r JOIN PROYECTO p ON p.IdProyecto=r.IdProyecto
                       WHERE p.Codigo=%s AND r.Estado='LISTO_PARA_REVISION' ORDER BY r.Codigo""", (proj,))
        rows = [x[0] for x in cur.fetchall()]
        c.close()
        return rows
    except Exception:
        return []


def write_to_codex(env, proj, req, resumen):
    """Mantiene .ai-handoff/to_codex.md en sync con la derivacion (mirror que lee Codex).
    Lista TODOS los REQ pendientes de revision (no solo el ultimo) para no perder cola."""
    import datetime
    ts = datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ')
    path = os.path.join(ROOT, '.ai-handoff', 'to_codex.md')
    pend = pending_for_codex(env, proj)
    if req not in pend:
        pend.append(req)
    req_list = ', '.join(pend) if pend else req
    body = (
        'ESTADO: LISTO_PARA_REVISION\n'
        f'REQ: {req_list}\n'
        f'TS: {ts}\n'
        'AGENTE: claude\n'
        f'MENSAJE: [{proj}] Cola de auditoria: {req_list}. Ultimo derivado {req}: {resumen}\n\n'
        '---\n'
        '# Estados validos (Claude escribe, Codex lee):\n'
        '# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO\n'
        '\n'
        '# Reglas:\n'
        '# - Solo Claude (o el implementador) escribe en este archivo.\n'
        '# - Al cerrar o mergear un REQ, debe quedar en ESPERA.\n'
        '# - REQ puede ser una lista: REQ-0001, REQ-0002\n'
    )
    try:
        open(path, 'w', encoding='utf-8').write(body)
    except Exception as e:
        print('AVISO: no se pudo escribir to_codex.md:', e)


def main():
    if len(sys.argv) < 4:
        print(__doc__)
        sys.exit(2)
    mode, proj, req = sys.argv[1], sys.argv[2].upper(), sys.argv[3]
    resumen = sys.argv[4] if len(sys.argv) > 4 else f'{req}: reenvio a auditoria.'
    env = load_env(os.path.join(ROOT, '.env'))
    if mode == 'check':
        sys.exit(0 if run_check(env, proj, req) else 1)
    elif mode == 'ready':
        sys.exit(0 if run_ready(env, proj, req, resumen) else 1)
    else:
        print('Modo invalido (check|ready)')
        sys.exit(2)


if __name__ == '__main__':
    main()
