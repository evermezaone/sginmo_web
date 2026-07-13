# REQ-0078 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS  
**Fecha:** 2026-07-13  
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Alcance revisado

- `.ai-handoff/requirements/REQ-0078/req.md`
- `.ai-handoff/requirements/REQ-0078/claude-implementation.md`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PortalAuthService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PortalService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PortalSesion.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PortalLoginBean.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PortalBean.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/TenantContext.java`
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/web/FiltroAutenticacion.java`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V52__portal_externo_credenciales.sql`
- `Desarrollo/sginmo-web/src/main/webapp/portal/*.xhtml`
- `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/portal*.xhtml`

## Observaciones bloqueantes

### Obs 1 - El login por password revela que el documento existe y que no tiene clave

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PortalAuthService.java`

**Problema:** `loginPassword()` devuelve el mensaje especifico `"Aun no definio su contrasena. Use 'Primer ingreso' para crearla."` cuando existe una persona elegible pero no existe fila en `persona_portal_credencial`. Esto diferencia tres estados: documento inexistente/no elegible, documento elegible sin clave y documento elegible con clave incorrecta.

**Impacto:** viola los criterios del REQ: "si la persona no existe, esta inactiva, no pertenece al tenant o no tiene rol habilitado para portal, el mensaje debe ser generico" y "no revelar existencia de CI/RUC en mensajes de error". En un portal publico, confirmar que un CI/RUC pertenece a un socio es fuga de informacion.

**Solucion esperada:** usar respuesta generica tambien para `sin-credencial`, y orientar el primer ingreso desde la UI sin depender de confirmar existencia. La auditoria interna puede conservar el motivo especifico, pero el mensaje al usuario debe ser uniforme.

### Obs 2 - El portal autentica propietarios, pero el contenido sigue siendo solo de cliente

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PortalService.java`

**Problema:** `buscarElegible()` acepta rol `PROPIETARIO`, pero las consultas del portal filtran solo por relaciones de cliente: `operacion.cliente = :p`, `cobro.persona = :p` y documentos de operaciones donde la persona es cliente. No hay consultas de activos del propietario, operaciones por activo propietario, liquidaciones del propietario ni documentos vinculados a esos activos/liquidaciones.

**Impacto:** incumple los criterios del REQ que exigen que el propietario vea "sus activos, operaciones, liquidaciones y documentos permitidos". Un propietario puro puede autenticarse y ver una cuenta vacia o incompleta, aunque tenga activos/liquidaciones reales.

**Solucion esperada:** agregar un modelo de cuenta por rol. Para `CLIENTE`, mantener cuotas/pagos/deuda/documentos por cliente. Para `PROPIETARIO`, agregar activos propios, operaciones asociadas a esos activos, liquidaciones y documentos permitidos. Todas las consultas deben filtrar por `PortalSesion.persona` y tenant efectivo, no por parametros de request.

### Obs 3 - Si la persona no tiene email ni telefono, se crea un OTP imposible de recibir

**Severidad:** media  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PortalAuthService.java`

**Problema:** `solicitarOtp()` crea `portal_otp` aun cuando `canal` queda `null` por falta de email y telefono. Luego `enviarCodigo()` no envia nada. El socio queda en una pantalla de OTP sin forma de obtener el codigo.

**Impacto:** incumple el criterio de envio por celular/email segun disponibilidad y genera un flujo funcionalmente bloqueado. Tambien crea OTP validos no entregados.

**Solucion esperada:** si no hay canal disponible, no generar OTP usable. Mantener mensaje externo generico, auditar el evento como sin canal y definir una salida operativa clara: pedir actualizacion de datos por administracion o canal configurado.

## Validaciones que si cumplen

- Existe login publico separado en `/portal/login.xhtml`.
- La sesion del portal (`PortalSesion`) ya no depende de `usuario.perfil='PORTAL'`.
- `FiltroAutenticacion` deja `/portal/**` fuera del login administrativo y las pantallas internas del portal usan `viewAction` propio.
- Password y OTP se almacenan con bcrypt, no en texto plano.
- Hay tablas separadas `persona_portal_credencial` y `portal_otp` con RLS por tenant.
- `TenantContext` toma el tenant de `PortalSesion` cuando no hay login administrativo.
- Las descargas de documentos del cliente verifican `visible_portal`, estado activo y pertenencia a persona/operacion.

## Verificacion

No cierro el REQ. No se ejecuto build final porque las observaciones son de cumplimiento funcional/seguridad en revision estatica.
