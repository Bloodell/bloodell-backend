# Entrega 04 — Rota Vital / Bloodell

Período coberto: 23/10/2026 a 09/11/2026 (após a Entrega 03).
Repositórios: [bloodell-backend](https://github.com/Bloodell/bloodell-backend) ·
[bloodell-frontend](https://github.com/Bloodell/bloodell-frontend).

## Histórias implementadas

US-E4-01 a 04 estão no backend, integradas em `dev` (ainda não promovidas para
`main`/EC2), com testes de integração (Testcontainers + PostgreSQL) e documento
de contrato em `docs/`. US-E4-05 está no frontend, já em `main`.

### US-E4-01 — Login com JWT (Card 1)

```
┌──────────────────────────────────────────────────────────┐
│ US-E4-01 · Entrar no sistema                             │
│                                                          │
│ Como integrante da equipe do hemocentro/hospital,        │
│ quero entrar com e-mail e senha e receber um token,      │
│ para acessar a API de forma identificada e segura.       │
│                                                          │
│ CA1 POST /api/v1/auth/login devolve JWT com validade     │
│ CA2 E-mail comparado sem distinguir caixa; senha BCrypt  │
│ CA3 Credencial inválida → 401, sem revelar o motivo      │
│ CA4 GET /api/v1/auth/me devolve o perfil atual do banco  │
│ CA5 Com auth ligada, rotas de negócio exigem Bearer      │
│                                                          │
│ PR #36 · commit 49410f1 · docs/autenticacao-jwt-card-1.md│
└──────────────────────────────────────────────────────────┘
```

### US-E4-02 — Cadastro de usuários pelo administrador (Card 4)

```
┌──────────────────────────────────────────────────────────┐
│ US-E4-02 · Cadastrar integrantes                         │
│                                                          │
│ Como administrador, quero cadastrar integrantes com      │
│ nome, e-mail, perfil e senha inicial, para que cada      │
│ pessoa tenha sua própria conta de acesso.                │
│                                                          │
│ CA1 POST /api/v1/usuarios (só ADMIN) → 201 sem senha/hash│
│ CA2 E-mail duplicado → 409                               │
│ CA3 Entrada inválida → 400; sem token → 401; sem perfil  │
│     ADMIN → 403                                          │
│ CA4 Conta criada já consegue fazer login                 │
│                                                          │
│ PR #38 · commit 5be0843 · docs/cadastro-usuarios-card-4.md│
└──────────────────────────────────────────────────────────┘
```

### US-E4-03 — Consulta de usuários (Card 5)

```
┌──────────────────────────────────────────────────────────┐
│ US-E4-03 · Consultar contas                              │
│                                                          │
│ Como administrador, quero listar e consultar contas,     │
│ para saber quem tem acesso e com qual perfil.            │
│                                                          │
│ CA1 GET /api/v1/usuarios paginado (máx. 100, ID crescente)│
│ CA2 GET /api/v1/usuarios/{id}; inexistente → 404         │
│ CA3 Retorna só ID, nome, e-mail e perfil                 │
│ CA4 Parâmetros inválidos → 400; não-ADMIN → 403          │
│                                                          │
│ PR #38 · docs/consulta-usuarios-card-5.md                │
└──────────────────────────────────────────────────────────┘
```

### US-E4-04 — Atualização de usuários (Card 6)

```
┌──────────────────────────────────────────────────────────┐
│ US-E4-04 · Atualizar contas                              │
│                                                          │
│ Como administrador, quero alterar nome, e-mail e perfil  │
│ de uma conta, para manter os acessos corretos.           │
│                                                          │
│ CA1 PATCH /api/v1/usuarios/{id}; campos omitidos mantidos│
│ CA2 Senha e ID não podem ser alterados                   │
│ CA3 E-mail duplicado → 409                               │
│ CA4 Não é possível rebaixar o último ADMIN (409), mesmo  │
│     com atualizações simultâneas                         │
│ CA5 Login aceita o novo e-mail; /auth/me reflete o perfil│
│                                                          │
│ PR #38 · docs/atualizacao-usuarios-card-6.md             │
└──────────────────────────────────────────────────────────┘
```

### US-E4-05 — Central logística com mapa de rastreamento (frontend)

```
┌──────────────────────────────────────────────────────────┐
│ US-E4-05 · Acompanhar entregas no mapa                   │
│                                                          │
│ Como operador de logística, quero ver no mapa os         │
│ veículos e entregas em andamento, para acompanhar o      │
│ transporte dos hemocomponentes.                          │
│                                                          │
│ CA1 Rota /rastreamento ("Central logística") com mapa    │
│     MapLibre e caminhões em 3D                           │
│ CA2 Entregas, unidades, requisições e veículos vêm da    │
│     API (useRecurso: carregando, erro e vazio)           │
│ CA3 Trajeto por estradas; sem serviço de rota, usa curva │
│     estimada                                             │
│ CA4 Posição pela telemetria quando existe; senão,        │
│     estimada pelo tempo previsto (fonte indicada)        │
│ CA5 Identidade visual nova e página /design com o guia   │
│                                                          │
│ frontend PR #2 · commit 407bb23                          │
└──────────────────────────────────────────────────────────┘
```

> Limite: o frontend ainda não tem tela de login; a integração com o JWT
> (Card 3) segue pendente (issue #34).

### Endurecimento — S-01: JWT obrigatório em produção

PR #39 (commits `6f378bd`, `d0a15fc`, `66d08c1`): o perfil `prod` passa a
exigir autenticação JWT (antes, o modo demonstrativo deixava a API aberta).
Teste: `AutenticacaoProducaoIT`; testes de negócio isolam a autenticação.

## Versionamento (commits e PRs da entrega)

| Data | Commit | Autor | O que mudou |
|---|---|---|---|
| 30/10 | `49410f1` | arthurabelardo-dev | Login e autenticação JWT |
| 30/10 | `3f21837` | Carlos Henrique | Merge PR #36 (`feature/autenticacao-jwt`) |
| 31/10 | `5be0843` | arthurabelardo-dev | Gestão administrativa de usuários (Cards 4, 5, 6) |
| 05/11 | `3932f81` | Carlos Henrique | Merge PR #38 (`feature/cadastro-usuarios`) |
| 06/11 | `6f378bd` | chdevbr | Exigir JWT no perfil de produção |
| 06/11 | `d0a15fc` | chdevbr | Segredo JWT no teste de produção |
| 06/11 | `66d08c1` | chdevbr | Isolar autenticação dos testes de negócio |
| 06/11 | `2450c51` | Arthur Abelardo | Merge PR #39 (`fix/s-01-jwt-producao`) |
| 30/10 | `e46f5ba` | chdevbr | Frontend: checkpoint de base (CI, README, configs) |
| 04/11 | `407bb23` | chdevbr | Frontend: nova identidade visual, componentes e Central logística (mapa) |
| 06/11 | `2efc5e9` | Carlos Henrique | Frontend: merge PR #2 (`feat/redesign-visual-componentes`) |

Fluxo: branch de feature → PR → revisão por outro membro → merge. Deploy
automático na EC2 apenas em `main`.

## Issue/bug tracker (GitHub)

Issues em <https://github.com/Bloodell/bloodell-backend/issues> (10 abertas,
com rótulos `bug`, `enhancement`, `tech-debt`, `ci`, `U2`) e template de bug
report (PR #25). Exemplos: #26 (bug: alocação parcial marca requisição como
ALOCADA), #34 (autenticação e perfis de acesso), #32/#33 (FEFO e ABO/Rh, U2).

**Print da tela:** salvar a captura da página de issues em
`docs/evidencias/e4/issues.png` (e, se possível, a de um PR fechando uma
issue) e referenciá-la no README:
`![Issues do GitHub](docs/evidencias/e4/issues.png)`.

## Screencasts (YouTube)

- Uso do sistema, com ênfase nas novas histórias: _colar link_
- Explicação do código das histórias: _colar link_

Roteiros em [entrega-04-roteiros.md](entrega-04-roteiros.md).
