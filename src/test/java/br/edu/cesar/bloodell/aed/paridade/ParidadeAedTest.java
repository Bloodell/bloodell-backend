package br.edu.cesar.bloodell.aed.paridade;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;
import br.edu.cesar.bloodell.aed.contratos.EventoAed;
import br.edu.cesar.bloodell.aed.contratos.RequisicaoAed;
import br.edu.cesar.bloodell.aed.u1.FilaRequisicoes;
import br.edu.cesar.bloodell.aed.u1.ListaEstoque;
import br.edu.cesar.bloodell.aed.u1.PilhaHistorico;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ParidadeAedTest {

    private static final Path FIXTURES = Path.of("tests", "parity", "fixtures");
    private static final Path SAIDA = Path.of("target", "paridade");
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String ETAPA = System.getProperty("bloodell.etapa.aed", "U1");

    static Stream<Path> fixtures() throws IOException {
        try (var arquivos = Files.list(FIXTURES)) {
            return arquivos.filter(caminho -> caminho.toString().endsWith(".json")).sorted().toList().stream();
        }
    }

    @ParameterizedTest(name = "paridade: {0}")
    @MethodSource("fixtures")
    @DisplayName("executa a fixture nas estruturas Java e grava a saida para comparacao com o C++")
    void executarFixture(Path arquivo) throws IOException {
        JsonNode fixture = JSON.readTree(Files.readString(arquivo, StandardCharsets.UTF_8));
        String nome = fixture.get("nome").asText();
        String etapaDaFixture = fixture.path("etapa").asText("U1");

        if (ordemDaEtapa(etapaDaFixture) > ordemDaEtapa(ETAPA)) {
            return;
        }

        List<String> comandos = new ArrayList<>();
        fixture.get("comandos").forEach(no -> comandos.add(no.asText()));

        List<String> saida = executar(comandos);

        assertThat(saida)
                .as("a fixture %s nao produziu saida nenhuma", nome)
                .isNotEmpty();

        Files.createDirectories(SAIDA);
        Files.writeString(SAIDA.resolve(nome + ".java.out"),
                String.join("\n", saida) + "\n", StandardCharsets.UTF_8);
    }

    private List<String> executar(List<String> comandos) {
        ListaEstoque lista = new ListaEstoque();
        FilaRequisicoes fila = new FilaRequisicoes();
        PilhaHistorico pilha = new PilhaHistorico();
        List<String> saida = new ArrayList<>();

        for (String linha : comandos) {
            String[] partes = linha.trim().split("\\s+");
            String comando = partes[0];

            switch (comando) {
                case "ESTRUTURA" -> saida.add("ESTRUTURA " + partes[1]);

                case "INSERIR" -> {
                    lista.inserir(new BolsaAed(partes[1], partes[2], partes[3], partes[4],
                            Long.parseLong(partes[5]), partes[6]));
                    saida.add("OK " + lista.tamanho());
                }
                case "REMOVER" -> {
                    BolsaAed removida = lista.removerPorCodigo(partes[1]);
                    saida.add(removida == null ? "NULO" : "REMOVIDO " + removida.getCodigo());
                }
                case "BUSCAR" -> {
                    BolsaAed encontrada = lista.buscarPorCodigo(partes[1]);
                    saida.add(encontrada == null ? "NULO" : "ENCONTRADO " + encontrada.getCodigo());
                }
                case "PRIMEIRA" -> {
                    BolsaAed escolhida =
                            lista.primeiraCompativelPorIgualdade(partes[1], partes[2], partes[3]);
                    saida.add(escolhida == null ? "NULO"
                            : "ESCOLHIDA " + escolhida.getCodigo() + " " + escolhida.getSequenciaEntrada());
                }

                case "ENFILEIRAR" -> {
                    fila.enfileirar(new RequisicaoAed(partes[1], partes[2], partes[3],
                            Long.parseLong(partes[4])));
                    saida.add("OK " + fila.tamanho());
                }
                case "DESENFILEIRAR" -> {
                    RequisicaoAed retirada = fila.desenfileirar();
                    saida.add(retirada == null ? "NULO"
                            : "SAIU " + retirada.getCodigo() + " " + retirada.getPrioridade());
                }
                case "ESPIAR" -> {
                    RequisicaoAed proxima = fila.espiar();
                    saida.add(proxima == null ? "NULO" : "PROXIMA " + proxima.getCodigo());
                }

                case "EMPILHAR" -> {
                    pilha.empilhar(new EventoAed(partes[1], partes[2], partes[3], partes[4]));
                    saida.add("OK " + pilha.tamanho());
                }
                case "DESEMPILHAR" -> {
                    EventoAed retirado = pilha.desempilhar();
                    saida.add(retirado == null ? "NULO"
                            : "SAIU " + retirado.getCodigoBolsa() + " " + retirado.getStatusNovo());
                }
                case "TOPO" -> {
                    EventoAed doTopo = pilha.topo();
                    saida.add(doTopo == null ? "NULO"
                            : "TOPO " + doTopo.getCodigoBolsa() + " " + doTopo.getStatusNovo());
                }

                case "LISTAR" -> {
                    switch (partes[1]) {
                        case "LISTA" -> {
                            BolsaAed[] itens = lista.paraVetor();
                            saida.add("LISTAGEM " + itens.length);
                            for (int indice = 0; indice < itens.length; indice++) {
                                saida.add((indice + 1) + " " + itens[indice].getCodigo());
                            }
                        }
                        case "FILA" -> {
                            RequisicaoAed[] itens = fila.paraVetor();
                            saida.add("LISTAGEM " + itens.length);
                            for (int indice = 0; indice < itens.length; indice++) {
                                saida.add((indice + 1) + " " + itens[indice].getCodigo());
                            }
                        }
                        default -> {
                            EventoAed[] itens = pilha.paraVetor();
                            saida.add("LISTAGEM " + itens.length);
                            for (int indice = 0; indice < itens.length; indice++) {
                                saida.add((indice + 1) + " " + itens[indice].getCodigoBolsa());
                            }
                        }
                    }
                }

                case "TAMANHO" -> {
                    int tamanho = switch (partes[1]) {
                        case "LISTA" -> lista.tamanho();
                        case "FILA" -> fila.tamanho();
                        default -> pilha.tamanho();
                    };
                    saida.add("TAMANHO " + tamanho);
                }

                default -> saida.add("COMANDO_DESCONHECIDO " + comando);
            }
        }
        return saida;
    }

    private static int ordemDaEtapa(String etapa) {
        return "U2".equals(etapa) ? 2 : 1;
    }
}
