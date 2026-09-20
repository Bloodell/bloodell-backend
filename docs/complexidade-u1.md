# Complexidade das estruturas — Unidade 1

Os custos abaixo foram lidos **da implementação real** (`aed/u1` em Java e
`academic/cpp` em C++), não de tabela de livro. Onde Java e C++ diferem, está
anotado — e eles não diferem em nenhum caso, porque o algoritmo é o mesmo.

`n` = número de elementos na estrutura.

## ListaEstoque — lista simplesmente encadeada

| Operação | Tempo | Por que esse custo |
|---|---|---|
| `inserir` | O(1) | insere no fim usando a referência de cauda; não percorre |
| `buscarPorCodigo` | O(n) | percorre nó a nó até achar; pior caso, a lista inteira |
| `removerPorCodigo` | O(n) | mesma varredura, mais o religamento do nó anterior em O(1) |
| `primeiraCompativelPorIgualdade` | O(n) | para no primeiro que atende; pior caso, percorre tudo |
| `tamanho` | O(1) | contador mantido nas operações de escrita |
| `paraVetor` | O(n) | copia os n elementos |

Espaço: O(n) nós. Em C++, cada nó é um `new`/`delete` explícito; em Java, o
coletor de lixo recolhe o nó quando ele deixa de ser referenciado.

**Por que não é O(1) na busca:** não há índice. Esse é justamente o problema que
a tabela hash da Unidade 2 vai resolver — e é por isso que ela existe no
cronograma.

## FilaRequisicoes — três filas FIFO encadeadas, uma por prioridade

| Operação | Tempo | Por que esse custo |
|---|---|---|
| `enfileirar` | O(1) | vai direto para a cauda da fila daquela prioridade |
| `desenfileirar` | O(1) | no máximo 3 testes de "fila vazia" — constante, não depende de n |
| `espiar` | O(1) | idem |
| `tamanho` | O(1) | contador |
| `paraVetor` | O(n) | concatena as três filas na ordem de atendimento |

Espaço: O(n) nós + 6 ponteiros fixos (3 cabeças e 3 caudas).

**Comparação honesta com a alternativa.** A outra forma de fazer "FIFO com
urgência" é uma fila única com inserção ordenada por (prioridade, chegada). A
**ordem de saída é idêntica** — os testes de paridade provam isso. O que muda é
o custo: inserção ordenada é **O(n)**, porque percorre até achar o lugar. Ou
seja, as três filas entregam o mesmo resultado por um preço menor.

## PilhaHistorico — pilha encadeada

| Operação | Tempo | Por que esse custo |
|---|---|---|
| `empilhar` | O(1) | novo nó aponta para o antigo topo |
| `desempilhar` | O(1) | move o topo para o nó de baixo |
| `topo` | O(1) | lê o nó do topo |
| `tamanho` | O(1) | contador |
| `paraVetor` | O(n) | percorre do topo até a base |

Espaço: O(n) nós.

## Como esses números foram conferidos

1. leitura direta do código das duas implementações;
2. teste com mil elementos (`lista-mil-bolsas.json`), que passa nas duas e
   confirma que nada degrada além do previsto;
3. os testes em C++ rodam com `-fsanitize=address,undefined`, o que também
   garante que não há vazamento escondendo custo de memória.

## O que ainda não está aqui

Complexidade de FEFO, tabela hash, matriz ABO/Rh e vizinho mais próximo entra
neste documento **quando esses algoritmos forem implementados**, na Unidade 2.
Anotar Big-O de código que não existe seria inventar evidência.
