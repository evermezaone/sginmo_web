# Propuesta de estándar ABM — SGInmo Web

**Estado:** en estudio final del usuario (pendiente de aprobación).
**Implementación de referencia:** ABM de Artículos (`articulos.xhtml`, `ArticuloBean`, `ArticuloService`).
**Una vez aprobado:** se formaliza en `.ai-handoff/standards/frontend-jsf-primefaces.md` y se replica en
todos los ABM del sistema (REQ-0007 geografía, REQ-0008 monedas/formas de pago/impuestos, y siguientes).

## 1. Grilla (listado)

| # | Funcionalidad | Detalle |
|---|---------------|---------|
| 1 | Paginación lazy real | Solo se trae de la BD la página visible (COUNT + LIMIT/OFFSET). Escala a millones de filas sin degradar. |
| 2 | Orden por columna | Clic en el título ordena asc/desc. Se ejecuta en la BD (whitelist de rutas JPQL, sin inyección). |
| 3 | Búsqueda global | Un solo campo que busca a la vez en código, descripción, tipo e impuesto. Dispara a los 400 ms de dejar de tipear (sin botón). |
| 4 | Búsqueda avanzada bajo demanda | Botón embudo junto al buscador: muestra filtros por columna — texto (contiene) en campos libres, combos de igualdad en dominios cerrados (Tipo, Impuesto, Estado). Al ocultarlos se limpian solos. |
| 5 | Selector de columnas | Botón que permite elegir qué columnas ver. Lo elegido afecta también la exportación. Pendiente REQ-0004: recordar la elección por usuario (`preferencia_usuario`). |
| 6 | Exportación CSV / XML / PDF | Exporta la **página visible** (pageOnly) respetando el selector de columnas; Acciones excluida. La exportación masiva del filtro completo será un REQ aparte con límite y proceso asíncrono. |
| 7 | Anchos según dominio | Código/Estado angostos y sin corte, Descripción flexible, Precio alineado a la derecha con separador de miles. |
| 8 | Estado visual | Etiqueta de color (verde ACTIVO / rojo INACTIVO). |
| 9 | Navegación | Botón Inicio (ícono casa) en la esquina superior. |

## 2. Edición (diálogo)

| # | Funcionalidad | Detalle |
|---|---------------|---------|
| 10 | Diálogo modal con pestañas | Principal / Clasificación / Stock y notas / Propiedades. Guardar y Cancelar SIEMPRE visibles (fuera de las pestañas). Siempre abre en Principal. |
| 11 | Alta y edición unificadas | El mismo formulario sirve para Nuevo y Editar. |
| 12 | Validaciones claras | En español, con la etiqueta visible del campo, sin duplicados. Obligatorios, unicidad de código y de aplicación con mensaje de negocio ("Ya existe un artículo con el código X"), coherencia stock mín ≤ máx. |
| 13 | Combos parametrizables | Poblados desde la tabla genérica `entidad` (agregar una opción = un INSERT, sin tocar código) y desde maestros (solo impuestos ACTIVOS). |
| 14 | Combos de entidad robustos | Converter administrado + igualdad por id en la entidad JPA (obligatorio para todo combo que seleccione una entidad). |
| 15 | Atributos parametrizables | Pestaña Propiedades: pares propiedad/valor definidos en `entidad`, con control de duplicados, habilitada tras el primer guardado, alta/baja sin salir del diálogo. |
| 16 | Baja lógica, nunca DELETE | Activar/Inactivar con diálogo de confirmación. El historial se conserva siempre. |
| 17 | Aviso de éxito | Growl con el nombre del registro creado/actualizado; el diálogo se cierra solo. |

## 3. Arquitectura y calidad (invisible pero clave)

| # | Característica | Detalle |
|---|----------------|---------|
| 18 | Capas estrictas | XHTML solo presenta; Bean `@ViewScoped` solo orquesta la UI; reglas de negocio en Service `@ApplicationScoped` transaccional; la entidad JPA mapea el subconjunto que usa el ABM y respeta los DEFAULT de la BD. |
| 19 | Seguridad de consultas | Orden y filtros pasan por whitelist de rutas JPQL; imposible inyectar por esos parámetros. |
| 20 | Auditoría automática | Toda escritura registra creado/modificado por + fecha vía `AuditoriaListener`, sin código en el ABM. |
| 21 | Idioma y formato | Locale `es` fijo (faces-config); números con separador de miles local. |
| 22 | Responsive | Diálogo y grilla usables desde el celular (validado en producción de prueba). |
| 23 | Deploy verificable | Script de deploy atómico a la VPS que falla ruidosamente si el redeploy no entra. |

## 4. Estudio final del usuario (2026-07-05): decisiones sobre las mejoras propuestas

