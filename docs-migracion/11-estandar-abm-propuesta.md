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
| 6 | Exportación CSV / XML / PDF | Exporta lo visible (respeta el selector de columnas). La columna Acciones queda excluida siempre. |
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

## 4. Pendientes conocidos del estándar (no bloquean la aprobación)

- **Preferencias por usuario** (columnas visibles, y a futuro filas por página, último filtro): requiere login → registrado como alcance del REQ-0004 (`preferencia_usuario`).
- **Permisos por rol sobre botones** (quién ve Nuevo/Editar/Inactivar/Exportar): REQ-0004.
- **PDF con estilo corporativo** (logo, título, pie): hoy exporta tabla simple; los reportes formales serán JasperReports (REQ propio).
- **Filtro numérico por rango** (ej. precio entre X e Y): excluido a propósito por simplicidad; se agrega si un ABM lo necesita.
