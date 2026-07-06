# REQ-0009 - Empresas y sucursales

**Numero:** REQ-0009 · **Estado:** implementado (2026-07-06), pendiente validacion visual del usuario

## Objetivo Funcional
ABM de empresas operadoras (empresa = persona juridica con rol EMPRESA activo) con sus
sucursales, y CONTEXTO empresa/sucursal en sesion (diferido de REQ-0005): la barra superior
muestra la empresa del usuario y un selector de sucursal activa recordado por usuario.

## Criterios De Aceptacion
- [x] ABM empresas (pantalla 'empresas'): grilla lazy (razon social, RUC+DV, telefono,
      estado), dialogo con pestanas Principal / Sucursales.
- [x] Alta de empresa crea persona (JURIDICA) + persona_juridica + rol EMPRESA en una
      transaccion (cascade con PK compartida via @MapsId); RUC unico con mensaje claro.
- [x] Baja logica sobre persona.estado; enforcement de permisos en EmpresaService.
- [x] Sucursales por empresa: alta con descripcion/direccion/telefono, "por defecto" unica
      por empresa, baja logica (estado agregado en V13).
- [x] Contexto en sesion (ContextoEmpresa @SessionScoped): empresa del usuario logueado +
      selector de sucursal activa en la plantilla; la eleccion se recuerda por usuario en
      preferencia_usuario (clave sucursal_activa); expone sucursal() para los motores futuros.
- [x] V13 (APLICADA): sucursal.estado, pantalla 'empresas', rol EMPRESA y Casa matriz para
      la empresa placeholder del seed.
