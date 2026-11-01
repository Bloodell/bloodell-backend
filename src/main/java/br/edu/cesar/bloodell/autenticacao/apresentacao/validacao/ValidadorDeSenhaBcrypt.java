package br.edu.cesar.bloodell.autenticacao.apresentacao.validacao;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class ValidadorDeSenhaBcrypt implements ConstraintValidator<SenhaBcrypt, String> {
    @Override
    public boolean isValid(String senha, ConstraintValidatorContext contexto) {
        // Obrigatoriedade e tamanho minimo sao tratados por NotBlank e Size.
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
