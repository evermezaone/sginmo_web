# Preauditoria Claude - REQ-0078

Fecha: 2026-07-13
Responsable: Claude

- [x] Lei observaciones previas (REQ nuevo, sin observaciones) y la nota de analisis de Codex (no aprobar `Perfil PORTAL + persona vinculada`).
- [x] Consulte AUDITORIA_OBSERVACION: sin observaciones abiertas del REQ.
- [x] Sin credenciales/tokens/hosts hardcodeados; OTP y password en bcrypt (jamas en texto plano).
- [x] `req.md` sin criterios `[ ]` pendientes (21/21).
- [x] `claude-implementation.md` con Manifiesto, comandos probados y "Limitaciones Conocidas".
- [x] `test-plan.md` solo afirma lo real (build/Flyway/smoke + prueba funcional de ramas no-encontrado/invalido; happy-path con socio real + SMTP como manual).
- [x] Revise flujos equivalentes: reusa PortalService (0055), CorreoService y bcrypt (jbcrypt) de onesystem-security, patron RLS de tablas de negocio; identidad separada de SesionUsuario.
- [x] Toque BD (V52: 2 tablas + RLS + params + ampliacion portal_acceso): documente invariantes (RLS por tenant; OTP uso unico/expiracion; credencial con bloqueo; auditoria por evento).
- [x] Regla general aplicada: parametros configurables (no hardcode); mensajes genericos anti-enumeracion; identidad efectiva desde la sesion (no id de request); validacion persona+tenant en backend.
- [x] Ejecute `python tools/handoff.py check SGI REQ-0078` y paso sin errores.

Notas:

- Riesgo alto (autenticacion externa). Auditor: revisar aislamiento por persona+tenant (RLS + filtro), anti-enumeracion, hashing/expiracion de OTP y que el perfil PORTAL ya no condiciona el acceso.

## Respuesta Por Observacion Cerrada

(No aplica: REQ nuevo.)
