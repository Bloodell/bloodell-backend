package br.edu.cesar.bloodell.unidade.aplicacao;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import br.edu.cesar.bloodell.compartilhado.excecao.ConflitoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.cesar.bloodell.unidade.apresentacao.UnidadeDtos.UnidadeRequisicao;
import br.edu.cesar.bloodell.unidade.apresentacao.UnidadeDtos.UnidadeResposta;
import br.edu.cesar.bloodell.unidade.dominio.Hemocentro;
import br.edu.cesar.bloodell.unidade.dominio.Hospital;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.unidade.infraestrutura.UnidadeRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadeService {

    private final UnidadeRepository unidades;

    public UnidadeService(UnidadeRepository unidades) {
        this.unidades = unidades;
    }

    @Transactional
    public UnidadeResposta cadastrarHospital(UnidadeRequisicao entrada) {
        garantirNomeLivre(entrada.nome());
        Hospital hospital = new Hospital(entrada.nome(), criarEndereco(entrada), criarCoordenada(entrada));
        return paraResposta(unidades.save(hospital));
    }

    @Transactional
    public UnidadeResposta cadastrarHemocentro(UnidadeRequisicao entrada) {
        garantirNomeLivre(entrada.nome());
        Hemocentro hemocentro = new Hemocentro(entrada.nome(), criarEndereco(entrada), criarCoordenada(entrada));
        return paraResposta(unidades.save(hemocentro));
    }

    @Transactional(readOnly = true)
    public List<UnidadeResposta> listarHospitais() {
        return converter(unidades.listarHospitais());
    }

    @Transactional(readOnly = true)
    public List<UnidadeResposta> listarHemocentros() {
        return converter(unidades.listarHemocentros());
    }

    @Transactional(readOnly = true)
    public UnidadeResposta buscarHospital(Long id) {
        return paraResposta(buscarEntidade(id, Hospital.class, "Hospital"));
    }

    @Transactional(readOnly = true)
    public UnidadeResposta buscarHemocentro(Long id) {
        return paraResposta(buscarEntidade(id, Hemocentro.class, "Hemocentro"));
    }

    @Transactional
    public UnidadeResposta atualizarHospital(Long id, UnidadeRequisicao entrada) {
        return atualizar(id, entrada, Hospital.class, "Hospital");
    }

    @Transactional
    public UnidadeResposta atualizarHemocentro(Long id, UnidadeRequisicao entrada) {
        return atualizar(id, entrada, Hemocentro.class, "Hemocentro");
    }

    @Transactional
    public void removerHemocentro(Long id) {
        remover(id, "HEMOCENTRO", "Hemocentro");
    }

    @Transactional
    public void removerHospital(Long id) {
        remover(id, "HOSPITAL", "Hospital");
    }

    /**
     * Remove apenas unidades sem historico. Doacoes, bolsas, requisicoes, veiculos e
     * entregas referenciam a unidade com ON DELETE RESTRICT: apagar a unidade
     * quebraria a rastreabilidade, entao o banco recusa e a API responde 409.
     */
    private void remover(Long id, String tipoEsperado, String recurso) {
        Unidade unidade = unidades.findById(id)
                .filter(encontrada -> tipoEsperado.equals(encontrada.tipo()))
                .orElseThrow(() -> RecursoNaoEncontradoException.de(recurso, id));
        try {
            unidades.delete(unidade);
            unidades.flush();
        } catch (DataIntegrityViolationException excecao) {
            throw new ConflitoException(recurso + " " + unidade.getNome()
                    + " possui registros vinculados e nao pode ser removido(a).");
        }
    }

    @Transactional(readOnly = true)
    public Unidade buscarEntidade(Long id) {
        return unidades.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", id));
    }

    private Unidade buscarEntidade(Long id, Class<? extends Unidade> tipo, String recurso) {
        return unidades.findById(id)
                .filter(tipo::isInstance)
                .orElseThrow(() -> RecursoNaoEncontradoException.de(recurso, id));
    }

    private UnidadeResposta atualizar(Long id, UnidadeRequisicao entrada,
                                      Class<? extends Unidade> tipo, String recurso) {
        Unidade unidade = buscarEntidade(id, tipo, recurso);
        garantirNomeLivreParaAtualizacao(id, entrada.nome());
        unidade.atualizar(entrada.nome(), criarEndereco(entrada), criarCoordenada(entrada));
        return paraResposta(unidade);
    }

    private void garantirNomeLivre(String nome) {
        if (unidades.findByNomeIgnoreCase(nome.trim()).isPresent()) {
            throw new ConflitoException("Ja existe uma unidade chamada " + nome.trim() + ".");
        }
    }

    private void garantirNomeLivreParaAtualizacao(Long id, String nome) {
        unidades.findByNomeIgnoreCase(nome.trim())
                .filter(encontrada -> !encontrada.getId().equals(id))
                .ifPresent(encontrada -> {
                    throw new ConflitoException("Ja existe uma unidade chamada " + nome.trim() + ".");
                });
    }

    private Endereco criarEndereco(UnidadeRequisicao entrada) {
        return new Endereco(entrada.logradouro(), entrada.numero(), entrada.bairro(),
                entrada.cidade(), entrada.uf(), entrada.cep());
    }

    private CoordenadaGeo criarCoordenada(UnidadeRequisicao entrada) {
        return new CoordenadaGeo(entrada.latitude(), entrada.longitude());
    }

    private List<UnidadeResposta> converter(List<Unidade> lista) {
        return lista.stream().map(UnidadeService::paraResposta).toList();
    }

    public static UnidadeResposta paraResposta(Unidade unidade) {
        return new UnidadeResposta(
                unidade.getId(),
                unidade.tipo(),
                unidade.getNome(),
                unidade.getEndereco().getCidade(),
                unidade.getEndereco().getUf(),
                unidade.getLocalizacao().getLatitude(),
                unidade.getLocalizacao().getLongitude());
    }
}
