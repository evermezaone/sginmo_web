# Preauditoria Claude - REQ-0083 (Fase 1)

Fecha: 2026-07-13
Responsable: Claude

- [x] Lei observaciones previas (REQ nuevo, sin observaciones).
- [x] Consulte AUDITORIA_OBSERVACION: sin observaciones abiertas del REQ.
- [x] Sin credenciales/tokens/hosts hardcodeados.
- [x] `req.md` sin criterios `[ ]` pendientes; alcance no cubierto como Follow-up / Fuera de alcance (0084/0085).
- [x] `claude-implementation.md` con Manifiesto, comandos probados y "Limitaciones Conocidas".
- [x] `test-plan.md` solo afirma lo real (build/Flyway/smoke; funcional manual con socio+caja).
- [x] Revise flujos equivalentes: reutiliza el motor de cobros (f_cobrar_documento via CajaService), el patron de
      almacenamiento de archivos (baseDir por tenant) y la identidad de portal (PortalSesion, REQ-0078).
- [x] Toque BD (V56: tabla + RLS + params + pantalla): RLS por tenant; unique parcial anti-doble-aplicacion.
- [x] Regla general aplicada: aislamiento persona+tenant; validacion de archivo (tipo/tamano/hash) fuera del webroot;
      nunca se autoaplica sin aprobacion (Fase 1 = manual); auditoria por evento; permisos separados (VER/EDITAR/INACTIVAR).
- [x] Ejecute `python tools/handoff.py check SGI REQ-0083` y paso sin errores.

Notas:

- Riesgo medio-alto (dinero: aplica cobros). Auditor: revisar aislamiento, anti-doble-aplicacion, validacion de
  archivo y que la aplicacion pase por el motor de caja (no duplica reglas). Fase 1 sin OCR/banco.

## Respuesta Por Observacion Cerrada

(No aplica: REQ nuevo.)
