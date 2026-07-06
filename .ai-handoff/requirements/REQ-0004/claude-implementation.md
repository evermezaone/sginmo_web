# Implementacion Claude - REQ-0004 (Seguridad)

Fecha: 2026-07-06 · Estado: COMPLETO, validado funcionalmente por el usuario.

## Manifiesto Minimo Para Codex

**Que es:** modulo de seguridad completo, extraido como JAR REUTILIZABLE
`onesystem-security` (decision del usuario: "ONEsystem-security", Maven multi-modulo
en `Desarrollo/`: onesystem-parent -> onesystem-security + sginmo-web).

**Alcance implementado:**
1. Login bcrypt (jbcrypt 0.4) con bloqueo por intentos (parametros LOGIN_MAX_INTENTOS/
   LOGIN_BLOQUEO_MINUTOS), mismo mensaje para usuario inexistente/clave mala,
   changeSessionId() anti-fijacion, dontRollbackOn para que el intento fallido persista.
2. Permisos por ACCION (VER/CREAR/EDITAR/INACTIVAR/REACTIVAR/EXPORTAR/VER_AUDITORIA)
   + paquete OPERAR (= todo salvo VER_AUDITORIA). ADMINISTRADOR implicito; USUARIO
   explicito. Permisos efectivos = permiso_usuario UNION permiso_grupo de grupos ACTIVOS.
3. Grupos (grupo, usuario_grupo, permiso_grupo) con ABM propio (grupos.xhtml).
4. ABM usuarios (usuarios.xhtml): email/telefono (base 2FA), reseteo de clave que fuerza
   cambio, desbloqueo manual, no auto-inactivacion, pestanas Grupos y Permisos directos.
5. Cambio de contrasena (cambiar-password.xhtml) obligatorio al primer ingreso/reseteo
   (flag debe_cambiar_password + FiltroAutenticacion fuerza la pantalla).
6. Preferencias por usuario (preferencia_usuario): "Mi vista" en ABM Articulos.
7. Alertas por correo ante intento fallido/bloqueo: CorreoService (SMTP configurable en
   parametro_sistema, claves SMTP_*; pendiente solo SMTP_CLAVE del usuario). Asincrono,
   jamas rompe el login.
8. Datos de auditoria SOLO con VER_AUDITORIA o ADMINISTRADOR (decision del usuario).

**Archivos clave:**
- `Desarrollo/onesystem-security/` — JAR: py.com.one.core (Auditable, AuditoriaListener,
  UsuarioActual, NegocioException, ErroresBd), py.com.one.security.dominio (Usuario, Grupo,
  UsuarioGrupo, PermisoUsuario, PermisoGrupo, PreferenciaUsuario, Accion),
  py.com.one.security.servicio (SeguridadService, UsuarioService, GrupoService,
  PreferenciaService, CorreoService, ProveedorPantallas, ProveedorParametros),
  py.com.one.security.web (SesionUsuario, LoginBean, CambioPasswordBean, UsuarioBean,
  GrupoBean, FiltroAutenticacion). Pantallas DENTRO del JAR (META-INF/resources):
  login, cambiar-password, usuarios, grupos (usan por convencion /WEB-INF/plantilla.xhtml
  del anfitrion). Esquema de referencia: src/main/resources/db/esquema-referencia.sql.
- Integracion SGInmo: `sginmo-web/.../servicio/IntegracionSeguridad.java` (implementa
  ProveedorPantallas via entidad PANTALLAS y ProveedorParametros via parametro_sistema);
  persistence.xml lista las entidades del JAR; war-plugin declara `jakarta.mail.api`.
- Migraciones (aplicadas A MANO en la VPS, Flyway aun sin cablear): V5 seed seguridad,
  V6 permiso_usuario + OPERAR (V9), V7 preferencia_usuario + debe_cambiar_password +
  PANTALLAS, V10 grupos + email/telefono, V11 parametros SMTP.

**Comandos probados:**
- Build: `mvn -q clean package` en `Desarrollo/` (multi-modulo). Deploy:
  `powershell -File tools/deploy-vps.ps1` (subida atomica + .dodeploy + verificacion).
- Verificacion end-to-end por curl contra la VPS (documentada en test-plan.md).

**Invariantes de BD:** version optimista en TODAS las tablas (V4); toda tabla nueva nace
con version + auditoria; los SP futuros que actualicen tablas administradas por ABM deben
hacer SET version = version + 1.

**Riesgos/pendientes declarados:** 2FA por correo y preguntas de seguridad (siguiente
fase, infraestructura lista); SMTP_CLAVE vacia hasta que el usuario la cargue (pantalla
Parametros); puerto 8080 expuesto -> nginx+HTTPS como REQ de ops.
