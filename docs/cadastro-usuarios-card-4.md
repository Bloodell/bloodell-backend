# Card 4 — Cadastro de usuarios pelo administrador

Implementacao local em `feature/cadastro-usuarios`, sobre `3f21837`, que integra
o login JWT na `dev`. A base foi conferida no GitHub antes da implementacao.

## Revisao previa

- Reutilizados `Usuario`, `Perfil`, o `PasswordEncoder` BCrypt de custo 12 e a
  identidade JWT existente. Nao foi criada outra tabela nem outra autenticacao.
- V1 ja contem `usuarios`; V3 ja garante e-mail unico ignorando caixa e espacos.
  Nao foi necessaria uma migration adicional.
- A identidade JWT ja reconsulta o banco: a permissao ADMIN usa o perfil atual
  da conta, em vez de confiar na alegacao `perfil` de um token antigo.
- As rotas gerais anteriores exigiam apenas autenticacao. Este card acrescenta
  restricao ADMIN exclusivamente para `/api/v1/usuarios` e seus subcaminhos.
- O modo demonstrativo continua disponivel nas rotas anteriores, mas nunca
  libera anonimamente a gestao de usuarios.

## Contrato

POST `/api/v1/usuarios`, com `Authorization: Bearer <token-do-admin>`:

```json
{
  "nome": "Integrante sintetico",
  "email": "integrante@example.test",
  "perfil": "OPERADOR_HEMOCENTRO",
  "senha": "<senha-inicial-definida-pelo-admin>"
}
```

Substitua o marcador por uma senha inicial propria. Nao ha senha padrao.

Resposta 201: `{id, nome, email, perfil}`, sem senha/hash/token, com
`Cache-Control: no-store`. A conta criada entra pelo mesmo POST
`/api/v1/auth/login` e consulta GET `/api/v1/auth/me`; o cadastro nao autentica
automaticamente a nova conta nem substitui a sessao do administrador.

| Campo | Validacao |
|---|---|
| nome | Obrigatorio, nao vazio apos trim, ate 120 caracteres. |
| email | Obrigatorio, formato valido, ate 160 caracteres; trim e minusculas antes de validar/gravar. |
| perfil | Obrigatorio: ADMIN, OPERADOR_HEMOCENTRO, USUARIO_HOSPITAL ou LOGISTICA. |
| senha | Obrigatoria, nao pode conter so espacos, de 12 caracteres ate 72 bytes UTF-8; nunca e truncada ou aparada. |

Um ADMIN pode cadastrar qualquer um dos quatro perfis, inclusive outro ADMIN.
A senha e armazenada com o BCrypt ja configurado, custo 12. O DTO mascara
`toString()` e marca a senha como somente escrita no JSON e no OpenAPI.

| Status | Situacao |
|---|---|
| 201 | Conta cadastrada. |
| 400 | Campos invalidos, perfil desconhecido, senha fora dos limites ou JSON ilegivel. |
| 401 | Token ausente, invalido ou expirado; tambem bloqueia cadastro no modo demonstrativo. |
| 403 | Conta autenticada sem perfil ADMIN. |
| 409 | E-mail ja cadastrado, inclusive variacoes de caixa/espacos. |

Erros seguem `ProblemaApi`, com correlacao e sem repetir senha/hash. A resposta
403 da cadeia de seguranca agora tambem usa esse envelope.

## Duplicidade e concorrencia

O cadastro usa uma insercao atomica em PostgreSQL, por `JdbcTemplate`, na mesma
tabela da entidade JPA. `ON CONFLICT (lower(btrim(email))) DO NOTHING RETURNING id`
reutiliza o indice V3: em duas tentativas simultaneas para o mesmo e-mail, uma
cria a conta e a outra retorna 409.

