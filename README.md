# BLOODELL — backend

API REST de gestão, distribuição e monitoramento de hemocomponentes.
Projeto integrador do 3º semestre de Análise e Desenvolvimento de Sistemas — CESAR School — 2026.2.
Execução da equipe para o tema oficial do PI, "Rota Vital".

> **Aviso acadêmico.** Todos os dados são sintéticos. Não há nenhuma informação real
> de doador ou de paciente. As regras de compatibilidade sanguínea são didáticas e
> **não constituem orientação clínica** nem substituem protocolo de hemoterapia.
> Não há integração com Hemorrede, SUS ou qualquer sistema oficial.

Frontend em repositório separado: `bloodell-frontend`.

## Tecnologias

| Camada | Escolha |
|---|---|
| Linguagem / framework | Java 25 + Spring Boot 4.1.1 |
| Build | Maven (via wrapper `mvnw`) |
| Banco | PostgreSQL 17, publicado apenas em `127.0.0.1:${POSTGRES_PORT:-5432}` no ambiente local |
| Migrations | Flyway (SQL versionado em `src/main/resources/db/migration`) |
| Documentação da API | OpenAPI/Swagger (springdoc) |
| Testes | JUnit Jupiter, Mockito, Testcontainers |
| Estruturas de dados (AED) | Java puro + espelho em C11 (`academic/c`) |
| Infraestrutura | Docker, Docker Compose, GitHub Actions |

Sem Lombok e sem nenhum gerador de boilerplate: construtores, getters, `equals` e
`hashCode` são escritos à mão.

## De zero à API rodando (≈ 15 minutos)

Pré-requisitos: **Git**, **Docker Desktop** (ou Docker Engine) e, para o build local,
**JDK 25**. Compilador C11 (`cc`, `gcc` ou `clang`) e `make` são necessários apenas para
a paridade da AED.

```bash
# 1. clonar
git clone <url-do-repo> bloodell-backend
cd bloodell-backend

# 2. criar o arquivo de variáveis a partir do exemplo (o .env nunca é versionado)
cp .env.example .env
# edite POSTGRES_PASSWORD e BLOODELL_DB_PASSWORD com o mesmo valor

# 3. subir API + PostgreSQL
docker compose up --build -d
docker compose ps

# 4. conferir a saúde da aplicação
curl http://127.0.0.1:8080/actuator/health          # Linux/macOS
# Invoke-RestMethod http://127.0.0.1:8080/actuator/health   # PowerShell

# 5. popular o cenário sintético reproduzível
curl -X POST http://127.0.0.1:8080/api/v1/demonstracoes/cenarios \
  -H "Content-Type: application/json" \
  -d '{"cenario":"aula","semente":42}'

# 6. abrir a documentação da API
# http://127.0.0.1:8080/swagger-ui.html
```

**Tempo estimado por etapa:** clonar e configurar, 3 min · primeiro
`docker compose up --build` (baixa imagens e dependências Maven), 8 a 12 min ·
validar com o seed, 2 min. A partir da segunda vez, subir leva menos de 1 minuto.

Comandos do dia a dia:

```bash
docker compose logs -f api                      # acompanhar os logs
docker compose exec postgres psql -U bloodell -d bloodell   # abrir o banco
docker compose down                             # parar, preservando os dados
docker compose down -v                          # parar e APAGAR o banco (deliberado)
```

### Build e testes locais (sem Docker para a API)

```bash
./mvnw -B verify          # testes rápidos + integração (Testcontainers) + paridade C11 x Java
./mvnw -B test            # só os testes rápidos (não precisa de Docker)
./mvnw -B verify -Psem-paridade   # máquina sem compilador C11 (a etapa fica sem evidência)

cd academic/c && make testar      # só os testes das estruturas em C11
```

O estágio de build do `Dockerfile` executa `./mvnw -B test` antes de empacotar a
aplicação. Assim, uma falha nos testes rápidos interrompe a construção da imagem.
Os testes de integração com Testcontainers e a paridade C11 x Java continuam
como etapas explícitas do `mvnw verify` e da CI, pois exigem recursos adicionais
que não devem ser executados implicitamente dentro do build da imagem.

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

