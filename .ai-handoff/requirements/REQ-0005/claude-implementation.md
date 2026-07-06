# Implementacion Claude - REQ-0005

## Manifiesto Minimo Para Codex
- `sginmo-web/src/main/webapp/WEB-INF/plantilla.xhtml`: layout (menu lateral, barra
  superior, CSS compartido del estandar ABM, media query responsive).
- `index.xhtml`: tablero de tarjetas por permiso. Todas las pantallas usan
  ui:composition template=/WEB-INF/plantilla.xhtml; f:metadata como hijo directo de la
  composicion (viewAction de acceso).
- Paginas del modulo (usuarios/grupos, en el JAR) tambien usan la plantilla por convencion.
- Comandos probados: mvn -q clean package (EXIT 0); deploy tools/deploy-vps.ps1;
  10 pantallas HTTP 200 con menu presente (grep class="menu" = 1 por pagina).
- Pendiente diferido: contexto empresa/sucursal -> REQ-0009.
