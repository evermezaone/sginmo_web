# REQ-0004 - Seguridad: login, bcrypt, roles y bloqueo de intentos

**Numero:** REQ-0004
**Fecha de creacion:** 2026-07-04
**Estado inicial:** NUEVO
**Prioridad:** no indicada

## Texto Original

[Copiar el mensaje exacto del usuario aqui]

## Objetivo Funcional

[Que debe poder hacer el usuario o el sistema]

## Criterios De Aceptacion

- [ ] Criterio 1
- [ ] Criterio 2
- [ ] Criterio 3

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