> **Wrapper do Maven.** Já está no repositório (`mvnw`, `mvnw.cmd`, `.mvn/`), na
> versão 3.3.4 com `distributionType=only-script`: não há nenhum `.jar` versionado
> aqui. Na primeira execução, o wrapper baixa o **Maven 3.9.16** do repositório
> oficial e o guarda em cache — por isso a primeira vez demora mais. Todo mundo
> na equipe e o CI usam exatamente esse Maven, mesmo quem tiver outra versão
> instalada na máquina.
>
> O `.gitattributes` fixa as quebras de linha de `mvnw` e do `Makefile` em LF. Sem
> isso, um clone no Windows pode gravar CRLF e o build falha com
> `bad interpreter: /bin/sh^M` ou `missing separator`.

## Estrutura do projeto

```
src/main/java/br/edu/cesar/bloodell/
  config/            composição Spring, CORS, OpenAPI, correlação de log, extensão de auth
  compartilhado/     Value Objects, exceções e o envelope de erro da API
  unidade/ doacao/ bolsa/ estoque/ requisicao/ alocacao/
  rastreabilidade/ veiculo/ telemetria/ alerta/ indicadores/
  algoritmos/        inspeção didática das estruturas (visualizador)
  demonstracao/      cenário sintético reproduzível
  aed/               MÓDULO ACADÊMICO — Java puro, sem Spring e sem JPA
    contratos/       dados simples espelhados em C
    u1/              lista, fila e pilha com nós manuais
    u2/              algoritmos U2; vizinho mais próximo implementado
academic/c/          as mesmas estruturas em C11 (aulas de AED)
tests/parity/        fixtures comuns às duas implementações
docs/                decisões (ADR), complexidade, concorrência, checklists
scripts/paridade.py  comparador C11 x Java, ligado ao `mvnw verify`
```

Cada feature tem `dominio/`, `aplicacao/`, `apresentacao/` e `infraestrutura/` —
as camadas controller/service/repository existem dentro da feature, e não em
pastas gigantes por tipo de classe.

## Separação Unidade 1 / Unidade 2

| | Unidade 1 (agora) | Unidade 2 (a partir da semana 11) |
|---|---|---|
| Estruturas | lista, fila, pilha (implementadas) | — |
| Alocação | `AlocacaoProvisoriaU1`: primeira bolsa **do mesmo tipo**, pela ordem de chegada | matriz ABO/Rh + FEFO |
| Ordenação por validade | **não existe** | FEFO por inserção ordenada |
| Índice de estoque | consulta direta | tabela hash manual |
| Rota | **não existia** | vizinho mais próximo sobre distâncias geográficas |

Em `aed/u2`, matriz de compatibilidade, FEFO e índice hash continuam como
interfaces e stubs que lançam `UnsupportedOperationException`. O roteirizador
usa um grafo completo não direcionado com pesos Haversine e a heurística do
vizinho mais próximo. A rota estimada pode ser calculada em
`POST /api/v1/entregas/{id}/roteirizacao` enquanto a entrega estiver planejada.
`GET /api/v1/algoritmos/situacao` lista os algoritmos implementados e pendentes.

## Endpoints principais

