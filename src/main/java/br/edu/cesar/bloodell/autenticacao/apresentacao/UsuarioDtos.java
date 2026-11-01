package br.edu.cesar.bloodell.autenticacao.apresentacao;

import br.edu.cesar.bloodell.autenticacao.apresentacao.validacao.SenhaBcrypt;
import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UsuarioDtos {
    private UsuarioDtos() { }

    public record Cadastro(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotNull Perfil perfil,
            @NotBlank @Size(min = 12, max = 72) @SenhaBcrypt
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password",
                    description = "Senha inicial: pelo menos 12 caracteres e no maximo 72 bytes UTF-8.")
            String senha) {
        public Cadastro {
            nome = nome == null ? null : nome.trim();
            email = email == null ? null : Usuario.normalizarEmail(email);
        }

        @Override
        public String toString() { return "Cadastro[credenciais omitidas]"; }
    }

    @Schema(description = "Atualizacao parcial: somente nome, email e perfil. Campos omitidos sao preservados; null e campos extras sao recusados.")
    public static final class Atualizacao {
        @Size(min = 1, max = 120)
        private String nome;
        @Email @Size(min = 1, max = 160)
        private String email;
        private Perfil perfil;

        public String getNome() { return nome; }
        public String getEmail() { return email; }
        public Perfil getPerfil() { return perfil; }

        @JsonSetter(nulls = Nulls.FAIL)
        public void setNome(String nome) { this.nome = nome.trim(); }
        @JsonSetter(nulls = Nulls.FAIL)
        public void setEmail(String email) { this.email = Usuario.normalizarEmail(email); }
        @JsonSetter(nulls = Nulls.FAIL)
        public void setPerfil(Perfil perfil) { this.perfil = perfil; }

        @JsonAnySetter
        public void recusarCampoAdicional(String campo, Object valor) {
            throw new IllegalArgumentException("Somente nome, email e perfil podem ser atualizados.");
        }

        @JsonIgnore
        @Schema(hidden = true)
        @AssertTrue(message = "informe ao menos nome, email ou perfil")
        public boolean isPossuiAlteracao() { return nome != null || email != null || perfil != null; }

        @Override
        public String toString() { return "Atualizacao[dados omitidos]"; }
    }
}
