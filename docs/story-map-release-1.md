# Story map e testes da Release 1 — desafio de Infra 2026.2

Backbone: **cadastrar unidades → registrar doação → consultar estoque → criar
requisição → priorizar fila → alocar bolsa → acompanhar entrega**. A Release 1
avaliada aqui cobre os três passos de negócio abaixo, da API ao PostgreSQL.
Entrega, roteirização, FEFO e compatibilidade ABO/Rh completa continuam fora do
escopo destas três histórias; na U1, a alocação exige tipo exatamente igual e
respeita a ordem de chegada.

| História | Critério de aceite | Tarefas de regra e seus testes unitários | Integração com PostgreSQL / aceitação HTTP |
|---|---|---|---|
| US-R1-01 — Registrar doação e disponibilizar bolsas | Operador registra doação em hemocentro; cada componente solicitado gera uma bolsa disponível e consultável. Componente repetido é recusado antes de gravar. | R1-01A: validade, estados e reserva em `BolsaTest`; R1-01C: recusar componente repetido em `DoacaoServiceTest.naoGeraDuasBolsasDoMesmoComponente`. | R1-01B: `HistoriasReleaseUmIT.registrarDoacaoEDisponibilizarBolsa` grava, consulta doação e bolsa em outra requisição HTTP e verifica 422 para repetição. `CadastrosIT.consultaDeDoacoes` também cobre consulta. |
| US-R1-02 — Criar e consultar requisições prioritárias | Operador cria pedidos de hospital; emergência vem antes de urgente, e pedidos da mesma prioridade preservam chegada. | R1-02A: prioridade, empate, FIFO e fila vazia em `FilaRequisicoesTest`. | R1-02B: `HistoriasReleaseUmIT.criarEConsultarRequisicoesPrioritarias` cria três pedidos, lê a fila e consulta um pedido persistido; `FluxoDaUnidadeUmIT.filaRespeitaPrioridade` cobre cenário sintético adicional. |
| US-R1-03 — Processar alocação provisória U1 | Pedido na fila recebe uma bolsa elegível, com política didática declarada, passos da decisão e estados persistidos. | R1-03A: elegibilidade, igualdade de tipo/componente, estoque vazio e limite da U1 em `AlocacaoProvisoriaU1Test` e `BolsaTest`. | R1-03B: `HistoriasReleaseUmIT.processarAlocacaoProvisoria` lê novamente pedido e bolsa após processar. `ConcorrenciaNaAlocacaoIT` protege contra dupla alocação. |

As três tarefas de aceitação formam uma jornada ordenada em
`HistoriasReleaseUmIT`. O teste usa PostgreSQL 17 via Testcontainers e requisições
HTTP reais; o `@BeforeAll` limpa apenas o banco efêmero do teste. O teste unitário
R1-01C usa mocks apenas para isolar a regra antes de qualquer persistência.

## Cenários de aceitação

```gherkin
Funcionalidade: doação e estoque
  Cenário: uma doação disponibiliza bolsa para consulta posterior
    Dado um hemocentro cadastrado
    Quando registro uma doação A+ de 450 ml com PLASMA
    Então a API responde 201 com uma bolsa gerada
    E a consulta posterior da bolsa responde DISPONIVEL
  Cenário: componente repetido não cria duas bolsas
    Quando peço PLASMA duas vezes na mesma doação
    Então a API responde 422 sem registrar a doação

Funcionalidade: fila hospitalar
  Cenário: emergência precede urgência sem perder FIFO
    Dado um hospital cadastrado
    Quando registro urgente 1, emergência e urgente 2 nessa ordem
    Então a fila responde emergência, urgente 1, urgente 2
    E a requisição consultada permanece NA_FILA no banco

Funcionalidade: alocação provisória U1
  Cenário: bolsa disponível é alocada ao pedido
    Dado uma bolsa A+ de PLASMA disponível e um pedido equivalente na fila
    Quando processo a alocação
    Então a API responde 201 e informa PROVISORIA_U1 com passos da decisão
    E consultas posteriores mostram bolsa e pedido ALOCADA
```

Os cenários são especificação legível; a execução automatizada correspondente
está nos métodos Java acima, sem dependência adicional de runner Gherkin.

## Jornada TDD verificável

- Baseline: testes e funcionalidades anteriores aos commits desta entrega **não**
  são apresentados como TDD retroativo.
- Vermelho: `fa98630` adicionou somente `DoacaoServiceTest`, que falhou porque o
  serviço aceitou dois componentes iguais. O commit `15798af` ampliou a CI para
  qualquer branch e registrou a [execução vermelha](https://github.com/Bloodell/bloodell-backend/actions/runs/39361848840), com build de imagem pulado.
- Verde: `94c9e41` rejeitou componentes repetidos antes de salvar a doação. A
  [execução verde](https://github.com/Bloodell/bloodell-backend/actions/runs/39363541316)
  concluiu testes, paridade e build da imagem Docker.

Essa regra foi priorizada porque a duplicação inflaria o estoque de bolsas sem
correspondência com uma coleta real. Há cobertura para o fluxo principal e para
erros selecionados; limites ainda não cobertos por estas histórias incluem
falhas de rede e recuperação após interrupção do provedor.
