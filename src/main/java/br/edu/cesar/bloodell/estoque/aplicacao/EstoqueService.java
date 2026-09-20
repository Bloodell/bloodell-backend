package br.edu.cesar.bloodell.estoque.aplicacao;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.estoque.apresentacao.EstoqueDtos.EstoqueItemResposta;
import br.edu.cesar.bloodell.estoque.dominio.EstoqueItem;
import br.edu.cesar.bloodell.estoque.infraestrutura.EstoqueItemRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstoqueService {

    private final EstoqueItemRepository itens;

    public EstoqueService(EstoqueItemRepository itens) {
        this.itens = itens;
    }

    @Transactional(readOnly = true)
    public List<EstoqueItemResposta> listar(Long unidadeId, TipoComponente componente) {
        List<EstoqueItem> encontrados = itens.listarComFiltros(unidadeId, componente);
        List<EstoqueItemResposta> respostas = new ArrayList<>(encontrados.size());
        for (EstoqueItem item : encontrados) {
            respostas.add(new EstoqueItemResposta(
                    item.getId(),
                    item.getUnidade().getId(),
                    item.getUnidade().getNome(),
                    item.getTipoSanguineo().formatado(),
                    item.getComponente().name(),
                    item.getQuantidade(),
                    item.getAtualizadoEm()));
        }
        return respostas;
    }
}
