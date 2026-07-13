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
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V22__activo_propietario_estado.sql`
- `Desarrollo/sginmo-web/src/main/webapp/portal/*.xhtml`
- `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/portal*.xhtml`

## Ronda 2

### Observaciones cerradas

- Obs 1 cerrada parcialmente: `loginPassword()` ya responde con mensaje generico cuando la persona existe pero no tiene credencial.
- Obs 2 cerrada parcialmente: se agregaron vistas de propietario para activos, operaciones, liquidaciones y documentos.
- Obs 3 cerrada parcialmente: `solicitarOtp()` ya no inserta `portal_otp` si la persona no tiene email ni telefono.

### Obs 4 - Las consultas de propietario incluyen relaciones inactivas de `activo_propietario`

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PortalService.java`

**Problema:** los nuevos metodos de propietario (`activosPropietario`, `operacionesPropietario`, `liquidacionesPropietario`, `documentosPropietario` y la autorizacion de `descargar`) usan `activo_propietario` sin filtrar `estado = 'ACTIVO'`. La migracion `V22__activo_propietario_estado.sql` agrega baja logica precisamente para preservar historico, y documenta que el ABM solo lista/valida propietarios activos.

**Impacto:** un propietario desvinculado/inactivo puede seguir viendo activos, operaciones, liquidaciones y documentos de propiedades que ya no le corresponden. En un portal externo esto es fuga de informacion sensible.

**Solucion esperada:** agregar `ap.estado = 'ACTIVO'` en todos los joins/subconsultas de propietario del portal, incluyendo descarga de documentos. Mantener RLS por tenant y filtro por persona autenticada.

### Obs 5 - La validacion OTP sigue diferenciando documento inexistente vs persona elegible sin OTP vigente

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PortalAuthService.java`

**Problema:** `validarOtp()` responde con `GENERICO` cuando `buscarElegible()` no encuentra persona, pero si la persona existe y no hay OTP vigente devuelve `"El codigo expiro o no es valido. Solicite uno nuevo."`. Esto afecta directamente el caso corregido de OTP sin canal: `solicitarOtp()` no genera OTP, pero la UI igualmente lleva a `/portal/otp`; al intentar validar, el mensaje distinto confirma que el documento pertenece a una persona elegible.

**Impacto:** sigue violando la regla de no revelar existencia de CI/RUC en el portal publico. Un atacante puede iniciar el flujo con un documento y comparar la respuesta de validacion OTP.

**Solucion esperada:** para fallas de OTP que ocurren antes de tener una identidad validada, usar un mensaje externo uniforme. El motivo exacto (`sin-persona`, `sin-otp-vigente`, `sin-canal`, `codigo-incorrecto`, `max-intentos`) debe quedar en auditoria, no como señal distinguible para el usuario. Si se quiere un texto de UX, que sea el mismo para todos los casos, por ejemplo "No pudimos validar el codigo. Solicite uno nuevo o verifique los datos ingresados."

## Validaciones que si cumplen

- Existe login publico separado en `/portal/login.xhtml`.
- La sesion del portal (`PortalSesion`) ya no depende de `usuario.perfil='PORTAL'`.
- `FiltroAutenticacion` deja `/portal/**` fuera del login administrativo y las pantallas internas del portal usan `viewAction` propio.
- Password y OTP se almacenan con bcrypt, no en texto plano.
- Hay tablas separadas `persona_portal_credencial` y `portal_otp` con RLS por tenant.
- `TenantContext` toma el tenant de `PortalSesion` cuando no hay login administrativo.
- `loginPassword()` ya no revela el caso de persona elegible sin credencial.
- `solicitarOtp()` ya no genera un OTP usable/no entregado cuando no hay email ni telefono.
- Se ejecuto build Maven correctamente.

## Verificacion

```text
mvn -q -pl sginmo-web -am clean package
EXIT 0
```
