package br.edu.cesar.bloodell.alocacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.alocacao.aplicacao.AlocacaoService;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConcorrenciaNaAlocacaoIT extends TesteDeIntegracao {

    @Autowired
    private UnidadeService unidadeService;

    @Autowired
    private DoacaoService doacaoService;

    @Autowired
    private RequisicaoService requisicaoService;

    @Autowired
    private AlocacaoService alocacaoService;

    @Test
    @DisplayName("duas alocacoes simultaneas da mesma bolsa: uma vence, a outra falha")
    void apenasUmaAlocacaoVence() throws Exception {
        UnidadeResposta hemocentro = unidadeService.cadastrarHemocentro(new UnidadeRequisicao(
                "Hemocentro Concorrencia " + System.nanoTime(), "Rua", "1", "Centro",
                "Recife", "PE", "50000-000", -8.05, -34.88));
        UnidadeResposta hospital = unidadeService.cadastrarHospital(new UnidadeRequisicao(
                "Hospital Concorrencia " + System.nanoTime(), "Rua", "2", "Centro",
                "Recife", "PE", "50000-000", -8.06, -34.89));

        doacaoService.registrar(new DoacaoRequisicao(hemocentro.id(), "AB-", 450, null,
                List.of(TipoComponente.CONCENTRADO_HEMACIAS)));

        Long primeira = requisicaoService.criar(new RequisicaoEntrada(hospital.id(), Prioridade.URGENTE,
                List.of(new ItemRequisicaoEntrada(TipoComponente.CONCENTRADO_HEMACIAS, "AB-", 1)))).id();
        Long segunda = requisicaoService.criar(new RequisicaoEntrada(hospital.id(), Prioridade.URGENTE,
                List.of(new ItemRequisicaoEntrada(TipoComponente.CONCENTRADO_HEMACIAS, "AB-", 1)))).id();

        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhas = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch chegada = new CountDownLatch(2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        for (Long id : List.of(primeira, segunda)) {
            executor.submit(() -> {
                try {
                    largada.await();
                    alocacaoService.processar(id);
                    sucessos.incrementAndGet();
                } catch (InterruptedException interrupcao) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException esperado) {
                    falhas.incrementAndGet();
                } finally {
                    chegada.countDown();
                }
            });
        }

        largada.countDown();
        assertThat(chegada.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(sucessos.get())
                .as("a mesma bolsa nao pode ser alocada duas vezes")
                .isEqualTo(1);
        assertThat(falhas.get())
                .as("a segunda tentativa precisa falhar de forma controlada")
                .isEqualTo(1);
    }
}
