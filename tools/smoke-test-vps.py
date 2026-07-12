"""
Smoke-test HTTP de SGInmo web (post-deploy)
===========================================
Procedimiento FIRME: se corre DESPUES de cada deploy a la VPS (lo invoca
`tools/deploy-vps.ps1` al final). Loguea con un usuario real y hace GET de todas
las pantallas, detectando la pagina de error de Facelets/Mojarra
("An Error Occurred" / "does not have the property '...'"). Asi caza los errores de
RENDER que el build NO atrapa (JSF se compila como strings; solo fallan al renderizar) —
el caso que motivo esto: /personas.xhtml referenciaba #{per.telefono} tras reducir la
entidad Persona en el multiempresa.

Cobertura: el RENDER de cada pantalla (la grilla). NO ejercita los dialogos dynamic="true"
(cargan por ajax al abrir) ni las escrituras — eso queda para verificacion manual/funcional.

Credenciales (NUNCA hardcodeadas): toma SMOKE_USER / SMOKE_PASS de:
  1) variables de entorno del proceso, o
  2) el archivo `.env` del repo (gitignored).
Base URL: SMOKE_BASE (env) o el default de produccion.

Uso:
  python tools/smoke-test-vps.py
  SMOKE_USER=admin SMOKE_PASS=... python tools/smoke-test-vps.py
Exit code: 0 si TODAS las pantallas renderizan; !=0 si alguna da error de render (o falla el login).
"""
import urllib.request, urllib.parse, urllib.error, http.cookiejar, re, os, sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def cargar_env():
    env = {}
    ruta = os.path.join(RAIZ, ".env")
    if os.path.exists(ruta):
        for linea in open(ruta, encoding="utf-8", errors="replace"):
            linea = linea.strip()
            if "=" in linea and not linea.startswith("#"):
                k, v = linea.split("=", 1)
                env[k.strip()] = v.strip()
    return env

_env = cargar_env()
def cfg(clave, defecto=None):
    return os.environ.get(clave) or _env.get(clave) or defecto

BASE = cfg("SMOKE_BASE", "http://77.237.235.69:8080/sginmo-web").rstrip("/")
USER = cfg("SMOKE_USER", "admin")
PWD  = cfg("SMOKE_PASS")

PANTALLAS = ["index", "personas", "activos", "operaciones", "caja", "articulos", "monedas",
             "impuestos", "formas-pago", "geografia", "empresas", "listas", "parametros",
             "usuarios", "grupos", "ingresos-egresos", "liquidaciones",
             "plantillas-documentos",  # REQ-0041
             "salud",  # REQ-0051
             "agenda",  # REQ-0052
             "documentos",  # REQ-0053
             "documentos-generados",  # REQ-0054
             "dashboard-gerencial",  # REQ-0056
             "cobranza",  # REQ-0057
             "comprobantes",  # REQ-0058
             "arqueo",  # REQ-0059
             "importacion"]  # REQ-0061

_cj = http.cookiejar.CookieJar()
_op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(_cj))
_op.addheaders = [("User-Agent", "sginmo-smoke")]

def _open(url, data=None, headers=None):
    req = urllib.request.Request(url, data=data)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        r = _op.open(req, timeout=30)
        return r.getcode(), r.read().decode("utf-8", "replace"), r.geturl()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace"), url

def _viewstate(html):
    m = re.search(r'name="jakarta\.faces\.ViewState"[^>]*value="([^"]+)"', html)
    return m.group(1) if m else None

def login():
    if not PWD:
        print("ERROR: falta SMOKE_PASS (env o .env). No se puede loguear.", file=sys.stderr)
        return False
    _c, h, _u = _open(BASE + "/login.xhtml")
    vs = _viewstate(h)
    if not vs:
        print("ERROR: no se obtuvo ViewState de login.xhtml", file=sys.stderr)
        return False
    form = {"jakarta.faces.partial.ajax": "true", "jakarta.faces.source": "frmLogin:btnEntrar",
            "jakarta.faces.partial.execute": "frmLogin", "jakarta.faces.partial.render": "frmLogin",
            "frmLogin:btnEntrar": "frmLogin:btnEntrar", "frmLogin": "frmLogin",
            "frmLogin:usuario": USER, "frmLogin:clave": PWD, "jakarta.faces.ViewState": vs}
    _c, h, _u = _open(BASE + "/login.xhtml", data=urllib.parse.urlencode(form).encode(),
                      headers={"Faces-Request": "partial/ajax",
                               "Content-Type": "application/x-www-form-urlencoded"})
    rd = re.search(r'<redirect url="([^"]+)"', h)
    if not rd:
        print("ERROR: login sin redirect (credenciales?)", file=sys.stderr)
        return False
    base_host = BASE.rsplit("/", 1)[0]
    _open(base_host + rd.group(1))
    return True

def render_error(html):
    if "An Error Occurred" in html or "does not have the property" in html or "<title>Error" in html:
        m = re.search(r"does not have the property '[^']+'|/[\w-]+\.xhtml @\d+,\d+[^<]{0,120}", html)
        return m.group(0)[:110] if m else "error de render"
    return None

def main():
    print(f"Smoke-test SGInmo — {BASE} (user={USER})")
    if not login():
        return 2
    fallas = 0
    for p in PANTALLAS:
        c, h, u = _open(f"{BASE}/{p}.xhtml")
        err = render_error(h)
        en_login = "frmLogin" in h and "login.xhtml" in u
        estado = "ERROR" if err else ("(no logueado?)" if en_login else "ok")
        if err:
            fallas += 1
        print(f"  {p:16s} HTTP {c} {estado}" + (f"  -> {err}" if err else ""))
    print("=== RESULTADO:", "TODAS OK" if fallas == 0 else f"{fallas} pantalla(s) con ERROR de render", "===")
    return 0 if fallas == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
