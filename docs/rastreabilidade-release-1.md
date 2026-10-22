# Rastreabilidade — Release 1 (E2-02)

Liga cada linha do [story map da Release 1](story-map-release-1.md) aos testes
que a comprovam. Os nomes abaixo são os que aparecem nos relatórios do
surefire, do failsafe e do Cucumber.

## Onde cada tipo de teste roda

| Tipo | O que exercita | Onde fica | Quem executa |
|---|---|---|---|
| Unitário | Regra de domínio isolada, sem Spring nem banco | `src/test/java/**/*Test.java` | surefire (`mvnw test`) |
| Integração | API HTTP real + PostgreSQL 17 no Testcontainers | `src/test/java/**/*IT.java` | failsafe (`mvnw verify`) |
| Aceitação | Cenários Gherkin executados pela API real + PostgreSQL | `src/test/resources/aceitacao/*.feature`, rodados por `AceitacaoIT` | failsafe (`mvnw verify`) |

Nenhum desses testes usa mock de API ou de banco. Os cenários de aceitação só
chamam endpoints HTTP: conferem o estado persistido com uma nova consulta à API,
nunca lendo o repositório. Cada cenário começa com o banco vazio.

Cada cenário Gherkin leva a tag da história (`@US-R1-0x`) e da tarefa
(`@R1-0xB`), o que permite rodar só uma parte:

```bash
./mvnw -B verify -Dit.test=AceitacaoIT -Dcucumber.filter.tags="@US-R1-03"
```

### Baseline e testes novos

Seguindo o story map, nada aqui alega TDD retroativo:

- **Criados na E2-02:** `FabricaDeBolsasTest` e todos os cenários Gherkin. Eles
  descrevem um comportamento que já existia. Nenhuma regra mudou nesta task,
  então não há ciclo vermelho → verde de implementação a mostrar.
- **Baseline:** todos os outros testes citados já existiam antes desta task.

## US-R1-01 — Registrar doação e disponibilizar bolsas

| Tarefa | Unitário | Integração (PostgreSQL) | Aceitação (Gherkin) |
|---|---|---|---|
| **R1-01A** — geração e validade das bolsas | `FabricaDeBolsasTest`: `validadePorComponente` (42, 365 e 5 dias), `herdaDadosDaDoacao`, `codigoRastreavel` · `BolsaTest`: `cicloInicial`, `validadeIncoerente`, `venceHojeAindaVale` | — | — |
| **R1-01B** — registro e consulta via API | — | `CadastrosIT.consultaDeDoacoes` | [us-r1-01-registrar-doacao.feature](../src/test/resources/aceitacao/us-r1-01-registrar-doacao.feature): ✅ *a doação gera bolsas com validade por componente e elas entram no estoque* · ❌ *tipo sanguíneo inválido é rejeitado sem gravar nada* · ❌ *doação em unidade que não é hemocentro é recusada pela regra de negócio* |

## US-R1-02 — Criar e consultar requisições prioritárias

| Tarefa | Unitário | Integração (PostgreSQL) | Aceitação (Gherkin) |
|---|---|---|---|
| **R1-02A** — prioridade, empate, FIFO e fila vazia | `FilaRequisicoesTest`: `filaAtendePrioridadeDepoisChegada`, `listagemRespeitaOrdemDeAtendimento`, `filaVaziaDevolveNulo`, `prioridadeDesconhecida`, `esvaziarEReusar` | — | — |
| **R1-02B** — criação e consulta via API | — | `FluxoDaUnidadeUmIT.filaRespeitaPrioridade`, `FluxoDaUnidadeUmIT.envelopeDeErro` | [us-r1-02-requisicoes-prioritarias.feature](../src/test/resources/aceitacao/us-r1-02-requisicoes-prioritarias.feature): ✅ *a fila ordena por prioridade e preserva a chegada no empate* · ✅ *a fila vazia é consultada sem erro* · ❌ *requisição sem itens é rejeitada e não entra na fila* · ❌ *quantidade zero é rejeitada e não entra na fila* |

## US-R1-03 — Processar alocação provisória U1

