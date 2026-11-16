package br.edu.cesar.bloodell.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.autenticacao.apresentacao.UsuarioDtos;
import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class UsuarioDtosTest {
    @Test
    void senhaNaoApareceNoToStringNemNaSerializacaoDoCadastro() {
        String senha = "Senha inicial sintetica!";
        var entrada = new UsuarioDtos.Cadastro("  Equipe  ", "  EQUIPE@EXAMPLE.TEST  ", Perfil.LOGISTICA, senha);
        assertThat(entrada.nome()).isEqualTo("Equipe");
        assertThat(entrada.email()).isEqualTo("equipe@example.test");
        assertThat(entrada.senha()).isEqualTo(senha);
        assertThat(entrada.toString()).doesNotContain(senha);
        assertThat(new ObjectMapper().writeValueAsString(entrada)).doesNotContain(senha, "\"senha\"");
    }
}
