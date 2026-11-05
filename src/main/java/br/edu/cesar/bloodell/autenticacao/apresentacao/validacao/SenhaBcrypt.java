package br.edu.cesar.bloodell.autenticacao.apresentacao.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ValidadorDeSenhaBcrypt.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaBcrypt {
    String message() default "deve conter no maximo 72 bytes em UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
