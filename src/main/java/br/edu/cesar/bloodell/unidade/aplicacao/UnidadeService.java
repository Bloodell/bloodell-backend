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
import java.util.ArrayList;
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
        Hospital hospital = new Hospital(entrada.nome(), montarEndereco(entrada), montarCoordenada(entrada));
        return paraResposta(unidades.save(hospital));
    }

    @Transactional
    public UnidadeResposta cadastrarHemocentro(UnidadeRequisicao entrada) {
        garantirNomeLivre(entrada.nome());
        Hemocentro hemocentro = new Hemocentro(entrada.nome(), montarEndereco(entrada), montarCoordenada(entrada));
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
    public UnidadeResposta buscar(Long id) {
        return paraResposta(buscarEntidade(id));
    }

    @Transactional
    public UnidadeResposta atualizar(Long id, UnidadeRequisicao entrada) {
        Unidade unidade = buscarEntidade(id);
        unidades.findByNomeIgnoreCase(entrada.nome().trim())
                .filter(encontrada -> !encontrada.getId().equals(id))
                .ifPresent(encontrada -> { throw new ConflitoException(
                        "Ja existe uma unidade chamada " + entrada.nome().trim() + "."); });
        unidade.atualizar(entrada.nome(), montarEndereco(entrada), montarCoordenada(entrada));
        return paraResposta(unidade);
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

    private void garantirNomeLivre(String nome) {
        if (unidades.findByNomeIgnoreCase(nome.trim()).isPresent()) {
            throw new ConflitoException("Ja existe uma unidade chamada " + nome.trim() + ".");
        }
    }

    private Endereco montarEndereco(UnidadeRequisicao entrada) {
        return new Endereco(entrada.logradouro(), entrada.numero(), entrada.bairro(),
                entrada.cidade(), entrada.uf(), entrada.cep());
    }

    private CoordenadaGeo montarCoordenada(UnidadeRequisicao entrada) {
        return new CoordenadaGeo(entrada.latitude(), entrada.longitude());
    }

    private List<UnidadeResposta> converter(List<Unidade> lista) {
        List<UnidadeResposta> respostas = new ArrayList<>(lista.size());
        for (Unidade unidade : lista) {
            respostas.add(paraResposta(unidade));
        }
        return respostas;
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
