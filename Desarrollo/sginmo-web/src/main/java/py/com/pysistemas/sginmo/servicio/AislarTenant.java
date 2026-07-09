package py.com.pysistemas.sginmo.servicio;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un service (o metodo) para que, dentro de su transaccion, se fije el tenant del
 * usuario en la sesion de PostgreSQL (app.tenant) para que las politicas RLS (V28) filtren
 * por tenant. F5 (REQ-0037). Debe ir junto a @Transactional para que exista una tx donde el
 * SET LOCAL aplique.
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AislarTenant {
}
