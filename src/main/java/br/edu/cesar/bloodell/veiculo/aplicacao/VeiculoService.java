package br.edu.cesar.bloodell.veiculo.aplicacao;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.excecao.ConflitoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.unidade.aplicacao.UnidadeService;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.veiculo.apresentacao.VeiculoDtos.VeiculoRequisicao;
import br.edu.cesar.bloodell.veiculo.apresentacao.VeiculoDtos.VeiculoResposta;
import br.edu.cesar.bloodell.veiculo.dominio.StatusVeiculo;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.veiculo.infraestrutura.VeiculoRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculos;
    private final UnidadeService unidadeService;

    public VeiculoService(VeiculoRepository veiculos, UnidadeService unidadeService) {
        this.veiculos = veiculos;
        this.unidadeService = unidadeService;
    }

    @Transactional
    public VeiculoResposta cadastrar(VeiculoRequisicao entrada) {
        garantirPlacaLivre(entrada.placa(), null);
        Veiculo veiculo = new Veiculo(entrada.placa(), buscarHemocentro(entrada.hemocentroBaseId()),
                entrada.capacidadeBolsas(), new CoordenadaGeo(entrada.latitude(), entrada.longitude()));
        return paraResposta(veiculos.save(veiculo));
    }

    @Transactional(readOnly = true)
    public List<VeiculoResposta> listar(StatusVeiculo status) {
        List<Veiculo> encontrados = status == null
                ? veiculos.findAllByOrderByPlaca()
                : veiculos.findByStatusOrderByPlaca(status);
        List<VeiculoResposta> respostas = new ArrayList<>(encontrados.size());
        for (Veiculo veiculo : encontrados) {
            respostas.add(paraResposta(veiculo));
        }
        return respostas;
    }

    @Transactional(readOnly = true)
    public VeiculoResposta detalhar(Long id) {
        return paraResposta(buscarEntidade(id));
    }

    @Transactional
    public VeiculoResposta atualizar(Long id, VeiculoRequisicao entrada) {
        Veiculo veiculo = buscarEntidade(id);
        garantirPlacaLivre(entrada.placa(), id);
        veiculo.atualizarCadastro(entrada.placa(), buscarHemocentro(entrada.hemocentroBaseId()),
                entrada.capacidadeBolsas(), new CoordenadaGeo(entrada.latitude(), entrada.longitude()));
        return paraResposta(veiculo);
    }

    /**
     * Veiculo com entregas ou telemetria registradas nao e apagado: esses registros o
     * referenciam com ON DELETE RESTRICT e fazem parte da trilha de cadeia fria.
     */
    @Transactional
    public void remover(Long id) {
        Veiculo veiculo = buscarEntidade(id);
        if (veiculo.emRota()) {
            throw new RegraDeNegocioException("Veiculo " + veiculo.getPlaca() + " esta em rota e nao pode ser removido.");
        }
        try {
            veiculos.delete(veiculo);
            veiculos.flush();
        } catch (DataIntegrityViolationException excecao) {
            throw new ConflitoException("Veiculo " + veiculo.getPlaca()
                    + " possui entregas ou telemetria registradas e nao pode ser removido.");
        }
    }

    @Transactional(readOnly = true)
    public Veiculo buscarEntidade(Long id) {
        return veiculos.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Veiculo", id));
    }

    private Unidade buscarHemocentro(Long id) {
        Unidade base = unidadeService.buscarEntidade(id);
        if (!"HEMOCENTRO".equals(base.tipo())) {
            throw new RegraDeNegocioException("A base do veiculo precisa ser um hemocentro. A unidade "
                    + base.getNome() + " e um " + base.tipo().toLowerCase() + ".");
        }
        return base;
    }

    private void garantirPlacaLivre(String placa, Long idAtual) {
        veiculos.findByPlaca(placa.trim().toUpperCase())
                .filter(encontrado -> !encontrado.getId().equals(idAtual))
                .ifPresent(encontrado -> { throw new ConflitoException(
                        "Ja existe um veiculo com a placa " + encontrado.getPlaca() + "."); });
    }

    private static VeiculoResposta paraResposta(Veiculo veiculo) {
        return new VeiculoResposta(
                veiculo.getId(),
                veiculo.getPlaca(),
                veiculo.getHemocentroBase().getId(),
                veiculo.getHemocentroBase().getNome(),
                veiculo.getCapacidadeBolsas(),
                veiculo.getStatus().name(),
                veiculo.getPosicaoAtual().getLatitude(),
                veiculo.getPosicaoAtual().getLongitude());
    }
}