### 4.1 Incorporadas YA al ABM de referencia

| Mejora | Cómo quedó |
|--------|-----------|
| Control de concurrencia optimista | Columna `version` en las 36 tablas (V4) + `@Version` en `Auditable`. Mensaje: "El registro fue modificado por otro usuario...". Toda tabla nueva nace con `version`; los SP de los motores que toquen tablas administradas por ABM deben hacer `SET version = version + 1`. |
| Duplicados detectados antes del submit | Chequeo remoto al salir de `codigo` y `aplicacion` (aviso WARN temprano). El Service y el UNIQUE de la BD siguen validando al guardar (triple capa). |
| Exportación con límite explícito | `pageOnly`: se exporta solo la página visible. Exportación masiva = REQ futuro (límite + job asíncrono + confirmación). |
| Debounce estandarizado | 400 ms en búsqueda global (rango estándar 400–600). PrimeFaces serializa la cola AJAX: las respuestas llegan en orden, una respuesta vieja no pisa una nueva. |
| Limpiar filtros | Botón siempre visible junto a la búsqueda avanzada: limpia global + columnas en un clic. |
| Filtros activos ocultos | Resuelto por diseño: al ocultar el panel avanzado los filtros SE LIMPIAN, no pueden quedar filtros invisibles aplicados. |
| Mensajes de vacío diferenciados | "No hay resultados para estos filtros" vs "No hay artículos cargados". |
| Teclado | Esc cierra el diálogo (`closeOnEscape`); la búsqueda dispara sola (Enter implícito); Tab sigue el orden natural del formulario. |
| Numéricos con máscara local | Ya cubierto: `p:inputNumber` muestra formato local y guarda decimal limpio. |
| Columnas mínimas no ocultables | Ya cubierto: columnas clave con `toggleable="false"` (Código, Acciones). |

### 4.2 Aceptadas — mapeadas a REQs (dependen de login o de los motores)

| Mejora | Dónde queda |
|--------|-------------|
| Vistas guardadas por usuario ("Mi vista") | REQ-0004: `preferencia_usuario` guarda columnas + orden + filtros + tamaño de página como vistas nombradas. |
| Persistencia temporal de página/filtros/orden | Dentro del diálogo ya se conserva (misma vista). Entre pantallas se implementa junto con "Mi vista" (REQ-0004) para que conviva con la regla "ocultar limpia". |
| Permisos por acción | REQ-0004: ver / crear / editar / inactivar / reactivar / exportar / ver auditoría como permisos separados (exportar ES un permiso). |
| Historial de cambios visible | REQ nuevo (auditoría por triggers en BD, coherente con la arquitectura BD-céntrica): tabla `auditoria_cambio` + pestaña "Historial" en maestros sensibles (quién, qué campo, valor anterior/nuevo, cuándo). |
| Motivo obligatorio al inactivar maestros sensibles | Mismo REQ de auditoría: el motivo se guarda en el historial. Aplica a articulo, forma_pago, parametro_sistema, usuario, listas que afecten cobros/documentos. |
| Validación de uso antes de inactivar | Se implementa cuando existan los motores que referencian (documento/cobro): advertir impacto siempre, bloquear solo si rompe operación activa. |
| Autocompletar lazy en combos grandes | Regla aceptada: combos con > ~50 opciones usan `p:autoComplete` lazy (primer caso real: ubicación geográfica, REQ-0007). |
| PDF con fecha/usuario/filtros; CSV estable para ETL | Con la exportación masiva / reportes JasperReports. CSV: UTF-8, separador y formato decimal documentados. |

### 4.3 Matizadas (disenso fundamentado)

- **DTOs Create/Update para todo ABM**: para maestros simples se mantiene la entidad
  subset-mapeada como modelo del formulario (menos código, menos mapeos que mantener, y la
  entidad ya respeta los DEFAULT de la BD). Los DTOs se reservan para flujos complejos
  (documentos, cobros, liquidaciones) donde el formulario NO es 1:1 con la tabla.
- **Suite de pruebas por ABM**: se acepta el espíritu, adaptado: prueba unitaria de la
  whitelist y validaciones del Service; la paginación/filtro/orden se prueba contra la BD
  real de la VPS (checklist del REQ). Testcontainers/integración completa se evalúa cuando
  haya CI (hoy no hay pipeline; el gate es handoff + revisión de Codex).

### 4.4 Contrato mínimo repetible de todo ABM (regla arquitectónica aceptada, adaptada a las convenciones del proyecto)

