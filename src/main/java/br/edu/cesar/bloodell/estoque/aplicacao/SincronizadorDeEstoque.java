package br.edu.cesar.bloodell.estoque.aplicacao;

import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.estoque.dominio.EstoqueItem;
import br.edu.cesar.bloodell.estoque.infraestrutura.EstoqueItemRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SincronizadorDeEstoque {

    private final EstoqueItemRepository itens;

    public SincronizadorDeEstoque(EstoqueItemRepository itens) {
        this.itens = itens;
    }

    public void registrarEntrada(Bolsa bolsa) {
        localizarOuCriar(bolsa).somar(1);
    }

    public void registrarSaida(Bolsa bolsa) {
        localizarOuCriar(bolsa).somar(-1);
    }

    private EstoqueItem localizarOuCriar(Bolsa bolsa) {
        Optional<EstoqueItem> encontrado = itens
                .findByUnidadeIdAndComponenteAndTipoSanguineo_AboAndTipoSanguineo_Rh(
                        bolsa.getUnidadeAtual().getId(),
                        bolsa.getComponente(),
                        bolsa.getTipoSanguineo().getAbo(),
                        bolsa.getTipoSanguineo().getRh());
        return encontrado.orElseGet(() -> itens.save(new EstoqueItem(
                bolsa.getUnidadeAtual(), bolsa.getTipoSanguineo(), bolsa.getComponente())));
    }
}
