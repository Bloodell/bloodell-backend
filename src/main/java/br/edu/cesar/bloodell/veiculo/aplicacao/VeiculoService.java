package br.edu.cesar.bloodell.veiculo.aplicacao;

import br.edu.cesar.bloodell.veiculo.apresentacao.VeiculoDtos.VeiculoResposta;
import br.edu.cesar.bloodell.veiculo.dominio.StatusVeiculo;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.veiculo.infraestrutura.VeiculoRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculos;

    public VeiculoService(VeiculoRepository veiculos) {
        this.veiculos = veiculos;
    }

    @Transactional(readOnly = true)
    public List<VeiculoResposta> listar(StatusVeiculo status) {
        List<Veiculo> encontrados = status == null
                ? veiculos.findAllByOrderByPlaca()
                : veiculos.findByStatusOrderByPlaca(status);
        List<VeiculoResposta> respostas = new ArrayList<>(encontrados.size());
        for (Veiculo veiculo : encontrados) {
            respostas.add(new VeiculoResposta(
                    veiculo.getId(),
                    veiculo.getPlaca(),
                    veiculo.getHemocentroBase().getId(),
                    veiculo.getHemocentroBase().getNome(),
                    veiculo.getCapacidadeBolsas(),
                    veiculo.getStatus().name(),
                    veiculo.getPosicaoAtual().getLatitude(),
                    veiculo.getPosicaoAtual().getLongitude()));
        }
        return respostas;
    }
}
