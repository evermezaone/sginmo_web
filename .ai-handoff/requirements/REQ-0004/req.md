# REQ-0004 - Seguridad: login, bcrypt, roles y bloqueo de intentos

**Numero:** REQ-0004
**Fecha de creacion:** 2026-07-04
**Estado inicial:** NUEVO
**Prioridad:** no indicada

## Texto Original

"entonces debemos implementar ya el login, y luego volver a pulir e implementar el ABM
estandar, y una vez aprobado, aplicar a todos los ABMs" (usuario, 2026-07-05; el REQ nace
del backlog doc 08: seguridad con login, bcrypt, roles y bloqueo de intentos).

## Objetivo Funcional

El sistema exige autenticacion para acceder a cualquier pantalla. Login con usuario y
contrasena (bcrypt), bloqueo temporal por intentos fallidos (parametrizable), sesion con
logout, y el usuario autenticado alimenta la auditoria de entidades. Fases siguientes del
mismo REQ: permisos por accion + modo solo lectura, preferencias por usuario ("Mi vista",
columnas del selector) y ABM de usuarios con cambio de contrasena.

## Criterios De Aceptacion

- [x] Sin sesion, cualquier *.xhtml redirige a login.xhtml (tambien los POST AJAX vencidos, via partial-response).
- [x] Login valido inicia sesion, renueva el id de sesion (anti-fijacion) y va a index.
- [x] Credenciales invalidas: mismo mensaje para usuario inexistente y password erronea.
- [x] Bloqueo tras LOGIN_MAX_INTENTOS fallidos por LOGIN_BLOQUEO_MINUTOS (parametro_sistema); el contador de intentos SE GRABA aunque la autenticacion falle (dontRollbackOn).
- [x] Usuario INACTIVO no puede ingresar.
- [x] Logout invalida la sesion y vuelve al login.
- [x] La auditoria (usuario_creacion/modificacion) registra el codigo del usuario logueado.
- [x] Permisos por accion (V6 permiso_usuario): ADMINISTRADOR todo implicito; USUARIO solo
      permisos explicitos por pantalla+accion (comodin '*'). Verificado con usuario de prueba
      'consulta' (solo VER en articulos): sin Nuevo, sin exportar, sin inactivar, icono ojo
      en vez de lapiz y dialogo en modo consulta (campos bloqueados, sin Guardar).
- [x] Datos de auditoria (Modificado por / Fecha modif. en la grilla, historial futuro) SOLO
      con VER_AUDITORIA o perfil ADMINISTRADOR (decision del usuario 2026-07-05); columnas
      ocultas por defecto, se activan desde el selector de columnas.
- [ ] preferencia_usuario + "Mi vista" (columnas, orden, filtros, tamano de pagina).
- [ ] ABM de usuarios con cambio de contrasena obligatorio al primer ingreso.

## Dependencias

- Depende de: ninguna
- Requerido por: ninguno

## Alcance agregado por decision del usuario (2026-07-05)

**Preferencias por usuario (Opcion B, validacion del ABM Articulos):** una vez que exista
login, implementar la tabla `preferencia_usuario` (usuario + pantalla + clave + valor,
UNIQUE por los tres primeros) y un `PreferenciaService` generico. Primer uso: persistir
las columnas visibles del selector de columnas (p:columnToggler) de cada ABM, para que
cada usuario recupere su configuracion al volver a entrar. El usuario decidio NO activar
un mecanismo provisorio sin login; hasta este REQ el selector funciona pero no recuerda.

**Ampliacion por estudio del estandar ABM (2026-07-05, doc 11):**
- **"Mi vista"**: el usuario puede guardar combinaciones nombradas de columnas visibles +
  orden + filtros + tamano de pagina por pantalla (extension natural de preferencia_usuario,
  valor JSON). Incluye la persistencia temporal de pagina/filtros/orden entre pantallas.
- **Permisos por ACCION, no solo por pantalla**: ver / crear / editar / inactivar /
  reactivar / exportar / ver auditoria como permisos separados. Exportar datos ES un permiso.
