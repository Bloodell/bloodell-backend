# Card 1 — Login com JWT

Card consultado em 29/10/2026: https://app.clickup.com/t/90132678197/86akuuutn
Branch de trabalho: `feature/autenticacao-jwt`.

## Revisao anterior a implementacao

| Ponto | Decisao neste card |
|---|---|
| `usuarios` existe desde V1, mas nao tinha entidade/repositorio | Reutilizar a tabela, mapear Usuario e os quatro perfis existentes. |
| Hash legado pode ser nulo | Conta sem BCrypt valido nao autentica; nunca interpretar valor legado como senha em texto. |
| E-mail UNIQUE distinguia maiusculas e espacos | V3 adiciona indice unico normalizado; revisar duplicatas antes de migrar banco existente. |
| Filtro de autenticacao apenas registrava aviso | Substituir por Spring Security stateless e validacao JWT. |
| Compose nao repassava configuracao de autenticacao | Repassar flag, chave, validade e provisionamento opcional. |
| CI so cobria `feat/**` em push, mas a branch usa `feature/**` | Incluir `feature/**` e `dev`; deploy continua exclusivo de `main`. |
| Frontend atual nao envia Bearer | Integracao pertence ao Card 3; ativacao coordenada antes do deploy. |
| Alocacao parcial com estoque insuficiente tem aceite pendente | Pendencia de negocio em `story-map-release-1.md`, independente do JWT. |
| W06/W08 e algoritmos U2 continuam pendentes | Nao sao pre-requisitos tecnicos do login. |

Nao foi necessario implementar outro card antes do login. A infraestrutura de
usuarios, chave e cadeia de seguranca faz parte desta entrega.

## Escopo e contrato

- POST `/api/v1/auth/login`: JSON `{ "email": "conta@example.test", "senha": "..." }`.
  Retorna 200 com `accessToken`, `tokenType: "Bearer"`, `expiresIn` (segundos) e
  `usuario: {id, nome, email, perfil}`. E-mail e comparado sem distinguir caixa.
- GET `/api/v1/auth/me`: exige `Authorization: Bearer <accessToken>` e retorna
  `{id, nome, email, perfil}`. Reconsulta o banco; conta removida ou sem hash valido
  perde acesso. O perfil retornado reflete o banco, nao uma alegacao do cliente.
- Credenciais incorretas, conta inexistente e hash legado invalido: 401 generico.
- Token ausente, malformado, expirado, assinatura/emissor/audiencia incorretos ou
  usuario inexistente: 401, sem stack trace nem detalhes da chave.
- JSON/campos invalidos: 400. Senhas acima de 72 bytes UTF-8 nao sao aceitas pelo
  BCrypt; nao truncamos senhas silenciosamente.
- Login e identidade usam `Cache-Control: no-store`. Nenhuma resposta expoe hash.
- JWT HS256, chave Base64 de pelo menos 32 bytes aleatorios, validade padrao de
  15 minutos. Assinatura, emissor, audiencia, subject, emissao, expiracao e `nbf`
  sao verificados. Sem tolerancia adicional depois da expiracao.
- Nao ha cadastro publico, refresh token nem logout/revogacao individual de token.
  Alterar a senha por si so nao revoga tokens existentes; remover a conta ou seu
  hash impede acesso, e trocar a chave invalida todos os tokens anteriores.

Com `BLOODELL_AUTH_HABILITADA=true`, todas as rotas exigem identidade, exceto:
POST login, GET health (e probes), OpenAPI e Swagger. Preflight CORS e atendido
somente para as origens configuradas. Sem cookies/sessoes/HTTP Basic; o Bearer
e lido do cabecalho Authorization. CSRF desabilitado nesse modelo sem credenciais
ambientais de browser; reavaliar se autenticacao por cookie for introduzida.

Com a flag `false`, permanece o modo demonstrativo anterior nas rotas de negocio,
e `/api/v1/auth/**` fica indisponivel (401), inclusive `/me`. Este modo NAO protege
a API e nao serve como evidencia de seguranca. A flag permanece falsa por padrao
para permitir coordenar os Cards 2 e 3 antes de mudar o ambiente publicado.

## Configuracao local

No `.env` ignorado pelo Git:

```dotenv
BLOODELL_AUTH_HABILITADA=true
BLOODELL_JWT_SEGREDO_BASE64=<gerar-localmente>
BLOODELL_JWT_EMISSOR=bloodell
BLOODELL_JWT_AUDIENCIA=bloodell-api
BLOODELL_JWT_VALIDADE=15m
```

Gere a chave em um terminal privado (nao copie a saida para issues/logs/videos):

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Ou em Linux: `openssl rand -base64 32`. Use valores diferentes em cada ambiente.
Sem chave valida e com autenticacao ligada, a aplicacao falha ao iniciar.
O Compose le o `.env`; para `mvnw spring-boot:run`, exporte as variaveis no processo,
pois Spring Boot nao carrega `.env` automaticamente.

Antes da V3 em banco ja usado, um administrador deve verificar:

```sql
SELECT lower(btrim(email)) AS email_normalizado, count(*)
FROM usuarios GROUP BY lower(btrim(email)) HAVING count(*) > 1;
SELECT id, email FROM usuarios WHERE senha_hash IS NULL;
```

Duplicatas precisam de revisao manual, sem excluir contas automaticamente.
Hashes legados ausentes/invalidos precisam ser provisionados administrativamente;
nao existe senha padrao nem conversao automatica de texto puro.

## Primeira conta administrativa

Somente para banco sem usuarios, configure temporariamente, alem das variaveis JWT:

```dotenv
BLOODELL_ADMIN_INICIAL_HABILITADO=true
BLOODELL_ADMIN_NOME=<nome>
BLOODELL_ADMIN_EMAIL=<email>
BLOODELL_ADMIN_SENHA=<senha-forte-local>
```

