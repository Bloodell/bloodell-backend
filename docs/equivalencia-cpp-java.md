# Equivalência C++ ↔ Java das estruturas da AED

O mesmo algoritmo existe duas vezes: em C++ (`academic/cpp`), como nas aulas, e
em Java (`src/main/java/.../aed/u1`), dentro da aplicação. Este documento mostra
que as duas versões são a mesma coisa escrita em duas linguagens — e o CI
comprova isso a cada push.

## Tabela de tradução

| Conceito | C++ | Java | Observação |
|---|---|---|---|
| Ponteiro para nó | `NoBolsa* proximo;` | `NoBolsa proximo;` | referência Java faz o papel do ponteiro |
| Ausência de nó | `nullptr` | `null` | mesma verificação, `== nullptr` / `== null` |
| Alocar | `new NoBolsa(valor)` | `new NoBolsa(valor)` | em C, seria `malloc(sizeof(NoBolsa))` |
| Liberar | `delete no;` | nada — o coletor recolhe | em Java, basta soltar a referência (`no.setProximo(null)`) |
| Acessar campo do nó | `no->dado.codigo` | `no.getDado().getCodigo()` | encapsulamento no lado Java |
| Estrutura de dados simples | `struct BolsaAed` | `class BolsaAed` | mesmos nomes de campos, mesma ordem |
| Devolver "não achei" | `bool` + parâmetro de saída | devolve `null` | C++ não tem `Optional` sem biblioteca |
| Coleção de tamanho fixo | vetor C (`NoRequisicao* cabecas[3]`) | vetor Java (`NoRequisicao[] cabecas`) | nada de `std::vector` nem `ArrayList` |

## O que está proibido nos dois lados

Dentro das estruturas avaliadas, **nenhuma coleção pronta**: nem `std::vector`,
`std::list`, `std::map`, `std::stack`, `std::queue`, nem `ArrayList`,
`LinkedList`, `ArrayDeque`, `HashMap`, `Arrays.sort` ou `Collections`. Nós,
ponteiros e vetores manuais dos dois lados. Esconder a estrutura atrás de uma
coleção da linguagem invalida a demonstração.

Fora do módulo (DTOs, controllers, telas, ingestão de telemetria), coleções são
permitidas normalmente.

## Correspondência de arquivos

| Estrutura | C++ | Java |
|---|---|---|
| Contratos | `include/contratos.hpp` | `aed/contratos/*.java` |
| Lista | `include/lista_estoque.hpp` + `src/lista_estoque.cpp` | `aed/u1/ListaEstoque.java`, `NoBolsa.java` |
| Fila | `include/fila_requisicoes.hpp` + `src/fila_requisicoes.cpp` | `aed/u1/FilaRequisicoes.java`, `NoRequisicao.java` |
| Pilha | `include/pilha_historico.hpp` + `src/pilha_historico.cpp` | `aed/u1/PilhaHistorico.java`, `NoEvento.java` |
| Testes | `tests/testes.cpp` | `aed/u1/*Test.java` (mesmos nomes de cenário) |
| Runner de paridade | `src/runner.cpp` | `aed/paridade/ParidadeAedTest.java` |

## Como a paridade é verificada automaticamente

1. As fixtures em `tests/parity/fixtures/*.json` guardam uma sequência de
   comandos em texto simples — as duas implementações leem a mesma sequência.
2. `mvnw verify` roda `ParidadeAedTest`, que executa os comandos nas estruturas
   Java e grava `target/paridade/<nome>.java.out`.
3. Na fase seguinte, `scripts/paridade.py` compila o runner C++, roda a mesma
   fixture nele, grava `<nome>.cpp.out` e **compara linha a linha**.
4. Qualquer divergência derruba o build, mostrando a linha e os dois valores.

O texto simples é proposital: assim o lado C++ não precisa de um parser de JSON,
e nenhuma biblioteca externa chega perto das estruturas avaliadas.

## Cenários cobertos hoje

| Fixture | O que prova |
|---|---|
| `lista-ordem-de-chegada` | inserção, busca, remoção de cabeça/meio/fim, remoção de código inexistente e escolha pela ordem de chegada |
| `lista-mil-bolsas` | mesma ordem preservada em volume (1000 elementos) |
| `fila-prioridade-e-chegada` | prioridade e, dentro dela, ordem de chegada |
| `pilha-historico-lifo` | LIFO em 10 eventos e comportamento da pilha vazia |

Em `lista-ordem-de-chegada`, a bolsa `BL-9` vence antes de `BL-1` **de
propósito**: se alguma das implementações escolher `BL-9`, é sinal de que
alguém antecipou FEFO na Unidade 1, e o build falha.

## Quando a Unidade 2 chegar

Cada algoritmo novo entra com fixture própria marcada `"etapa": "U2"`. Elas já
são ignoradas hoje pelos dois runners. A virada acontece trocando
`bloodell.etapa.aed` para `U2` no `pom.xml`, em um pull request ligado ao marco
acadêmico — nunca por uma flag de demonstração.
