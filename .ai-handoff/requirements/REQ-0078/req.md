# REQ-0078 - Portal externo de socios con CI/RUC, OTP y password de persona

**Numero:** REQ-0078
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

El portal no debe funcionar con `Perfil PORTAL + persona vinculada`. Para el portal, el cliente debe ingresar su CI o RUC, el sistema debe enviar un OTP a su celular o mail, luego debe entrar y cambiar su password guardado en la tabla del cliente/persona. Desde alli debe ver solo su cuenta y nada mas.

## Objetivo Funcional

Reemplazar el mecanismo interno de acceso del `REQ-0055` por un portal externo de autoservicio para clientes y propietarios. El acceso debe estar basado en la identidad comercial de la persona (CI/RUC), validacion por OTP y password propio de portal asociado a la persona, no en usuarios administrativos ni perfiles internos del sistema.

## Alcance

- El portal es para socios externos: clientes, propietarios o ambos segun roles de la persona.
- El portal no es para empleados ni usuarios administrativos.
- El usuario administrativo puede configurar datos necesarios del socio, pero no debe navegar como socio usando su cuenta interna.
- La implementacion existente de `REQ-0055` puede reutilizar vistas/servicios de consulta, pero debe corregir la autenticacion y el modelo de credenciales.

## Criterios De Aceptacion

- [x] Existe una pantalla publica de acceso al portal separada del login administrativo.
- [x] El socio ingresa CI/RUC o documento equivalente; el sistema busca una persona activa del tenant con rol CLIENTE y/o PROPIETARIO.
- [x] Si la persona no existe, esta inactiva, no pertenece al tenant o no tiene rol habilitado para portal, el mensaje debe ser generico y no revelar si el documento existe.
- [x] El sistema envia un OTP al celular y/o email registrado de la persona, segun disponibilidad y configuracion.
- [x] El OTP tiene expiracion configurable, limite de reintentos y bloqueo temporal ante abusos.
- [x] El OTP se almacena hasheado o de forma no reutilizable; nunca debe quedar el codigo plano persistido.
- [x] En el primer acceso exitoso con OTP, el socio debe definir o cambiar su password de portal.
- [x] El password de portal queda asociado a la persona/cliente, con hash fuerte tipo bcrypt/PBKDF2/Argon2; nunca reversible ni en texto plano.
- [x] En accesos posteriores, el socio puede entrar con CI/RUC + password de portal.
- [x] Existe flujo de recuperacion de password con OTP, sin intervencion de usuario administrativo.
- [x] La sesion del portal se identifica por persona, tenant y roles comerciales, no por `usuario.perfil='PORTAL'`.
- [x] El perfil `PORTAL` en la tabla `usuario` no debe ser requisito para que un socio use el portal.
- [x] El portal muestra solo informacion de la persona autenticada: cuotas, pagos, deuda, documentos, operaciones y liquidaciones permitidas segun sea cliente o propietario.
- [x] Un cliente no puede ver datos de otros clientes, propietarios o tenants aunque altere URLs, IDs o parametros.
- [x] Un propietario solo puede ver sus activos, operaciones, liquidaciones y documentos permitidos; no puede ver informacion de otros propietarios.
- [x] El portal no expone menu administrativo ni permite ejecutar cobros, anulaciones, ediciones, bajas, configuraciones ni operaciones internas.
- [x] Todos los endpoints/servicios del portal validan persona+tenant en backend; no alcanza con ocultar controles en JSF.
- [x] Los documentos solo se pueden descargar si estan marcados como visibles para portal y pertenecen a la persona autenticada o a sus operaciones/activos permitidos.
- [x] Queda auditoria de solicitud de OTP, validacion OTP, login, cambio de password, logout y descargas, con persona, tenant, IP, user-agent y fecha/hora.
- [x] La pantalla es responsive para celular y no depende de estar previamente logueado al sistema administrativo.
- [x] Los textos y parametros de envio OTP son configurables por empresa/sucursal cuando aplique.

## Modelo De Datos Esperado

La solucion debe agregar o ajustar tablas/campos sin mezclar credenciales externas con usuarios internos:

- Campos o tabla de credenciales de portal por persona, por ejemplo `persona_portal_credencial`.
- Hash de password de portal, fecha de ultimo cambio, estado, intentos fallidos y bloqueo temporal.
- Tabla de OTP de portal, por ejemplo `portal_otp`, con hash/codigo no reversible, expiracion, intentos y uso unico.
- Tabla de auditoria de portal ampliada o nueva para eventos de seguridad.
- Indices por tenant + documento/persona para busqueda segura y eficiente.

## Reglas De Seguridad

- No usar el perfil administrativo `PORTAL` como mecanismo principal de acceso de socios.
- No permitir login de portal por usuario administrativo.
- No revelar existencia de CI/RUC en mensajes de error.
- No guardar OTP ni password en texto plano.
- No aceptar IDs de persona desde request como autoridad de acceso.
- La identidad efectiva del portal se toma de la sesion de portal creada luego de OTP/password.
- Debe existir proteccion contra fuerza bruta de CI/RUC, OTP y password.
- La sesion de portal debe tener timeout y logout independientes del login administrativo.

## Dependencias

- Depende de: REQ-0004, REQ-0012, REQ-0055, REQ-0064.
- Reemplaza parcialmente: autenticacion de portal implementada en REQ-0055.
- Requerido por: portal vendible para clientes y propietarios.

## Fuentes Y Trazabilidad

- Decision explicita del usuario del 2026-07-12: el portal debe usar CI/RUC + OTP + password de persona/cliente, no `Perfil PORTAL + persona vinculada`.
- REQ-0055 queda como base funcional de consulta, pero su autenticacion debe corregirse con este requerimiento.
