# 01 — Stack tecnológico actual (legado)

## Componentes

| Capa | Tecnología | Versión | Observación |
|---|---|---|---|
| Runtime | .NET Framework | **4.0** (2010) | Sin soporte; VS2017 |
| Lenguaje | C# | — | ~330 archivos .cs, ~2.7 MB de código |
| UI | Windows Forms | — | ~100 formularios en 10 módulos; MDI con menú por perfil |
| ORM | Entity Framework | **5.0** (EDMX database-first) | `Modelo\Model1.edmx`; 31 tablas + ~23 vistas mapeadas |
| Base de datos | **Firebird** | cliente FirebirdClient 5.11 | Archivo `inmobiliaria.fdb`; charset UTF8 |
| Reportes | **Crystal Reports** | runtime 12/14 | ~17 reportes .rpt; exportación a PDF |
| Logging | log4net 1.2.10 + System.Diagnostics.Trace | | Log a `Inmobiliaria.log` |
| JSON/HTTP | Newtonsoft.Json 12, helper REST propio (`RestUtility`) | | Basic Auth |

## Arquitectura

Tres capas informales dentro de un único proyecto (`Inmobiliaria.csproj`):

```
Presentacion (WinForms)  →  Services (23 servicios)  →  Modelo (EF5 EDMX + DAOs genéricos)  →  Firebird
                          ↘  Utilidades (seguridad, formatos, validaciones)
Reportes (Crystal .rpt) ← consultas/vistas de la BD
```

- **Patrón DAO genérico**: `ObtenerDesdeFiltros(Expression<Func<T,bool>>)`, `Agregar`, `Actualizar`, `Eliminar`, `Guardar`.
- **Mucha regla de negocio vive en los formularios** (event handlers), no en Services — documentada en el doc 03.
- **Multi-tenancy por aplicación**: la empresa/sucursal activa se guarda en `VARIABLES_ENTORNOS` por usuario y se usa como filtro en las consultas.
- Conexión **directa cliente → BD** (2 capas reales). Las cadenas comentadas en `app.config` muestran que llegó a usarse por internet contra IP pública y `www.pysistemas.com`.

## Riesgos y alertas de seguridad

| # | Hallazgo | Ubicación | Severidad |
|---|---|---|---|
| 1 | Credenciales BD hardcodeadas `sysdba/masterkey` (también en generador de .ini por defecto) | `app.config:87`, `PysistemasConfiguraciones` | CRÍTICA |
| 2 | IPs de producción y dominio expuestos en cadenas comentadas | `app.config:85,88` | ALTA |
| 3 | "Encriptación" de contraseñas = Base64(UTF-16), reversible | `Utilidades\Seguridad\PysistemasSeguridad.cs` | CRÍTICA |
| 4 | Sin límite de intentos de login ni bloqueo de cuenta | `IngresoSistemaService` | ALTA |
| 5 | BD expuesta directamente al cliente (sin capa de servicios remota) | arquitectura | ALTA |
| 6 | Ruta de BD hardcodeada `c:\inmobiliaria.fdb` | `app.config`, `PysistemasConfiguraciones` | MEDIA |

## Por qué ninguna pieza migra directo

- **.NET 4.0 → .NET moderno**: el código de servicios es portable con ajustes, pero EDMX no existe en EF Core (habría que re-scaffoldear el modelo).
- **WinForms → web**: reescritura total de UI; además gran parte de las reglas está en los forms y hay que extraerla (ya documentada en docs 03-05).
- **Crystal Reports**: sin camino razonable en web moderna; rehacer los ~17 reportes con otra herramienta (ver doc 06).
- **Firebird**: soportable pero minoritario; conviene migrar a un motor con mejor ecosistema web (ver doc 06).