1. `Service.contar/listar(filtroGlobal, filtrosColumna, orden)` con **whitelist** de rutas JPQL.
2. `Service.guardar()` con `validar()` (unicidades con mensaje de negocio + coherencias) y chequeo `@Version` con mensaje claro.
3. `Service.existeXxx(valor, exceptoId)` por cada clave única (reutilizado por el chequeo remoto y por validar()).
4. `Service.cambiarEstado()` = baja/alta lógica (nunca DELETE).
5. Bean `@ViewScoped` solo orquesta; entidad con `equals/hashCode` por id.
6. Checklist de prueba del REQ: paginación, orden, filtros, unicidad, baja lógica, concurrencia.

## 5. Segunda ronda de estudio (2026-07-05, 14 observaciones del usuario)

**Decisión del usuario:** implementar YA el login (REQ-0004) → pulir el estándar con todo
este estudio → aprobar → replicar a todos los ABMs.

### 5.1 Reglas del estándar aceptadas (se aplican en el pulido post-login)

| # | Regla | Nota de implementación |
|---|-------|------------------------|
| 1 | `estado` (vigencia lógica) separado de `habilitado` (seleccionable para operaciones nuevas) | Se agrega `habilitado` SOLO donde aplica (forma_pago, articulo, rango_comprobante); no es columna universal. Los combos de operación filtran por habilitado; los históricos muestran todo. |
| 2 | Alcance del dato declarado: global / por empresa / por sucursal / mixto | Cada ABM lo declara en su REQ. moneda=global, forma_pago=global, rango_comprobante=empresa+sucursal, articulo=global (revisable). |
| 3 | Clonado de registros ("Duplicar") | Botón en Acciones: copia todo salvo claves únicas (codigo/aplicacion) y abre el diálogo en modo Nuevo; estado inicial ACTIVO sin guardar hasta confirmar. |
| 5 | Campos calculados solo lectura | El estándar exige documentar el origen (columna BD / vista / servicio / cálculo UI) y renderizarlos como salida, nunca como input deshabilitado editable por request. |
| 7 | Reactivación segura | `cambiarEstado(→ACTIVO)` re-ejecuta las validaciones de unicidad y reglas vigentes. Matiz: con UNIQUE total en BD (caso articulo.codigo) el conflicto es imposible por construcción; la regla es crítica donde la unicidad sea parcial (solo activos). |
| 8 | Errores técnicos traducidos | Mapper estándar en la capa Service: unique violation → mensaje de negocio; FK violation → "está en uso"; optimistic lock → "otro usuario modificó" (ya implementado). Nunca mensajes crudos de PostgreSQL. |
| 11 | Validación de dominios al guardar | El Service rechaza códigos de `entidad` inexistentes o inactivos aunque el combo "no debería" mandarlos (anti-manipulación de request / pantalla vieja). Excepción declarada: edición histórica. |
| 12 | Referencia estable en combos | Ya es el diseño (FKs compuestas lista+codigo; entidades por id). Queda como regla EXPLÍCITA: se guarda clave, jamás descripción. |
| 13 | Columnas obligatorias de toda grilla | código, descripción, estado + fecha_modificacion y usuario_modificacion (ocultas por defecto, disponibles en el selector de columnas — oro para soporte). |
| 14 | Verificación mínima de UI | Checklist manual obligatoria por REQ: abrir, buscar, filtrar, ordenar, crear, editar, inactivar/reactivar, exportar, responsive. Playwright cuando haya CI. |

### 5.2 Aceptadas como REQs propios (en orden de prioridad del usuario)

1. ~~Concurrencia optimista~~ — YA IMPLEMENTADA (V4 + @Version).
2. **Permisos por acción** + **modo solo lectura** (obs 9: mismo ABM abre en consulta sin
   botones según permiso, sin duplicar pantallas) → REQ-0004.
3. **Vistas/filtros persistentes** ("Mi vista") → REQ-0004.
4. **Validación de uso antes de inactivar** + **dependencias visibles** (obs 6: "aparece en
   12 documentos y 2 liquidaciones" — trazabilidad, no solo advertencia) → REQ con los motores.
5. **Historial visible** + motivo obligatorio al inactivar maestros sensibles → REQ auditoría (triggers en BD).
6. **Importación controlada** CSV/XLSX (prevalidación, vista previa de errores, todo-o-nada,
   reporte de rechazos, auditoría del archivo) → REQ propio; primeros: articulos, ubicaciones, entidades, personas.
7. **Auditoría de exportaciones** (quién, qué entidad, filtros, filas, formato) → REQ auditoría; requiere login.

## 6. Pendientes que no bloquean la aprobación

- Skeleton de carga por grilla (hoy: overlay estándar de PrimeFaces). Pulido menor.
- Filtro numérico por rango (ej. precio entre X e Y): se agrega si un ABM lo necesita.
