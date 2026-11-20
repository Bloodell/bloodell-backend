package br.edu.cesar.bloodell.geracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.doacao.aplicacao.DoacaoService;
import br.edu.cesar.bloodell.doacao.apresentacao.DoacaoDtos.DoacaoRequisicao;
import br.edu.cesar.bloodell.requisicao.aplicacao.RequisicaoService;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.ItemRequisicaoEntrada;
import br.edu.cesar.bloodell.requisicao.apresentacao.RequisicaoDtos.RequisicaoEntrada;
import br.edu.cesar.bloodell.requisicao.dominio.Prioridade;
import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import br.edu.cesar.bloodell.unidade.aplicacao.UnidadeService;
import br.edu.cesar.bloodell.unidade.apresentacao.UnidadeDtos.UnidadeRequisicao;
import br.edu.cesar.bloodell.unidade.apresentacao.UnidadeDtos.UnidadeResposta;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ColisaoDeCodigosIT extends TesteDeIntegracao {

    private static final int CRIACOES_PARALELAS = 16;

    @Autowired
    private UnidadeService unidades;

    @Autowired
    private DoacaoService doacoes;

    @Autowired
    private RequisicaoService requisicoes;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararBanco() {
        limparBanco(jdbc);
    }

    @Test
    @DisplayName("B-03: criacoes simultaneas de doacoes colidem no codigo DOA gerado por count")
    void criacoesParalelasDeDoacoesConfirmamColisao() throws Exception {
        UnidadeResposta hemocentro = unidades.cadastrarHemocentro(unidade("Hemocentro B-03"));

        Resultado resultado = executarEmParalelo(() -> doacoes.registrar(new DoacaoRequisicao(
                hemocentro.id(), "O+", 450, null, List.of(TipoComponente.PLASMA))));

        assertColisaoConfirmada("doacoes", "codigo", resultado);
    }

    @Test
    @DisplayName("B-03: criacoes simultaneas de requisicoes colidem no codigo REQ gerado por count")
    void criacoesParalelasDeRequisicoesConfirmamColisao() throws Exception {
        UnidadeResposta hospital = unidades.cadastrarHospital(unidade("Hospital B-03"));

        Resultado resultado = executarEmParalelo(() -> requisicoes.criar(new RequisicaoEntrada(
                hospital.id(), Prioridade.URGENTE,
                List.of(new ItemRequisicaoEntrada(TipoComponente.PLASMA, "O+", 1)))));

        assertColisaoConfirmada("requisicoes", "codigo", resultado);
    }

    private Resultado executarEmParalelo(OperacaoDeCriacao criacao) throws Exception {
        AtomicInteger criadas = new AtomicInteger();
        AtomicInteger rejeitadas = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch chegada = new CountDownLatch(CRIACOES_PARALELAS);
        ExecutorService executor = Executors.newFixedThreadPool(CRIACOES_PARALELAS);

        for (int i = 0; i < CRIACOES_PARALELAS; i++) {
            executor.submit(() -> {
                try {
                    largada.await();
                    criacao.executar();
                    criadas.incrementAndGet();
                } catch (InterruptedException interrupcao) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException colisao) {
                    rejeitadas.incrementAndGet();
                } finally {
                    chegada.countDown();
                }
            });
        }

        largada.countDown();
        assertThat(chegada.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        return new Resultado(criadas.get(), rejeitadas.get());
    }

    private void assertColisaoConfirmada(String tabela, String colunaCodigo, Resultado resultado) {
        Integer persistidas = jdbc.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);
        Integer codigosDistintos = jdbc.queryForObject(
                "SELECT count(DISTINCT " + colunaCodigo + ") FROM " + tabela, Integer.class);

        System.out.printf("[B-03] %s: tentativas=%d, criadas=%d, rejeitadas=%d, persistidas=%d, codigos-distintos=%d%n",
                tabela, CRIACOES_PARALELAS, resultado.criadas(), resultado.rejeitadas(),
                persistidas, codigosDistintos);

        assertThat(resultado.rejeitadas())
                .as("o gerador count()+1 nao e atomico sob criacoes simultaneas")
                .isPositive();
        assertThat(persistidas).isEqualTo(resultado.criadas());
        assertThat(codigosDistintos)
                .as("a restricao UNIQUE impede persistir identificadores duplicados")
                .isEqualTo(persistidas);
    }

    private UnidadeRequisicao unidade(String nome) {
        return new UnidadeRequisicao(nome, "Rua", "1", "Centro", "Recife", "PE", "50000-000",
                -8.05, -34.88);
    }

    @FunctionalInterface
    private interface OperacaoDeCriacao {
        void executar();
    }

    private record Resultado(int criadas, int rejeitadas) {
    }
}
