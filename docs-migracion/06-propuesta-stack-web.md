# 06 — Stack de desarrollo definido: WildFly + Jakarta EE + PrimeFaces

**Decisión (2026-07-03):** la aplicación web se desarrolla en Java sobre **WildFly 40** con **PrimeFaces** como framework de UI, base de datos **PostgreSQL** y **autenticación integrada** (Jakarta Security). SGInmo es un proyecto independiente (no comparte stack ni infraestructura con otros sistemas del usuario).

## Stack definido

| Capa | Tecnología | Notas |
|---|---|---|
| Servidor de aplicaciones | **WildFly 40** (mayo 2026, **Jakarta EE 11**) | datasource PostgreSQL administrado por el servidor; credenciales fuera del código (vault/variables) |
| Lenguaje / runtime | **Java 21 LTS** | WildFly 40 certifica EE 11 sobre Java SE 17 y 21 |
| UI | **Jakarta Faces 4.1 + PrimeFaces 15** | tema corporativo; `p:dataTable` lazy + `p:dialog` reemplazan FrmMaestro/FrmBuscadorGenerico |
| Inyección / servicios | **CDI 4.1** (beans `@ApplicationScoped`/`@ViewScoped`) | los 23 Services del legado se reescriben como servicios CDI |
| Transacciones | **`@Transactional` (JTA)** | corrige la deuda crítica: cobro, operación+cuotas, liquidación y anulación como unidades atómicas |
| Persistencia | **JPA 3.2 (Hibernate)** | entidades desde las 31 tablas (doc 02); reemplaza EF5/EDMX |
| Base de datos | **PostgreSQL 16** | ETL único desde Firebird (ver abajo) |
| Seguridad | **Jakarta Security** integrada | login propio; contraseñas con **PBKDF2/bcrypt** (elimina el Base64 reversible); roles en BD (`ADMINISTRADOR`, `USUARIO`, ampliable); autorización declarativa en backend (`@RolesAllowed`), no solo en UI |
| Reportes | **JasperReports** (.jrxml) | reemplazo natural de los 17 Crystal Reports; export PDF/XLSX; visor embebido o descarga |
| Imágenes / archivos | filesystem del servidor (o S3 compatible) | reemplaza BLOBs; se guarda la ruta/URL |
| Build | **Maven** (WAR único) | módulos opcionales: `-core` (dominio/servicios) y `-web` (JSF) |
| Logging | SLF4J + Logback (o el subsistema de WildFly) | reemplaza log4net/Trace |
| Auditoría | listeners JPA (`@PrePersist`/`@PreUpdate`) | conserva USUARIO/FECHA_CREACION/MODIFICACION automáticamente |

## Mapeo legado → nuevo

| Legado (WinForms) | Nuevo (Jakarta EE / PrimeFaces) |
|---|---|
| FrmMaestro (base ABM) | plantilla Facelets + composite components; `p:dataTable` con `LazyDataModel` |
| FrmBuscadorGenerico / ~15 buscadores | `p:dialog` reutilizable con búsqueda lazy, o `p:autoComplete` |
| Menú MDI por perfil (`Principal.cs`) | layout con `p:menubar` filtrado por rol + `@RolesAllowed` en servicios |
| VARIABLES_ENTORNOS (empresa/sucursal activa) | sesión (`@SessionScoped` UserContext) + filtro/scope por `empresa_id` en consultas |
| Services C# | servicios CDI `@Transactional` (misma división: Cobros, Operaciones, CronogramasCuotas, Liquidaciones…) |
| DAOs genéricos EF | repositorios JPA (`EntityManager` + criteria) |
| Crystal Reports (17) | JasperReports (.jrxml) con los mismos parámetros (doc 05 §9) |
| Contrato/pagaré con monto en letras | plantilla Jasper + utilidad Java "número a letras" (portar `Convertir_Numero`) |
| Dígito verificador módulo 11 | utilidad Java (portar tal cual, doc 04 §9) |
| MessageBox (`PysistemasMensajes`) | `FacesMessage` / `p:growl` |
| `.ini` con conexión | datasource JNDI en WildFly (`java:/jdbc/SGInmoDS`) |

## Migración de datos: Firebird → PostgreSQL

ETL único al momento del corte (el mapeo completo está en el doc 02). Normalizaciones en el paso:
- `varchar "SI"/"NO"` → `boolean`
- Estados string → enums (columnas `varchar` + `@Enumerated(STRING)` en JPA, o tipos enum de PG)
- `numeric(15,0)` → `numeric(15,2)` (habilita decimales/multimoneda futura)
- BLOBs de imágenes → archivos en disco + columna de ruta
- Corregir typo `FECHA_MODIFCACION` (CIUDADES_DEPARTAMENTOS)
- `USUARIOS.PASSWORD`: **no migrable directamente** — como el "cifrado" actual es reversible, se pueden desencriptar y re-hashear con bcrypt en el ETL, o forzar reset de contraseñas
- **Exportar la tabla DOMINIOS de producción antes de diseñar** (ahí viven enumeraciones y parámetros: comisiones, mora, días de gracia)
- Las vistas `*_VIEW` no se migran: se reemplazan por consultas JPA; las 2 con lógica (contratos vencidos, operaciones pendientes) como named queries

## Correcciones de negocio a incorporar (no replicar los bugs del legado)

1. Transacciones JTA en cobro, operación+cuotas, liquidación, anulación (hoy inexistentes).
2. Renovación de contratos: cerrar el anterior sin duplicar cuotas (bug actual reconocido en el código).
3. Redondeo con `BigDecimal` (nada de truncar); diferencia de división ajustada a la última cuota como hoy.
4. Multimoneda real o decisión explícita de operar solo en Gs. (hoy MONEDA_ID=1 fijo).
5. Unicidad a nivel BD: una liquidación por operación; documento de socio único.
6. Autorización en backend (`@RolesAllowed`), no solo ocultar menús.
7. Rate limiting / bloqueo por intentos fallidos en el login.

## Infraestructura

WildFly requiere un **VPS o servidor dedicado** (JVM): no corre en hosting compartido. Mínimo razonable: 2 vCPU / 4 GB RAM con WildFly + PostgreSQL en la misma máquina al inicio; HTTPS con reverse proxy (nginx/Caddy) delante.

## Fases sugeridas

1. **Fase 0** — validar reglas (docs 02-05) con el negocio; exportar DOMINIOS y datos reales; provisionar VPS + WildFly + PostgreSQL.
2. **Fase 1** — esqueleto: proyecto Maven, datasource, login + roles + contexto empresa/sucursal, layout PrimeFaces, catálogos (Mantenimiento completo).
3. **Fase 2** — núcleo: socios de negocios, entidades, propiedades/lotes, operaciones + cronograma de cuotas.
4. **Fase 3** — dinero: cobros con mora, ingresos/egresos, liquidaciones, renovaciones/rescisiones.
5. **Fase 4** — reportes JasperReports (17) + dashboard de vencimientos.
6. **Fase 5** — ETL de datos, corrida en paralelo con el legado, corte.