| Método e rota | O que faz |
|---|---|
| `POST /api/v1/doacoes` | registra doação e gera as bolsas |
| `GET /api/v1/doacoes`, `GET /api/v1/doacoes/{id}` | consulta doações e as bolsas geradas (sem PUT/DELETE: preserva a rastreabilidade) |
| `GET /api/v1/bolsas?status=&componente=&unidadeId=&page=&size=` | lista bolsas |
| `GET /api/v1/bolsas/{id}/rastreabilidade` | linha do tempo da bolsa |
| `GET /api/v1/estoques?unidadeId=&componente=` | posição de estoque |
| `GET · POST /api/v1/hospitais` e `/api/v1/hemocentros`, `GET · PUT · DELETE .../{id}` | cadastro de unidades (DELETE responde 409 se houver registros vinculados) |
| `POST /api/v1/requisicoes` | cria requisição hospitalar |
| `GET · PUT · DELETE /api/v1/requisicoes/{id}` | consulta, altera a prioridade (só na fila) ou cancela |
| `GET /api/v1/requisicoes/fila` | fila produzida pela estrutura da AED |
| `POST /api/v1/requisicoes/{id}/alocacoes` | processa e aloca (201 / 409 / 422) |
| `GET · POST /api/v1/entregas`, `GET · PUT · DELETE /api/v1/entregas/{id}` | planeja, consulta, ajusta ou cancela uma entrega |
| `POST /api/v1/entregas/{id}/roteirizacao` | calcula e persiste rota geográfica estimada para uma entrega planejada |
| `POST /api/v1/entregas/{id}/despacho · /inicio-transporte · /conclusao` | avança o fluxo de entrega até o atendimento da requisição |
| `POST /api/v1/telemetrias` | ingestão assíncrona (202) |
| `GET /api/v1/telemetrias/metricas` | números da ingestão concorrente (SO) |
| `GET /api/v1/alertas` | alertas ativos de cadeia fria e falha de comunicação |
| `GET · POST /api/v1/veiculos`, `GET · PUT · DELETE /api/v1/veiculos/{id}` | cadastro da frota (base precisa ser hemocentro; veículo em rota não é alterado) |
| `GET /api/v1/veiculos/{id}/telemetrias?de=&ate=` | histórico de cadeia fria |
| `GET /api/v1/algoritmos/estoque · /fila · /historico · /situacao` | visualizador |
| `GET /api/v1/indicadores/estoque · /demanda · /tempos` | painéis descritivos |
| `POST /api/v1/demonstracoes/cenarios` | cenário sintético (`semente: 42`) |
| `GET /actuator/health` | health check |

Documentação completa e navegável em `/swagger-ui.html`.

### Formato de erro

Todo erro responde no mesmo envelope, inspirado na RFC 7807:

```json
{
  "type": "https://bloodell.cesar.school/erros/regra-de-negocio",
  "title": "Regra de negocio violada",
  "status": 422,
  "detail": "A bolsa BL-000012-HEM-7 venceu em 2026-09-10 e nao pode ser reservada.",
  "instance": "/api/v1/requisicoes/12/alocacoes",
  "momento": "2026-09-19T21:40:11-03:00",
  "correlacao": "a1b2c3d4e5f6",
  "campos": [{ "campo": "quantidade", "mensagem": "deve ser maior que zero" }]
}
```

O campo `correlacao` é o mesmo valor do cabeçalho `X-Correlacao-Id` e aparece
entre colchetes em cada linha de log daquela requisição.

## Configuração e segredos

Nenhuma senha vai para o Git. As variáveis vêm do `.env` (ignorado) e, no deploy,
do ambiente do provedor. O `.env.example` documenta todas elas.
Perfis: `local` (padrão, usado pelo Compose), `test` (integração) e `prod`.

## Documentação complementar

- `docs/adr/` — decisões de arquitetura e o que dispara a revisão de cada uma
- `docs/complexidade-u1.md` — Big-O medido na implementação real
- documentação formal de equivalência C ↔ Java será consolidada em etapa posterior
- `docs/concorrencia.md` — os pontos de concorrência e como são testados
- `docs/versionamento-api.md` — regra de mudança de contrato
- `docs/checklist-pronto-para-demo.md` — checagem na véspera de cada entrega
- `docs/pendencias-academicas.md` — conflitos do blueprint aguardando os professores

## Equipe

| Nome | E-mail institucional | Área |
|---|---|---|
| _preencher_ | _@cesar.school_ | Domínio e persistência |
| _preencher_ | _@cesar.school_ | Algoritmos AED (C e Java) |
| _preencher_ | _@cesar.school_ | Algoritmos AED (C e Java) |
| _preencher_ | _@cesar.school_ | API e integração |
| _preencher_ | _@cesar.school_ | Frontend e painéis |
| _preencher_ | _@cesar.school_ | Infraestrutura, CI/CD e telemetria |

## Entregas

| Entrega | Data | Link | Screencast |
|---|---|---|---|
| Entrega 01 | 31/08/2026 | — | — |
| Entrega 02 | 21/09/2026 | _preencher_ | _preencher_ |
| Entrega 03 | 19/10/2026 | — | — |
| Entrega 04 e apresentação | 09/11/2026 | — | — |
