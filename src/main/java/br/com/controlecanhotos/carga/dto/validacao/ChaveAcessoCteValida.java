package br.com.controlecanhotos.carga.dto.validacao;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Chave de acesso de CT-e válida (ver {@link br.com.controlecanhotos.carga.ChaveAcessoCte}). Nulo é aceito. */
@Documented
@Constraint(validatedBy = ChaveAcessoCteValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface ChaveAcessoCteValida {

	String message() default "Chave de acesso de CT-e inválida";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
