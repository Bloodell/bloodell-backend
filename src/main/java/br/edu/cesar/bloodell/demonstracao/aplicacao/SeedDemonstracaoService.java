package br.edu.cesar.bloodell.demonstracao.aplicacao;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.compartilhado.excecao.ConflitoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import br.edu.cesar.bloodell.demonstracao.apresentacao.DemonstracaoDtos.CenarioEntrada;
import br.edu.cesar.bloodell.demonstracao.apresentacao.DemonstracaoDtos.CenarioResposta;
import br.edu.cesar.bloodell.doacao.aplicacao.DoacaoService;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoRequisicao;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoResposta;
import br.edu.cesar.bloodell.requisicao.aplicacao.RequisicaoService;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.ItemRequisicaoEntrada;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.RequisicaoEntrada;
import br.edu.cesar.bloodell.requisicao.dominio.Prioridade;
import br.edu.cesar.bloodell.unidade.dominio.Hemocentro;
import br.edu.cesar.bloodell.unidade.dominio.Hospital;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.unidade.infraestrutura.UnidadeRepository;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.veiculo.infraestrutura.VeiculoRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedDemonstracaoService {

    private static final Logger LOG = LoggerFactory.getLogger(SeedDemonstracaoService.class);

    public static final long SEMENTE_PADRAO = 42L;

    private static final String AVISO =
            "Cenario 100% sintetico para fins academicos. Nenhum dado real de doador ou paciente. "
            + "Regras de compatibilidade sao didaticas e nao constituem orientacao clinica.";

    private static final String[] TIPOS_SORTEAVEIS = {
        "O+", "O+", "O+", "O+", "O+", "O+", "O+",
        "A+", "A+", "A+", "A+", "A+", "A+", "A+",
        "B+", "B+",
        "AB+",
        "O-", "A-", "B-", "AB-"
    };

    private final UnidadeRepository unidades;
    private final VeiculoRepository veiculos;
    private final DoacaoService doacaoService;
    private final RequisicaoService requisicaoService;
    private final PropriedadesBloodell propriedades;

    public SeedDemonstracaoService(UnidadeRepository unidades,
                                   VeiculoRepository veiculos,
                                   DoacaoService doacaoService,
                                   RequisicaoService requisicaoService,
                                   PropriedadesBloodell propriedades) {
        this.unidades = unidades;
        this.veiculos = veiculos;
        this.doacaoService = doacaoService;
        this.requisicaoService = requisicaoService;
        this.propriedades = propriedades;
    }

    @Transactional
    public CenarioResposta popular(CenarioEntrada entrada) {
        if (!propriedades.getDemonstracao().isHabilitado()) {
            throw new RegraDeNegocioException("O cenario de demonstracao esta desligado neste ambiente "
                    + "(bloodell.demonstracao.habilitado=false).");
        }
        if (!unidades.findAll().isEmpty()) {
            throw new ConflitoException("O banco ja possui unidades cadastradas. "
                    + "Para recriar o cenario, limpe o banco (docker compose down -v) e repita a chamada.");
        }

        long semente = entrada.semente() == null ? SEMENTE_PADRAO : entrada.semente();
        Random sorteio = new Random(semente);

        List<Unidade> hemocentros = criarHemocentros();
        List<Unidade> hospitais = criarHospitais();
        int veiculosCriados = criarVeiculos(hemocentros.get(0), sorteio);

        int doacoes = 0;
        int bolsas = 0;
        OffsetDateTime agora = OffsetDateTime.now();

        for (int indice = 0; indice < 24; indice++) {
            Unidade hemocentro = hemocentros.get(sorteio.nextInt(hemocentros.size()));
            String tipo = TIPOS_SORTEAVEIS[sorteio.nextInt(TIPOS_SORTEAVEIS.length)];
            int volume = 420 + sorteio.nextInt(60);
            OffsetDateTime quando = agora.minusDays(sorteio.nextInt(20)).minusHours(sorteio.nextInt(12));

            List<TipoComponente> componentes = sortearComponentes(sorteio);
            DoacaoResposta registrada = doacaoService.registrar(new DoacaoRequisicao(
                    hemocentro.getId(), tipo, volume, quando, componentes));
            doacoes++;
            bolsas += registrada.bolsasGeradas().size();
        }

        int requisicoes = criarRequisicoes(hospitais, sorteio);

        LOG.info("Cenario '{}' criado com semente {}: {} doacao(oes), {} bolsa(s), {} requisicao(oes).",
                entrada.cenario(), semente, doacoes, bolsas, requisicoes);

        return new CenarioResposta(entrada.cenario(), semente, AVISO,
                hemocentros.size(), hospitais.size(), veiculosCriados, doacoes, bolsas, requisicoes,
                List.of(
                        "GET /api/v1/estoques -- posicao de estoque",
                        "GET /api/v1/requisicoes/fila -- fila montada pela estrutura da AED-U1",
                        "GET /api/v1/algoritmos/estoque -- retrato da lista encadeada",
                        "POST /api/v1/requisicoes/{id}/alocacoes -- alocacao provisoria da U1",
                        "GET /api/v1/indicadores/estoque -- painel descritivo"));
    }

    private List<Unidade> criarHemocentros() {
        List<Unidade> criados = new ArrayList<>();
        criados.add(unidades.save(new Hemocentro("Hemocentro Central Recife",
                new Endereco("Rua Sintetica", "100", "Boa Vista", "Recife", "PE", "50000-000"),
                new CoordenadaGeo(-8.0578, -34.8829))));
        criados.add(unidades.save(new Hemocentro("Hemocentro Regional Jaboatao",
                new Endereco("Avenida Sintetica", "250", "Centro", "Jaboatao dos Guararapes", "PE", "54000-000"),
                new CoordenadaGeo(-8.1128, -35.0150))));
        return criados;
    }

    private List<Unidade> criarHospitais() {
        List<Unidade> criados = new ArrayList<>();
        criados.add(unidades.save(new Hospital("Hospital Modelo Norte",
                new Endereco("Rua Ficticia", "10", "Casa Amarela", "Recife", "PE", "52070-000"),
                new CoordenadaGeo(-8.0270, -34.9130))));
        criados.add(unidades.save(new Hospital("Hospital Modelo Sul",
                new Endereco("Rua Ficticia", "20", "Boa Viagem", "Recife", "PE", "51020-000"),
                new CoordenadaGeo(-8.1210, -34.9020))));
        criados.add(unidades.save(new Hospital("Hospital Modelo Oeste",
                new Endereco("Rua Ficticia", "30", "Varzea", "Recife", "PE", "50740-000"),
                new CoordenadaGeo(-8.0480, -34.9560))));
        return criados;
    }

    private int criarVeiculos(Unidade base, Random sorteio) {
        String[] placas = {"BLD-1A11", "BLD-2B22", "BLD-3C33"};
        for (String placa : placas) {
            double deslocamentoLat = (sorteio.nextDouble() - 0.5) / 100.0;
            double deslocamentoLon = (sorteio.nextDouble() - 0.5) / 100.0;
            veiculos.save(new Veiculo(placa, base, 20, new CoordenadaGeo(
                    base.getLocalizacao().getLatitude() + deslocamentoLat,
                    base.getLocalizacao().getLongitude() + deslocamentoLon)));
        }
        return placas.length;
    }

    private List<TipoComponente> sortearComponentes(Random sorteio) {
        List<TipoComponente> componentes = new ArrayList<>();
        componentes.add(TipoComponente.CONCENTRADO_HEMACIAS);
        if (sorteio.nextInt(10) < 6) {
            componentes.add(TipoComponente.PLASMA);
        }
        if (sorteio.nextInt(10) < 4) {
            componentes.add(TipoComponente.PLAQUETAS);
        }
        return componentes;
    }

    private int criarRequisicoes(List<Unidade> hospitais, Random sorteio) {
        Prioridade[] prioridades = Prioridade.values();
        int criadas = 0;
        for (int indice = 0; indice < 7; indice++) {
            Unidade hospital = hospitais.get(sorteio.nextInt(hospitais.size()));
            Prioridade prioridade = prioridades[sorteio.nextInt(prioridades.length)];
            String tipo = TIPOS_SORTEAVEIS[sorteio.nextInt(TIPOS_SORTEAVEIS.length)];
            int quantidade = 1 + sorteio.nextInt(2);

            requisicaoService.criar(new RequisicaoEntrada(hospital.getId(), prioridade,
                    List.of(new ItemRequisicaoEntrada(TipoComponente.CONCENTRADO_HEMACIAS, tipo, quantidade))));
            criadas++;
        }
        return criadas;
    }
}
