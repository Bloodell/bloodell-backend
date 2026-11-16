package br.edu.cesar.bloodell.autenticacao.aplicacao;

import br.edu.cesar.bloodell.autenticacao.infraestrutura.*;
import br.edu.cesar.bloodell.unidade.infraestrutura.UnidadeRepository;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VinculoDeUnidadeService {
    private final VinculoDeUnidadeRepository vinculos;
    private final UsuarioRepository usuarios;
    private final UnidadeRepository unidades;
    private final BloqueioDeUsuarios bloqueio;
    private final AcessoUnidade acesso;

    public VinculoDeUnidadeService(VinculoDeUnidadeRepository vinculos, UsuarioRepository usuarios,
            UnidadeRepository unidades, BloqueioDeUsuarios bloqueio, AcessoUnidade acesso) {
        this.vinculos = vinculos;
        this.usuarios = usuarios;
        this.unidades = unidades;
        this.bloqueio = bloqueio;
        this.acesso = acesso;
    }

    @Transactional(readOnly = true)
    public Set<Long> consultar(Long usuarioId) {
        acesso.exigirAdministrador();
        conferirUsuario(usuarioId);
        return vinculos.unidadesDoUsuario(usuarioId);
    }

    @Transactional
    public Set<Long> substituir(Long usuarioId, Set<Long> ids) {
        bloqueio.bloquearEscritas();
        acesso.exigirAdministrador();
        conferirUsuario(usuarioId);
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new IllegalArgumentException("Informe IDs de unidades positivos.");
        for (Long id : ids) {
            if (!unidades.existsById(id)) throw RecursoNaoEncontradoException.de("Unidade", id);
        }
        vinculos.substituir(usuarioId, ids);
        return Set.copyOf(ids);
    }

    private void conferirUsuario(Long id) {
        if (!usuarios.existsById(id)) throw RecursoNaoEncontradoException.de("Usuario", id);
    }
}
