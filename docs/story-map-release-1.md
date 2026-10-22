# Story map — Release 1

## Objetivo

Permitir que o operador registre doações, acompanhe as bolsas disponíveis,
registre demandas hospitalares e processe a alocação provisória da Unidade 1.

Os dados são sintéticos e as regras são didáticas.

## Backbone

As atividades principais do usuário, em ordem, são:

1. Registrar doações.
2. Consultar bolsas e estoque.
3. Registrar demandas hospitalares.
4. Consultar a fila de requisições.
5. Processar alocações e consultar seus resultados.

## Recorte da Release 1

| Atividade do backbone | História da Release 1 | Depende de |
|---|---|---|
| Registrar doações e consultar bolsas/estoque | US-R1-01 — Registrar doação e disponibilizar bolsas | — |
| Registrar demandas e consultar a fila | US-R1-02 — Criar e consultar requisições prioritárias | — |
| Processar alocações e consultar resultados | US-R1-03 — Processar alocação provisória U1 | US-R1-01, US-R1-02 |

## Como ler as tarefas

Cada história tem duas tarefas filhas, com o mesmo padrão de ID e de dono:

- **A — regra de domínio, com testes unitários.** Responsável: Abelardo.
- **B — API real e PostgreSQL, com testes de integração e de aceitação.** Responsável: Danilo.

Os critérios de aceite são numerados (`CA1`, `CA2`…) para que cada tarefa diga
quais ela cobre. As colunas de teste apontam a evidência: o nome do teste nos
relatórios do surefire e do failsafe, ou a tag do cenário Gherkin (`@R1-0xB`).
Testes que já existiam antes da E2-02 são baseline.

## US-R1-01 — Registrar doação e disponibilizar bolsas

Como operador do hemocentro, quero registrar uma doação e consultar
as bolsas geradas para acompanhar sua disponibilidade no estoque.

Critérios de aceite:

- **CA1** — Registrar uma doação individual pela API.
- **CA2** — Gerar bolsas e validade conforme as regras da U1.
- **CA3** — Consultar posteriormente as bolsas e o estoque persistidos no PostgreSQL.
- **CA4** — Verificar sucesso e rejeição de entradas inválidas.

Tarefas:

| ID | Tarefa | Cobre | Responsável | Depende de | Unitário | Integração (PostgreSQL) | Aceitação |
|---|---|---|---|---|---|---|---|
| R1-01A | Validar geração e validade das bolsas por regra | CA2, CA4 | Abelardo | — | `FabricaDeBolsasTest`, `BolsaTest` (`cicloInicial`, `validadeIncoerente`, `venceHojeAindaVale`) | — | — |
| R1-01B | Validar registro e consulta via API real e PostgreSQL | CA1, CA2, CA3, CA4 | Danilo | R1-01A | — | `CadastrosIT.consultaDeDoacoes` | Cenários `@R1-01B` |

Card: https://app.clickup.com/t/90132678197/86akumh51

## US-R1-02 — Criar e consultar requisições prioritárias

Como operador, quero registrar demandas hospitalares e consultar a fila
para visualizar a ordem de atendimento por prioridade e chegada.

Critérios de aceite:

- **CA1** — Criar e consultar requisições pela API com persistência no PostgreSQL.
- **CA2** — Ordenar por EMERGENCIA, URGENTE e ELETIVA.
- **CA3** — Preservar a ordem de chegada entre requisições da mesma prioridade.
- **CA4** — Consultar uma fila vazia sem erro.
- **CA5** — Verificar sucesso e rejeição de entradas inválidas.

Tarefas:

| ID | Tarefa | Cobre | Responsável | Depende de | Unitário | Integração (PostgreSQL) | Aceitação |
|---|---|---|---|---|---|---|---|
| R1-02A | Validar prioridade, empate, FIFO e fila vazia | CA2, CA3, CA4, CA5 | Abelardo | — | `FilaRequisicoesTest` | — | — |
| R1-02B | Validar criação e consulta de requisições via API e PostgreSQL | CA1, CA2, CA3, CA4, CA5 | Danilo | R1-02A | — | `FluxoDaUnidadeUmIT` (`filaRespeitaPrioridade`, `envelopeDeErro`) | Cenários `@R1-02B` |

