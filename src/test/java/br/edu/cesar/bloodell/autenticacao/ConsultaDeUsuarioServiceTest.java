package br.edu.cesar.bloodell.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.edu.cesar.bloodell.autenticacao.aplicacao.ConsultaDeUsuarioService;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class ConsultaDeUsuarioServiceTest {
    @Test
    void repositorioSemUsuariosRetornaPaginaVaziaComMetadados() {
        var usuarios = mock(UsuarioRepository.class);
        var paginacao = PageRequest.of(0, 20, Sort.by("id"));
        when(usuarios.findAll(paginacao)).thenReturn(Page.<Usuario>empty(paginacao));
        var pagina = new ConsultaDeUsuarioService(usuarios).listar(paginacao);
        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
        assertThat(pagina.getNumber()).isZero();
        assertThat(pagina.getSize()).isEqualTo(20);
    }
}
