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

| Atividade do backbone | História da Release 1 |
|---|---|
| Registrar doações e consultar bolsas/estoque | US-R1-01 — Registrar doação e disponibilizar bolsas |
| Registrar demandas e consultar a fila | US-R1-02 — Criar e consultar requisições prioritárias |
| Processar alocações e consultar resultados | US-R1-03 — Processar alocação provisória U1 |

## US-R1-01 — Registrar doação e disponibilizar bolsas

Como operador do hemocentro, quero registrar uma doação e consultar
as bolsas geradas para acompanhar sua disponibilidade no estoque.

Critérios de aceite:

- Registrar uma doação individual pela API.
- Gerar bolsas e validade conforme as regras da U1.
- Consultar posteriormente as bolsas e o estoque persistidos no PostgreSQL.
- Verificar sucesso e rejeição de entradas inválidas.

Tarefas:

- R1-01A — Validar geração e validade das bolsas por regra, com testes unitários.
- R1-01B — Validar registro e consulta via API real e PostgreSQL, com teste de aceitação.

Card: https://app.clickup.com/t/90132678197/86akumh51

## US-R1-02 — Criar e consultar requisições prioritárias

Como operador, quero registrar demandas hospitalares e consultar a fila
para visualizar a ordem de atendimento por prioridade e chegada.

Critérios de aceite:

- Criar e consultar requisições pela API com persistência no PostgreSQL.
- Ordenar por EMERGENCIA, URGENTE e ELETIVA.
- Preservar a ordem de chegada entre requisições da mesma prioridade.
- Consultar uma fila vazia sem erro.
- Verificar sucesso e rejeição de entradas inválidas.

Tarefas:

- R1-02A — Validar prioridade, empate, FIFO e fila vazia, com testes unitários.
- R1-02B — Validar criação e consulta de requisições via API e PostgreSQL, com teste de aceitação.

Card: https://app.clickup.com/t/90132678197/86akumh52

## US-R1-03 — Processar alocação provisória U1

Como operador, quero processar uma requisição e consultar o resultado
persistido para acompanhar a reserva das bolsas.

Critérios de aceite:

- Selecionar bolsas disponíveis do mesmo tipo sanguíneo e componente,
  respeitando a ordem de chegada.
- Recusar bolsas vencidas ou incompatíveis com a política provisória U1.
- Tratar estoque insuficiente sem registrar uma alocação parcial indevida.
- Identificar a política como PROVISORIA_U1 na resposta.
- Confirmar o estado persistido por uma consulta posterior.

Tarefas:

- R1-03A — Validar seleção, validade, compatibilidade U1 e estoque vazio,
  com testes unitários.
- R1-03B — Validar a resposta da alocação e consultar novamente o estado
  persistido, com teste de aceitação via API e PostgreSQL.

Card: https://app.clickup.com/t/90132678197/86akumh53

## Limites deste recorte

Estas três histórias usam a política provisória U1. Compatibilidade
ampliada ABO/Rh, FEFO, índice hash e roteirização não são requisitos
destas histórias.

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
