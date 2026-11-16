package br.edu.cesar.bloodell.estoque.aplicacao;

import br.edu.cesar.bloodell.autenticacao.aplicacao.AcessoUnidade;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.estoque.apresentacao.EstoqueDtos.EstoqueItemResposta;
import br.edu.cesar.bloodell.estoque.dominio.EstoqueItem;
import br.edu.cesar.bloodell.estoque.infraestrutura.EstoqueItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstoqueService {

    private final AcessoUnidade acesso;

    private final EstoqueItemRepository itens;

    public EstoqueService(EstoqueItemRepository itens, AcessoUnidade acesso) {
        this.acesso = acesso;
        this.itens = itens;
    }

    @Transactional(readOnly = true)
    public List<EstoqueItemResposta> listar(Long unidadeId, TipoComponente componente) {
        if (unidadeId != null) acesso.exigir(unidadeId);
        List<EstoqueItem> encontrados = acesso.filtrar(itens.listarComFiltros(unidadeId, componente));
        return encontrados.stream()
                .map(EstoqueService::paraResposta)
                .toList();
    }

    private static EstoqueItemResposta paraResposta(EstoqueItem item) {
        return new EstoqueItemResposta(
                item.getId(),
                item.getUnidade().getId(),
                item.getUnidade().getNome(),
                item.getTipoSanguineo().formatado(),
                item.getComponente().name(),
                item.getQuantidade(),
                item.getAtualizadoEm());
    }
}