A senha deve ter pelo menos 12 caracteres e no maximo 72 bytes UTF-8. Ao iniciar,
a API grava BCrypt com custo 12 e perfil ADMIN. Nunca sobrescreve senha nem promove
conta existente. Reinicio com o mesmo admin ja provisionado nao altera a conta.
Se houver outras contas e o admin informado nao existir, o provisionamento falha.

Execute `docker compose up --build -d --wait`. Apos o primeiro uso, desligue
`BLOODELL_ADMIN_INICIAL_HABILITADO`, remova `BLOODELL_ADMIN_SENHA` do `.env` e recrie
o container da API com `docker compose up -d --wait api`. A conta fica no volume.
Nao rode `down -v` para configurar autenticacao: isso apagaria os dados.

## Verificacao manual

Exemplo em PowerShell, sem gravar senha no historico nem imprimir o JWT:

```powershell
$credencial = Get-Credential -Message 'Conta local BLOODELL (usuario = email)'
$entrada = @{ email = $credencial.UserName; senha = $credencial.GetNetworkCredential().Password } | ConvertTo-Json
$login = Invoke-RestMethod http://127.0.0.1:8080/api/v1/auth/login -Method Post -ContentType application/json -Body $entrada
Invoke-RestMethod http://127.0.0.1:8080/api/v1/auth/me -Headers @{ Authorization = "Bearer $($login.accessToken)" }
Remove-Variable entrada,credencial,login
```

O Swagger inclui o esquema `bearerAuth` e `/me` declara a exigencia de Bearer.
Em ambientes remotos, enviar credenciais e tokens somente via HTTPS.

## Testes e limites da entrega

`AutenticacaoIT` usa a API HTTP real, PostgreSQL/Testcontainers e uma chave aleatoria
gerada durante a execucao. Cobre login, BCrypt, identidade, token ausente/invalido/
expirado, outra assinatura, emissor/audiencia incorretos, subject invalido, `nbf`,
expiracao ausente, conta removida, perfil atualizado, CORS, rotas protegidas,
health publico e unicidade de email. `ConfiguracaoJwtTest` e `AdminInicialTest`
cobrem chave/configuracao e provisionamento sem sobrescrita de contas.

Comandos: `mvnw.cmd -B verify` (Windows) ou `./mvnw -B verify` (Linux).
Exigem Docker, JDK 25, Python e compilador C11. Logs locais ficam em `target/`.
As tentativas iniciais do teste preexistente foram bloqueadas pelo ambiente Docker;
isso nao constitui evidencia TDD vermelha de uma falha funcional.

## Validacao manual local — 30/10/2026

Imagem `bloodell-backend:jwt-card1` reconstruida com o codigo atual. Ambiente
Compose separado `bloodell-jwt-manual`, API em `http://127.0.0.1:18080`, banco e
volume proprios. A API anterior na porta 8080 e seu banco foram preservados.
Autenticacao habilitada, chave aleatoria externa ao Git e conta ADMIN sintetica
`admin.jwt@example.test`, criada pelo provisionamento inicial.

| Requisicao/cenario | Resultado |
|---|---|
| GET health sem token | 200 |
| POST login com credenciais corretas | 200; Bearer com validade de 900 segundos |
| GET me com Bearer valido | 200; identidade ADMIN esperada |
| GET me sem token | 401 |
| GET me com token invalido | 401 |
| POST login com senha incorreta | 401 |
| GET hemocentros sem token | 401 |
| GET hemocentros com Bearer valido | 200 |
| GET me com assinatura adulterada | 401 |
| GET me com JWT assinado mas expirado | 401 |
| GET OpenAPI sem token | 200; esquema Bearer disponivel |

Assinatura HS256 conferida localmente, assim como emissor, audiencia, subject e
intervalo de 900 segundos entre emissao e expiracao. Login sem cookie, com
`Cache-Control: no-store`, sem senha/hash na resposta. O caso expirado usa um JWT
deliberadamente expirado, assinado com a chave exclusiva do ambiente de teste.
O banco confirmou uma unica conta ADMIN com hash BCrypt de custo 12. Depois,
o provisionamento foi desligado, a senha de bootstrap foi removida do ambiente
do container e a API foi recriada. Os mesmos 11 cenarios passaram novamente,
confirmando a persistencia da conta sem depender do provisionamento a cada inicio.

Arquivos locais ignorados pelo Git em `target/jwt-manual/`: `compose.json`, `.env`,
`credenciais.json`, `verificar.ps1` e `resultado.json`. Senhas, chave e tokens nao
fazem parte desta evidencia. A senha sintetica para repetir pelo Swagger esta
somente em `credenciais.json`; o script HTTP tambem pode ser executado novamente:

```powershell
& .\target\jwt-manual\verificar.ps1
```

Esses arquivos sao temporarios: `mvnw clean` apaga `target/`. Nao os use para
guardar contas ou chaves de outro ambiente. Para parar somente este teste,
preservando seu volume:

```powershell
docker compose -p bloodell-jwt-manual --env-file target/jwt-manual/.env -f target/jwt-manual/compose.json stop
```

## Proximos cards

Ficam para os proximos cards: matriz de autorizacao/403 por perfil (Card 2), login
e Bearer no frontend (Card 3), identificacao do operador nas operacoes/auditoria
e preparacao do deploy com contas, chave, HTTPS e testes integrados. Perfis ainda
nao restringem operacoes e nao implementam isolamento por hospital/unidade.
Protecao contra tentativas repetidas de login tambem deve ser tratada antes de
uma exposicao real de contas, sem declarar este card uma entrega de seguranca completa.
