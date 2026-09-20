package br.edu.cesar.bloodell.alocacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;
import br.edu.cesar.bloodell.aed.u1.ListaEstoque;
import br.edu.cesar.bloodell.alocacao.dominio.AlocacaoProvisoriaU1;
import br.edu.cesar.bloodell.alocacao.dominio.DiarioDaDecisao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlocacaoProvisoriaU1Test {

    private final AlocacaoProvisoriaU1 politica = new AlocacaoProvisoriaU1();

    @Test
    @DisplayName("escolhe a primeira que chegou, mesmo havendo outra que vence antes")
    void naoAplicaFefo() {
        ListaEstoque estoque = new ListaEstoque();
        estoque.inserir(new BolsaAed("BL-1", "A+", "CONCENTRADO_HEMACIAS", "2026-12-31", 1, "DISPONIVEL"));
        estoque.inserir(new BolsaAed("BL-2", "A+", "CONCENTRADO_HEMACIAS", "2026-09-22", 2, "DISPONIVEL"));

        BolsaAed escolhida = politica.escolher(estoque, "A+", "CONCENTRADO_HEMACIAS", new DiarioDaDecisao());

        assertThat(escolhida.getCodigo())
                .as("FEFO e conteudo da Unidade 2 e nao pode estar implementado na U1")
                .isEqualTo("BL-1");
    }

    @Test
    @DisplayName("nao usa compatibilidade ABO/Rh: exige tipo exatamente igual")
    void naoAplicaCompatibilidade() {
        ListaEstoque estoque = new ListaEstoque();
        estoque.inserir(new BolsaAed("BL-O", "O-", "CONCENTRADO_HEMACIAS", "2026-12-31", 1, "DISPONIVEL"));

        BolsaAed escolhida = politica.escolher(estoque, "A+", "CONCENTRADO_HEMACIAS", new DiarioDaDecisao());

        assertThat(escolhida)
                .as("a matriz ABO/Rh e da Unidade 2; aqui so vale igualdade")
                .isNull();
    }

    @Test
    @DisplayName("ignora bolsa reservada e componente diferente")
    void respeitaStatusEComponente() {
        ListaEstoque estoque = new ListaEstoque();
        estoque.inserir(new BolsaAed("BL-1", "A+", "CONCENTRADO_HEMACIAS", "2026-12-31", 1, "RESERVADA"));
        estoque.inserir(new BolsaAed("BL-2", "A+", "PLASMA", "2026-12-31", 2, "DISPONIVEL"));
        estoque.inserir(new BolsaAed("BL-3", "A+", "CONCENTRADO_HEMACIAS", "2026-12-31", 3, "DISPONIVEL"));

        BolsaAed escolhida = politica.escolher(estoque, "A+", "CONCENTRADO_HEMACIAS", new DiarioDaDecisao());

        assertThat(escolhida.getCodigo()).isEqualTo("BL-3");
    }

    @Test
    @DisplayName("os passos anotados descrevem o que realmente aconteceu")
    void diarioRegistraPassosReais() {
        ListaEstoque estoque = new ListaEstoque();
        estoque.inserir(new BolsaAed("BL-1", "A+", "CONCENTRADO_HEMACIAS", "2026-12-31", 1, "DISPONIVEL"));
        DiarioDaDecisao diario = new DiarioDaDecisao();

        politica.escolher(estoque, "A+", "CONCENTRADO_HEMACIAS", diario);

        assertThat(diario.getPassos()).isNotEmpty();
        assertThat(diario.getPassos())
                .extracting("etapa")
                .contains("estoque", "filtro", "escolha", "limite-academico");
        assertThat(diario.getPassos().get(0).ordem()).isEqualTo(1);
    }

    @Test
    @DisplayName("estoque vazio devolve nulo e registra o motivo")
    void estoqueVazio() {
        DiarioDaDecisao diario = new DiarioDaDecisao();

        BolsaAed escolhida = politica.escolher(new ListaEstoque(), "A+", "CONCENTRADO_HEMACIAS", diario);

        assertThat(escolhida).isNull();
        assertThat(diario.getPassos()).extracting("etapa").contains("resultado");
    }

    @Test
    @DisplayName("o aviso deixa claro que a politica e provisoria")
    void avisoEhExplicito() {
        assertThat(politica.nome()).isEqualTo("PROVISORIA_U1");
        assertThat(politica.aviso()).contains("PROVISORIA", "Sem matriz ABO/Rh, sem FEFO");
    }
}
