# Entrega AED Unidade 1 — Rota Vital

## Estruturas e justificativa

- `ListaEstoque`: lista simplesmente encadeada para manter bolsas na ordem de
  chegada e permitir busca/remoção sem antecipar o índice hash da Unidade 2.
- `FilaRequisicoes`: três filas FIFO encadeadas, uma por prioridade
  (`EMERGENCIA`, `URGENTE`, `ELETIVA`), preservando a ordem de chegada dentro de
  cada prioridade.
- `PilhaHistorico`: pilha encadeada para exibir os eventos mais recentes primeiro
  (LIFO). O banco continua sendo a fonte de verdade; a pilha é uma projeção em
  memória dos eventos persistidos.

As mesmas operações aparecem em `academic/c/` (C11, `struct`, ponteiros,
`malloc/free`) e em `src/main/java/.../aed/u1/` (nós e referências Java), sem
dependência de Spring, JPA ou repositórios no módulo AED.

## Testes

```bash
# Estruturas acadêmicas em C11; usa AddressSanitizer/UndefinedBehaviorSanitizer
# quando o compilador os suporta.
cd academic/c
make testar

# Testes Java, incluindo os cenários equivalentes das três estruturas.
cd ../..
./mvnw -B test

# Paridade C11 x Java (execute após os testes Java, que geram as saídas Java).
python3 scripts/paridade.py --etapa U1

# Fluxo completo: testes de integração também exigem Docker/Testcontainers.
./mvnw -B verify
```

## Integração e limite da U1

O fluxo de alocação hidrata e percorre `ListaEstoque`; requisições pendentes são
montadas em `FilaRequisicoes`; e o endpoint de visualização do histórico hidrata
`PilhaHistorico` a partir dos eventos persistidos. A regra de alocação da U1 é
igualdade exata de tipo/componente e ordem de chegada.

Este escopo não implementa nem antecipa FEFO, tabela hash, matriz de
compatibilidade ABO/Rh ou roteirização. Esses algoritmos pertencem à Unidade 2.
A documentação formal de equivalência será produzida em etapa posterior.
