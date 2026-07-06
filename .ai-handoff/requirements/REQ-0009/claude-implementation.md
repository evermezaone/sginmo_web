# Implementacion Claude - REQ-0009

## Manifiesto Minimo Para Codex
- Dominio py.com.pysistemas.sginmo.dominio.persona: Persona (subset), PersonaJuridica
  (@MapsId PK compartida, cascade PERSIST), PersonaRol, Sucursal.
- EmpresaService: listar/contar por rol EMPRESA (EXISTS PersonaRol), guardar transaccional
  (persona+juridica+rol), cambiarEstado sobre persona.estado, sucursales (por_defecto unica
  por empresa via UPDATE previo), exigir() de permisos en todas las escrituras.
- EmpresaBean + empresas.xhtml (patron estandar, pestanas Principal/Sucursales).
- ContextoEmpresa (@SessionScoped, SGInmo — el modulo de seguridad queda generico):
  carga lazy de empresa+sucursales activas; seleccion persistida via PreferenciaService.
- plantilla.xhtml: item de menu Empresas + selector de sucursal en la barra; index: tarjeta.
- V13__empresas_sucursales.sql (APLICADA en VPS a mano, Flyway sin cablear).
- Comandos probados: mvn -q clean package EXIT 0; deploy-vps.ps1; verificacion por HTML.
