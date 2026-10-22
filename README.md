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
git clone https://github.com/Bloodell/bloodell-backend.git bloodell-backend
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
> 
## Decisões de containerização

| Decisão | Alternativa considerada | Justificativa |
|---|---|---|
| Eclipse Temurin 25: JDK no build e JRE na execução | Usar JDK também na imagem final | Mantém a versão Java do projeto e reduz o tamanho e as ferramentas disponíveis na imagem final. A aplicação roda com usuário sem privilégios de root. |
| Dockerfile multi-stage | Compilar e executar em um único estágio | Separa compilação e execução. Somente o JAR gerado é copiado do builder para a imagem final, sem fontes ou ferramentas de build. |
| Testes unitários no builder | Construir a imagem sem executar testes | Uma falha nos testes rápidos interrompe a construção. Integração com PostgreSQL e paridade C11/Java são verificadas separadamente na CI com `./mvnw -B verify`. |
| Volume nomeado `postgres-data` | Guardar os dados apenas no container | Preserva o banco quando os containers são removidos e recriados. `docker compose down` mantém o volume; `docker compose down -v` remove os dados. |
| Comunicação pelo nome do serviço `postgres` | Fixar o endereço IP do container | O DNS do Compose permite que a API encontre o banco em `postgres:5432` pela rede `dados`, mesmo quando o IP do container muda. |
| Rede interna `dados` e portas locais | Publicar API e banco em todas as interfaces | A API e o banco se comunicam pela rede interna. Para desenvolvimento, as portas publicadas ficam vinculadas a `127.0.0.1`. |
| Healthchecks e `depends_on` com `service_healthy` | Iniciar os serviços sem verificar disponibilidade | A API só começa a iniciar depois que o PostgreSQL aceita conexões. O healthcheck da API consulta `/actuator/health`. |
| Variáveis de ambiente e `.env` ignorado pelo Git | Colocar credenciais diretamente nos arquivos versionados | O `.env.example` documenta a configuração local. As credenciais reais ficam fora do repositório e devem ser configuradas no ambiente do provedor durante o deploy. |

## Pipeline e deploy na EC2

O workflow `.github/workflows/ci.yml` roda os testes C11 e `./mvnw verify` em
pushes e pull requests. Somente depois dos dois jobs verdes ele constrói a imagem
Docker. Em um **push na `main`** (inclusive merge de PR), o mesmo job envia essa
imagem para a EC2 por SSH, atualiza o checkout de `main`, sobe API e PostgreSQL
com Compose e exige que `/actuator/health` responda com sucesso. Em PRs, a
imagem é validada, mas o deploy não ocorre. O antigo workflow de deploy que só
imprimia um lembrete foi removido.

O ambiente de demonstração usa o IP público atual
`https://34.235.158.105/actuator/health`; a interface é servida em
`https://34.235.158.105/`. O HTTP redireciona para HTTPS. O IP público
automático pode mudar se a instância for parada e iniciada novamente. Nesse
caso, atualizar `EC2_HOST` e
`EC2_KNOWN_HOSTS` nas variáveis do repositório, reemitir o certificado para o
novo IP e atualizar os links desta seção.

Na EC2, `~/bloodell-backend/.env` contém a senha do banco e não entra no Git.
O `compose.deploy.yaml` versionado escolhe a imagem pelo SHA do commit e ativa
o perfil `prod`; o volume `postgres-data` preserva os dados entre deploys. A
Actions usa o secret `EC2_SSH_KEY` e as variáveis `EC2_HOST` e
`EC2_KNOWN_HOSTS`. Nunca imprimir ou versionar a chave privada. O frontend está
em outro repositório e sua publicação ainda não faz parte desta esteira.

Para voltar à imagem anterior, localizar no log da Actions o SHA do último
deploy saudável e, na EC2, executar:

```bash
cd ~/bloodell-backend
sha_anterior='SUBSTITUA_PELO_SHA_DO_DEPLOY_ANTERIOR'
BLOODELL_IMAGE_TAG="$sha_anterior" docker compose -f compose.yaml -f compose.deploy.yaml up -d --no-build --wait
curl --fail http://127.0.0.1:8080/actuator/health
```

O rollback reutiliza uma imagem já carregada na EC2. Não remover imagens antigas
antes de confirmar o novo deploy. Migrações Flyway incompatíveis podem exigir
restauração do banco; trocar só a imagem não reverte o schema.

| Decisão | Alternativa considerada | Justificativa |
|---|---|---|
| GitHub Actions com `needs` | Deploy em workflow paralelo | Os testes e a imagem precisam concluir antes da publicação; uma execução vermelha não alcança a EC2. |
| EC2 existente e SSH com chave dedicada | Criar outro serviço no Render | A equipe já tem a EC2 e a URL funcionando; a chave fica em GitHub Secrets e a identidade do servidor é fixada em `EC2_KNOWN_HOSTS`. |
| Transferir a imagem pelo SHA | Recompilar na EC2 | Publica exatamente a imagem que passou pelo job da esteira e permite voltar a uma versão anterior. |
| PR para `dev` e promoção de `dev` para `main` | Publicar cada branch | Mantém a integração do grupo em `dev` e reserva o deploy público para merges aprovados em `main`. |

O Nginx usa certificado Let's Encrypt para o IP, emitido pelo Certbot 5.8 via
`webroot`. O certificado dura cerca de seis dias; o timer
`snap.certbot.renew.timer` tenta renová-lo automaticamente, e o hook
`/etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh` recarrega o Nginx após
renovação bem-sucedida. A rota HTTP `/.well-known/acme-challenge/` permanece
acessível para essa validação. A renovação foi simulada com
`certbot renew --dry-run --run-deploy-hooks` e passou.

**Limites atuais:** o IP público automático ainda pode mudar após uma parada da
EC2. A API não tem autenticação, portanto o ambiente público deve conter somente
dados sintéticos. O frontend estático tem seu próprio processo de publicação e
ainda precisa de CI/CD.

As evidências de uma execução vermelha, uma verde com deploy e a URL validada
devem ser vinculadas aqui após o primeiro merge em `main`. Uma CI verde em PR
não é evidência de deploy.

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

## Documentação complementar:

- [Story map da Release 1](docs/story-map-release-1.md) — backbone, três histórias, critérios de aceite e tarefas vinculadas ao ClickUp.

- `docs/adr/` — decisões de arquitetura e o que dispara a revisão de cada uma
- `docs/complexidade-u1.md` — Big-O medido na implementação real
- `docs/w04-estruturas-base.md` — evidências da W04–W05: estruturas-base, CI/CD e topologia de comunicação
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
