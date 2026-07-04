# Estandar Backend — Jakarta EE / CDI / JPA (SGInmo Web)

## Arquitectura

- Lógica de negocio SOLO en servicios CDI (`@ApplicationScoped`) bajo `py.com.pysistemas.sginmo.servicio`.
- Los backing beans JSF (`@ViewScoped`/`@RequestScoped`, paquete `web`) orquestan UI: no calculan montos, no cambian estados, no validan reglas de negocio.
- Repositorios/DAO con `EntityManager` inyectado; consultas con named queries o Criteria, siempre parametrizadas. Prohibido concatenar SQL/JPQL con input.

## Transacciones (regla crítica del proyecto)

- Toda operación multi-tabla va en UN método de servicio anotado `@Transactional`: guardar cobro (cabecera + detalles + estado de cuotas), guardar operación (+ cronograma + movimientos automáticos + estado de propiedad), liquidación (+ detalles + cierre de operación + liberación de propiedad), anulación de cobro (reversa de cuotas).
- El estado de PROPIEDAD se deriva SIEMPRE dentro de la misma transacción que la operación que lo causa (invariante: nunca un SP de "corrección" como en el legado).
- Renovación: cerrar contrato anterior y crear el nuevo en la misma transacción, sin duplicar cuotas (bug del legado que NO se replica).

## Dinero y cálculos

- `BigDecimal` en todo cálculo monetario; escala 2, `RoundingMode.HALF_UP` salvo regla documentada.
- Diferencias de división del cronograma se ajustan en la ÚLTIMA cuota (RN-CUO-002).
- Mora: `dias = fechaCobro - (vencimiento + diasGracia)`; `mora = dias * montoMoraDia` (RN-COBR-001). Fuente de defaults: parámetros (doc 07 §2).

## Estados y tipos

- Enums Java con `@Enumerated(EnumType.STRING)`, valores EXACTOS del doc 07 §3 (ej.: `EstadoCuota {PENDIENTE, CANCELADO}`, `TipoOperacion {ALQUILER, VENTA}`, `TipoPersoneria {PERFIS, PERJUR}`).
- Máquinas de estado del doc 00: toda transición inválida debe lanzar excepción de negocio, no ignorarse.

## Seguridad

- `@RolesAllowed`/checks en la capa de servicio para TODA acción sensible (anular cobro, descuentos, regenerar cuotas, parámetros). Ocultar el botón no es control.
- Contraseñas: hash bcrypt/PBKDF2 vía Jakarta Security. Nunca reversible, nunca logueada.
- Sin credenciales/secrets en código ni en tests: datasource JNDI + `.env`/config del servidor.

## Validación y errores

- Bean Validation (`@NotNull`, `@Size`, etc.) en entidades/DTOs + validaciones de negocio en servicio.
- Excepciones de negocio tipadas (`NegocioException`) con mensaje para el usuario; el bean JSF las convierte en `FacesMessage`.

## Auditoría

- `Auditable` (mapped superclass) con usuario/fecha creación/modificación poblados por listener JPA desde el contexto de sesión. Prohibido setearlos a mano en beans.

## Trazabilidad

- Todo método que implementa una regla documentada cita en Javadoc el ID (`RN-COBR-001`), el archivo legado (`FrmCobros.cs`) o el SP (`RPT_*`) del que proviene.
