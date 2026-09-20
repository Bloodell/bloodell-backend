package br.edu.cesar.bloodell.doacao.aplicacao;

import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
import br.edu.cesar.bloodell.bolsa.dominio.FabricaDeBolsas;
import br.edu.cesar.bloodell.bolsa.infraestrutura.BolsaRepository;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoSanguineo;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.BolsaGeradaResposta;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoRequisicao;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoResposta;
import br.edu.cesar.bloodell.doacao.dominio.Doacao;
import br.edu.cesar.bloodell.doacao.infraestrutura.DoacaoRepository;
import br.edu.cesar.bloodell.estoque.aplicacao.SincronizadorDeEstoque;
import br.edu.cesar.bloodell.rastreabilidade.aplicacao.RegistradorDeEventos;
import br.edu.cesar.bloodell.unidade.aplicacao.UnidadeService;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(DoacaoService.class);

    private final DoacaoRepository doacoes;
    private final BolsaRepository bolsas;
    private final UnidadeService unidadeService;
    private final SincronizadorDeEstoque sincronizador;
    private final RegistradorDeEventos registrador;

    public DoacaoService(DoacaoRepository doacoes,
                         BolsaRepository bolsas,
                         UnidadeService unidadeService,
                         SincronizadorDeEstoque sincronizador,
                         RegistradorDeEventos registrador) {
        this.doacoes = doacoes;
        this.bolsas = bolsas;
        this.unidadeService = unidadeService;
        this.sincronizador = sincronizador;
        this.registrador = registrador;
    }

    @Transactional
    public DoacaoResposta registrar(DoacaoRequisicao entrada) {
        Unidade hemocentro = unidadeService.buscarEntidade(entrada.hemocentroId());
        if (!"HEMOCENTRO".equals(hemocentro.tipo())) {
            throw new RegraDeNegocioException("Doacao so pode ser registrada em um hemocentro. A unidade "
                    + hemocentro.getNome() + " e um " + hemocentro.tipo().toLowerCase() + ".");
        }

        OffsetDateTime quando = entrada.dataHora() == null ? OffsetDateTime.now() : entrada.dataHora();
        if (quando.isAfter(OffsetDateTime.now())) {
            throw new RegraDeNegocioException("Data da doacao no futuro nao e aceita.");
        }

        TipoSanguineo tipo = TipoSanguineo.doTextoCompacto(entrada.tipoSanguineo());
        Doacao doacao = doacoes.save(new Doacao(gerarCodigoDoacao(), hemocentro, quando, tipo, entrada.volumeMl()));

        List<BolsaGeradaResposta> geradas = new ArrayList<>();
        for (TipoComponente componente : entrada.componentes()) {
            long sequencia = bolsas.proximaSequenciaDeEntrada();
            Bolsa bolsa = bolsas.save(FabricaDeBolsas.aPartirDaDoacao(doacao, componente, sequencia));

            registrador.registrar(bolsa, null, EstadoBolsa.RECEBIDA,
                    null, "Bolsa gerada a partir da doacao " + doacao.getCodigo());

            EstadoBolsa anterior = bolsa.getStatus();
            bolsa.armazenar();
            registrador.registrar(bolsa, anterior, bolsa.getStatus(), null, "Armazenada no estoque");
            sincronizador.registrarEntrada(bolsa);

            geradas.add(new BolsaGeradaResposta(bolsa.getId(), bolsa.getCodigo(),
                    bolsa.getComponente().name(), bolsa.getDataValidade().toString(),
                    bolsa.getStatus().name()));
        }

        LOG.info("Doacao {} registrada no hemocentro {} com {} bolsa(s) gerada(s).",
                doacao.getCodigo(), hemocentro.getNome(), geradas.size());

        return new DoacaoResposta(doacao.getId(), doacao.getCodigo(), hemocentro.getId(),
                tipo.formatado(), doacao.getVolumeMl(), doacao.getDataHora(), geradas);
    }

    private String gerarCodigoDoacao() {
        long proximo = doacoes.count() + 1;
        String codigo = String.format("DOA-%06d", proximo);
        while (doacoes.existsByCodigo(codigo)) {
            proximo++;
            codigo = String.format("DOA-%06d", proximo);
        }
        return codigo;
    }
}
