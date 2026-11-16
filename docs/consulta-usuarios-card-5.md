# Card 5 — Consulta administrativa de usuarios

Implementacao local na `feature/cadastro-usuarios`, sobre o Card 4 ainda sem
commit. Os dois cards permanecem juntos na branch, preservando o cadastro e o
login existentes. A base `origin/dev` foi conferida antes de continuar.

## Endpoints

Ambos exigem JWT valido de uma conta com perfil ADMIN atual no banco.

| Endpoint | Resposta |
|---|---|
| GET `/api/v1/usuarios` | 200, pagina de contas com metadados. |
| GET `/api/v1/usuarios/{id}` | 200, dados da conta; 404 se nao existir. |

Cada conta contem somente `id`, `nome`, `email` e `perfil`. Entidades JPA nao sao
serializadas diretamente: as consultas reutilizam o DTO publico do usuario.
Senha e hash nao aparecem no conteudo nem nos metadados. Respostas de sucesso
usam `Cache-Control: no-store`.

Todas as contas sao consultaveis pelo ADMIN, inclusive administradores e contas
legadas sem hash valido. Isso nao habilita login para contas legadas; a regra
anterior que exige BCrypt valido continua valendo.

## Paginacao

O contrato segue as consultas paginadas ja existentes no projeto:

- `page`: indice da pagina, a partir de zero; padrao 0. Negativos viram 0.
- `size`: quantidade por pagina; padrao 20, normalizada entre 1 e 100.
- Ordenacao fixa por `id` crescente, com desempate natural pela chave unica.
  Nao ha parametro de ordenacao livre nem busca/filtros neste card.
- Metadados incluem `content`, `number`, `size`, `totalElements`, `totalPages` e
  `numberOfElements`, no envelope `Page` usado pela API.
- Pagina alem do fim retorna 200 com `content: []`, preservando os totais.
- Formato numerico invalido ou valor que nao cabe no tipo esperado retorna 400.
  Paginacao cujo deslocamento excede o limite inteiro do JPA tambem retorna 400.

Exemplos:

```text
GET /api/v1/usuarios?page=0&size=20
GET /api/v1/usuarios?page=1&size=20
GET /api/v1/usuarios/123
```

A lista inclui o proprio administrador. Num banco autenticavel ha pelo menos essa
conta; o caso de repositorio totalmente vazio e coberto no teste unitario do
servico. O caso HTTP de pagina vazia e exercitado com uma pagina alem do fim.

## Erros e permissoes

- 400: parametros numericos invalidos ou paginacao extrema.
- 401: token ausente, invalido ou expirado; tambem bloqueia consultas no modo
  demonstrativo, quando a autenticacao esta desligada.
- 403: conta autenticada sem perfil ADMIN, independentemente do ID consultado.
- 404: conta inexistente, apos autenticar e autorizar o administrador.

Erros usam `ProblemaApi`. Foi acrescentado tratamento de conversao de parametros
ao manipulador compartilhado para evitar 500 em entradas como `page=abc` ou
`/usuarios/abc`. O detalhe informa o parametro invalido sem copiar seu valor.
Esse tratamento tambem se aplica aos demais endpoints com parametros tipados.

A restricao de acesso reutiliza a regra `/api/v1/usuarios/**` do Card 4. O perfil
usado e o que esta no banco, sem confiar apenas na alegacao `perfil` do JWT. Nao
foi acrescentada a matriz de permissoes das demais operacoes (Card 2).

## Verificacao e roteiro manual

Com a versao atual do backend iniciada e autenticacao habilitada:

1. Faca login como ADMIN e use o token no Authorize do Swagger.
2. Cadastre contas pelo POST `/api/v1/usuarios` do Card 4.
3. Consulte a lista com `size=2`, navegando por `page=0`, `page=1` etc.
4. Confira os totais, a ordem dos IDs e a ausencia de senha/hash.
5. Consulte um ID retornado pela lista e um ID que nao exista (404).
6. Use uma conta de outro perfil: ambas as consultas devem retornar 403.
7. Sem token, espere 401. Com `page=abc`, autenticado como ADMIN, espere 400.

O Swagger documenta Bearer, parametros, respostas e erros dos dois GETs.
Containers existentes nao sao atualizados automaticamente; recrie a API local
se quiser testar o codigo novo via Docker.

`ConsultaDeUsuarioIT` utiliza HTTP e PostgreSQL reais: projecao sem credenciais,
paginacao com nomes repetidos, limite de 100, normalizacao de parametros, pagina
vazia, consulta por ID/conta legada, 404, parametros invalidos, todos os perfis
nao ADMIN, tokens ausentes/invalidos/expirados e Swagger.
`AutenticacaoDesabilitadaIT` cobre o bloqueio das duas consultas no modo demo.
`ConsultaDeUsuarioServiceTest` cobre repositorio sem contas.

```powershell
.\mvnw.cmd -B verify
docker build -t bloodell-backend:consulta-card5 .
```

O verify exige Docker, JDK 25, Python, make e compilador C11. Logs ignorados pelo
Git em `target/`: `consulta-red.log`, `consulta-focused.log`, `consulta-verify.log`
e `consulta-docker-build.log`. O primeiro teste de listagem falhou antes da
implementacao ao esperar 200 da rota GET ainda ausente (recebia 500).
Os 25 testes focados de consulta/modo demonstrativo passaram em
`target/consulta-focused.log` antes da verificacao completa de regressao.

Resultado final em 30/10/2026: `mvnw -B verify` aprovado, com 88 testes unitarios
e 85 de integracao/aceitacao (173 no total), sem falhas ou testes pulados.
Testes C da AED U1 aprovados e quatro fixtures identicas entre C/Java.
Imagem `bloodell-backend:consulta-card5` construida com sucesso. Os containers
existentes foram preservados; nao houve commit, push ou deploy nesta etapa.

## Limites

Somente consultas administrativas neste card. Atualizacao de usuarios (Card 6),
busca avancada, exportacao, exclusao, desativacao, troca de senha e telas do
frontend ficam para outras entregas. Nao foi necessaria migration ou alteracao
da configuracao JWT.

A atualizacao foi acrescentada na entrega seguinte:
[`atualizacao-usuarios-card-6.md`](atualizacao-usuarios-card-6.md).