Card: https://app.clickup.com/t/90132678197/86akumh52

## US-R1-03 — Processar alocação provisória U1

Como operador, quero processar uma requisição e consultar o resultado
persistido para acompanhar a reserva das bolsas.

Critérios de aceite:

- **CA1** — Selecionar bolsas disponíveis do mesmo tipo sanguíneo e componente,
  respeitando a ordem de chegada.
- **CA2** — Recusar bolsas vencidas ou incompatíveis com a política provisória U1.
- **CA3** — Tratar estoque insuficiente sem registrar uma alocação parcial indevida.
- **CA4** — Identificar a política como PROVISORIA_U1 na resposta.
- **CA5** — Confirmar o estado persistido por uma consulta posterior.

Tarefas:

| ID | Tarefa | Cobre | Responsável | Depende de | Unitário | Integração (PostgreSQL) | Aceitação |
|---|---|---|---|---|---|---|---|
| R1-03A | Validar seleção, validade, compatibilidade U1 e estoque vazio | CA1, CA2, CA3 (estoque vazio), CA4 | Abelardo | R1-01A | `AlocacaoProvisoriaU1Test`, `BolsaTest.validadeNaOperacao` | — | — |
| R1-03B | Validar a resposta da alocação e consultar novamente o estado persistido | CA1, CA2, CA3 (estoque vazio), CA4, CA5 | Danilo | R1-03A, R1-01B, R1-02B | — | `FluxoDaUnidadeUmIT.alocacaoProvisoria`, `ConcorrenciaNaAlocacaoIT` | Cenários `@R1-03B` |

Card: https://app.clickup.com/t/90132678197/86akumh53

**CA3 em aberto:** com estoque vazio, a API responde 409 e a requisição continua
na fila, e isso está coberto. Com estoque insuficiente (pediu 3, há 1), o
serviço aloca a bolsa disponível e marca a requisição como `ALOCADA`. A equipe
precisa decidir se essa alocação parcial é aceitável; se não for, a correção
entra por TDD numa tarefa nova.

## Adiado para a Unidade 2

Estes requisitos ficam **fora da U1** e não entram em nenhuma história da
Release 1. As histórias acima usam só a política provisória `PROVISORIA_U1`.

| Requisito | Situação na U1 | Onde entra |
|---|---|---|
| Compatibilidade ABO/Rh | Adiado. A U1 exige tipo sanguíneo exatamente igual | U2 — `MatrizCompatibilidade` |
| FEFO (sai primeiro a bolsa que vence primeiro) | Adiado. A U1 aloca pela ordem de chegada; a validade só é verificada | U2 — `FefoSeletor` |
| Índice de estoque em tabela hash | Adiado. A U1 consulta o estoque direto | U2 — `IndiceEstoqueHash` |
| Roteirização (vizinho mais próximo) | Fora da Release 1. O algoritmo já foi antecipado no código (`VizinhoMaisProximoRoteirizador`, usado em `POST /entregas/{id}/roteirizacao`), mas nenhuma história da Release 1 depende dele e a alocação U1 não o usa | U2 — `Roteirizador` |

`GET /api/v1/algoritmos/situacao` mostra pela API o que já está implementado e
o que ainda é stub.

## Importação no board

[story-map-release-1.csv](story-map-release-1.csv) traz as histórias e as
tarefas com os mesmos IDs, o pai de cada tarefa, o responsável, as dependências,
os critérios cobertos e o card. No ClickUp, o import de CSV mapeia as colunas na
hora de importar; se a coluna de dependências não for reconhecida, ela serve de
roteiro para criar as dependências à mão.

## Rastreabilidade e conclusão

O ClickUp registra responsáveis, status e dependências.
A task E2-02 deve relacionar cada tarefa aos testes unitários,
de integração e de aceitação correspondentes.

Este documento define o escopo e os critérios; não comprova sua execução.
A conclusão de cada história depende da validação dos critérios,
dos testes e dos links de commits, PRs e execuções da CI.

O histórico de TDD deve mostrar testes vermelhos antes da implementação
das mudanças de regra. Testes preexistentes serão identificados como
baseline, sem alegação retroativa de TDD.
