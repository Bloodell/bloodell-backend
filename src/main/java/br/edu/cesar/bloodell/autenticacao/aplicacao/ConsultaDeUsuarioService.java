package br.edu.cesar.bloodell.autenticacao.aplicacao;

import br.edu.cesar.bloodell.autenticacao.apresentacao.AutenticacaoDtos.UsuarioAtual;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "bloodell.seguranca.autenticacao-habilitada", havingValue = "true")
@Transactional(readOnly = true)
public class ConsultaDeUsuarioService {
    private final UsuarioRepository usuarios;

    public ConsultaDeUsuarioService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    public Page<UsuarioAtual> listar(Pageable paginacao) {
        return usuarios.findAll(paginacao).map(UsuarioAtual::de);
    }

    public UsuarioAtual buscar(Long id) {
        return usuarios.findById(id).map(UsuarioAtual::de)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", id));
    }
}
