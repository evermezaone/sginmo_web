# Test Plan - REQ-0010
- [x] Alta de usuario -> debe_cambiar_password=true; login redirige a cambiar-password.
- [x] Reseteo por admin -> hash nuevo + flag + intentos/bloqueo limpios.
- [x] Version optimista en conflicto -> mensaje claro (probado; antes fallaba en silencio).
- [x] Grupos y permisos directos: alta/quita con anti-duplicado y refresco.
- [x] Validado manualmente por el usuario desde el ABM (2026-07-06).