Essa escolha evita depender de uma consulta previa seguida de INSERT, que teria
uma janela de corrida. Tambem evita provocar uma excecao de unicidade cujo DETAIL
do PostgreSQL/Hibernate poderia acabar registrando a linha com o hash da senha.
O repositorio JPA existente continua consultando as contas para login e JWT.

## Como testar

1. Inicie a versao atual do backend com `BLOODELL_AUTH_HABILITADA=true` e a chave
   JWT configurada fora do Git, conforme `autenticacao-jwt-card-1.md`.
2. Use o administrador ja provisionado ou o mecanismo de primeira conta em banco
   vazio. Nao ha endpoint publico para criar o primeiro administrador.
3. No Swagger (`/swagger-ui.html`), execute POST `/api/v1/auth/login` com essa
   conta; copie `accessToken` para Authorize (somente o token, sem prefixo Bearer).
4. Execute POST `/api/v1/usuarios` e confira o 201 e os quatro campos de resposta.
5. Faca login com a nova conta e consulte `/api/v1/auth/me`.
6. Repita o cadastro com o mesmo e-mail: deve retornar 409.
7. Com uma conta nao ADMIN, tente cadastrar: deve retornar 403. Sem token, 401.

Recrie a imagem/API local para testar codigo novo via Docker. Containers que ja
estavam rodando continuam com a imagem anterior; este card nao faz deploy nem
substitui automaticamente ambientes existentes.

## Verificacao automatizada

`CadastroDeUsuarioIT` usa HTTP real e PostgreSQL/Testcontainers. Cobre o fluxo
cadastro/login/me, os quatro perfis cadastraveis, negacao dos outros perfis mesmo
com alegacao ADMIN no token, alteracao do perfil no banco, token ausente/invalido/
expirado, validacoes e campos nulos, limites de senha ASCII e multibyte, unicidade,
concorrencia, ausencia de senha/hash em respostas/logs e o contrato do Swagger.

`AutenticacaoDesabilitadaIT` cobre o bloqueio do cadastro no modo demonstrativo.
`UsuarioDtosTest` verifica que senha nao aparece em `toString()` nem no JSON.

O teste inicial, antes do endpoint, falhou ao esperar 201 (recebia 500 da rota
inexistente). Evidencia local: `target/cadastro-red.log`. Os primeiros 17 testes
focados passaram em `target/cadastro-focused.log`; depois foram acrescentados
os casos de Swagger, senha no tamanho minimo e serializacao para a suite completa.

```powershell
.\mvnw.cmd -B verify
docker build -t bloodell-backend:cadastro-card4 .
```

O verify exige Docker, JDK 25, Python, make e compilador C11. Logs locais da
verificacao completa e do build ficam em `target/cadastro-verify.log` e
`target/cadastro-docker-build.log`, ignorados pelo Git. O comando de paridade pode
marcar fixtures U2 como pendentes conforme a etapa vigente; isso nao as implementa.

Resultado final em 30/10/2026: `mvnw -B verify` aprovado, com 87 testes unitarios
e 62 testes de integracao/aceitacao (149 no total), sem falhas ou testes pulados.
Testes C da AED U1 aprovados e quatro fixtures com saida identica em C/Java.
Imagem `bloodell-backend:cadastro-card4` construida com sucesso. Nenhum container
existente foi substituido e nenhum deploy foi realizado.

## Limites

Este card implementa somente criacao administrativa de contas. Listagem/consulta
(Card 5), atualizacao (Card 6), exclusao, desativacao, troca/recuperacao de senha,
envio de e-mail, obrigacao de trocar a senha inicial e vinculo com hospital ou
hemocentro ficam para outras entregas. O frontend nao foi alterado.

A matriz de permissoes das demais rotas continua no Card 2. Cadastro de conta e
atribuicao de perfil nao implementam isolamento de dados entre instituicoes.

A consulta administrativa foi acrescentada na entrega seguinte:
[`consulta-usuarios-card-5.md`](consulta-usuarios-card-5.md).