| Tarefa | Unitário | Integração (PostgreSQL) | Aceitação (Gherkin) |
|---|---|---|---|
| **R1-03A** — seleção, validade, compatibilidade U1 e estoque vazio | `AlocacaoProvisoriaU1Test`: `naoAplicaFefo`, `naoAplicaCompatibilidade`, `respeitaStatusEComponente`, `estoqueVazio`, `avisoEhExplicito` · `BolsaTest.validadeNaOperacao` (bolsa vencida não pode ser alocada) | — | — |
| **R1-03B** — resposta da alocação e reconsulta do estado persistido | — | `FluxoDaUnidadeUmIT.alocacaoProvisoria` · `ConcorrenciaNaAlocacaoIT.apenasUmaAlocacaoVence` | [us-r1-03-alocacao-provisoria.feature](../src/test/resources/aceitacao/us-r1-03-alocacao-provisoria.feature): ✅ *aloca a primeira bolsa do mesmo tipo pela ordem de chegada, sem FEFO* · ❌ *tipo diferente não é usado, porque a U1 não aplica compatibilidade ABO/Rh* · ❌ *bolsa vencida é recusada e a requisição continua na fila* · ❌ *estoque vazio não registra alocação e a requisição continua na fila* |

✅ cenário de sucesso · ❌ cenário de erro ou de regra de negócio

### O que os cenários acrescentam ao `FluxoDaUnidadeUmIT`

O `FluxoDaUnidadeUmIT` continua valendo como aceite do fluxo completo sobre o
cenário sintético (`semente: 42`) e não foi duplicado. Os cenários Gherkin cobrem
o que ele não fixa:

- **R1-01B:** o fluxo usa o seed e nunca chama `POST /doacoes`. Os cenários
  registram a doação pela API e conferem validade, reconsulta e estoque.
- **R1-02B:** o fluxo confere a ordem da fila, mas não o desempate por chegada
  com dados controlados, nem a fila vazia ou a rejeição de requisição inválida.
- **R1-03B:** o fluxo aceita `201` **ou** `409`, porque depende do que o seed
  gerou. Os cenários fixam cada resultado com dados próprios e reconsultam
  requisição e bolsas depois da alocação.

## Teste vermelho bloqueia a CI

- `mvnw -B verify` falha quando qualquer teste unitário, de integração, de
  aceitação ou a paridade C11 × Java falha. Isso foi conferido nesta task:
  trocar a ordem esperada da fila no `.feature` deixou o build vermelho
  (`Tests run: 11, Failures: 1`, `exit 1`).
- `dev` e `main` têm um ruleset no GitHub que exige três checks verdes para o
  merge: *Estruturas da AED em C11*, *Build, testes e paridade C11 x Java* e
  *Build da imagem Docker*. A regra pode ser conferida em
  `https://api.github.com/repos/Bloodell/bloodell-backend/rules/branches/dev`.
- Prova prática: no [PR #12](https://github.com/Bloodell/bloodell-backend/pull/12),
  aberto com um teste vermelho de propósito, o check *Build, testes e paridade*
  terminou em `failure` e o *Build da imagem* em `skipped`, então o merge ficou barrado.

## Relatórios preservados

O job *Build, testes e paridade C11 x Java* publica, mesmo quando falha
(`if: always()`):

| Artefato | Conteúdo |
|---|---|
| `relatorios-de-teste` | `target/surefire-reports/`, `target/failsafe-reports/` e `target/cucumber/` (relatório HTML e JSON dos cenários) |
| `paridade-aed` | `target/paridade/` (saídas do comparador C11 × Java) |

## Pendência para decisão da equipe

O critério da US-R1-03 diz *"tratar estoque insuficiente sem registrar uma
alocação parcial indevida"*. Hoje, se a requisição pede 3 bolsas e só 1 está
disponível, o `AlocacaoService` aloca essa 1 e marca a requisição como
`ALOCADA` (`quantidadeAlocada` < `quantidadeSolicitada`). Estoque **vazio**
está coberto (409, a requisição continua na fila). Falta decidir se a
alocação parcial é aceitável:

- **se não for**, a mudança é de regra e entra por TDD: primeiro um cenário
  vermelho, depois a correção no serviço;
- **se for**, o critério do story map precisa ser reescrito para dizer isso.

Por isso não há cenário de estoque insuficiente nesta task.
