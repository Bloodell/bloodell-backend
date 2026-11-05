# Card 6 — Atualizacao administrativa de usuarios

Implementacao local na `feature/cadastro-usuarios`, sobre os Cards 4 e 5 ainda
sem commit. Base `origin/dev` conferida antes de continuar. A tabela, o login JWT
e o BCrypt existentes foram reutilizados; nao ha nova migration.

## Contrato

PATCH `/api/v1/usuarios/{id}`, com JWT de ADMIN:

```json
{
  "nome": "Nome corrigido",
  "email": "novo@example.test",
  "perfil": "USUARIO_HOSPITAL"
}
```

Os campos sao opcionais, mas pelo menos um deve ser informado. Somente `nome`,
`email` e `perfil` sao aceitos. Campos omitidos permanecem iguais. `null`
explicito, objeto vazio e campos adicionais (inclusive `senha`, `senha_hash` e
`id`) retornam 400; nao existe remocao de um campo por null neste contrato.

- Nome: trim, nao vazio, ate 120 caracteres.
- E-mail: trim e minusculas, formato valido, nao vazio, ate 160 caracteres.
- Perfil: ADMIN, OPERADOR_HEMOCENTRO, USUARIO_HOSPITAL ou LOGISTICA.
- O proprio e-mail pode ser mantido; o de outra conta retorna 409.

Resposta 200: `{id, nome, email, perfil}`, com `Cache-Control: no-store`.
ID, senha/hash e data de criacao nao sao alterados. Nenhuma resposta expoe senha
ou hash. E possivel corrigir os dados de uma conta legada sem criar senha para
ela; isso nao habilita seu login.

| Status | Situacao |
|---|---|
| 200 | Dados atualizados. |
| 400 | Campos/JSON/ID invalidos, campos extras, null explicito ou objeto vazio. |
| 401 | Token ausente, invalido ou expirado; tambem bloqueia PATCH no modo demonstrativo. |
| 403 | Solicitante nao e ADMIN, inclusive se perdeu a permissao enquanto aguardava a escrita. |
| 404 | Conta nao encontrada, apos autenticar/autorizar o solicitante. |
| 409 | E-mail de outra conta ou tentativa de rebaixar o ultimo administrador que pode entrar. |

Erros seguem `ProblemaApi`. O manipulador compartilhado passa a tratar
`AccessDeniedException` explicitamente como 403 tambem dentro do servico, sem
transformar uma permissao perdida durante a espera em erro interno.

## Efeito no login e no perfil

Alterar o e-mail permite entrar com o novo endereco e impede o login dessa conta
com o antigo. A senha continua a mesma. Tokens existentes continuam identificando
a conta por ID, dentro de sua validade; `/api/v1/auth/me` retorna os dados atuais.

A autorizacao usa o perfil atual no banco. Uma conta promovida a ADMIN ganha
acesso administrativo com seu token valido; uma conta rebaixada perde esse
acesso, mesmo se o JWT tiver a alegacao ADMIN. O cadastro nao troca a chave nem
exige reemissao de todos os tokens. O frontend pode atualizar a identidade por
`/auth/me`; este card nao acrescenta telas ou sincronizacao visual automatica.

## Ultimo administrador e concorrencia

Um ADMIN pode alterar seu proprio cadastro e, havendo outro administrador capaz
de entrar, remover seu proprio perfil ADMIN. Nunca pode rebaixar o ultimo ADMIN
com hash BCrypt valido. Contas legadas ADMIN com hash nulo/invalido nao contam
como substitutas para essa regra.

O PATCH usa uma transacao e `LOCK TABLE usuarios IN SHARE ROW EXCLUSIVE MODE`
antes de ler as contas. Esse bloqueio serializa atualizacoes e coordena com os
INSERTs do Card 4, sem impedir SELECTs comuns. Com o bloqueio adquirido:

1. Reconsulta o solicitante e confirma o perfil ADMIN atual.
2. Busca o alvo e verifica e-mail duplicado e a preservacao do ultimo ADMIN.
3. Aplica os campos enviados e grava; qualquer falha desfaz a atualizacao toda.

Assim, dois administradores tentando rebaixar a si mesmos simultaneamente
produzem 200 e 409, preservando um ADMIN. Atualizacoes concorrentes do mesmo
e-mail tambem produzem 200 e 409. Cadastro e atualizacao disputando um e-mail
deixam somente uma conta com ele, sem provocar violacao SQL que registre o hash.

Se o solicitante perder o perfil enquanto aguarda o bloqueio, recebe 403 e nao
altera o alvo. A permissao e reavaliada apos a espera, alem da verificacao JWT
inicial. O bloqueio abrange a pequena tabela administrativa durante o PATCH;
escritas concorrentes aguardam a transacao terminar. Regras de negocio desta API
nao substituem os cuidados de um administrador que execute SQL diretamente.

## Como testar

Com a versao atual do backend iniciada e JWT habilitado:

1. Entre como ADMIN e use o token no Authorize do Swagger.
2. Cadastre uma conta pelo Card 4 e consulte seu ID pelo Card 5.
3. Execute PATCH enviando apenas `nome`: e-mail, perfil e senha permanecem iguais.
4. Altere o e-mail e faca login com o novo endereco, usando a mesma senha.
5. Consulte `/auth/me` e confira os dados atualizados.
6. Tente usar e-mail de outra conta: espere 409 sem alteracao parcial.
7. Tente rebaixar o unico ADMIN: espere 409. Com outro ADMIN valido, o rebaixamento
   e permitido; a conta rebaixada nao pode mais usar as rotas administrativas.
8. Envie campo `senha`, objeto vazio ou `null`: espere 400.

O Swagger documenta os tres campos e os seis status. Containers existentes
continuam com a imagem anterior ate serem recriados; nenhum ambiente e
substituido automaticamente por esta implementacao.

## Verificacao automatizada

`AtualizacaoDeUsuarioIT` usa HTTP real (PATCH pelo cliente HTTP do JDK) e
PostgreSQL/Testcontainers. Cobre atualizacao completa/parcial, ID/hash preservados,
proprio e-mail, login com novo e-mail e rejeicao do antigo, `/me` e permissoes de
tokens anteriores, conta legada, ultimo ADMIN, entradas invalidas, credenciais
em respostas/logs, 401/403/404/409 e contrato Swagger.

Os cenarios concorrentes incluem dois rebaixamentos, disputa de e-mail entre
PATCHs e entre POST/PATCH, e uma requisicao que perde permissao enquanto espera
uma escrita. Este ultimo caso mantem um bloqueio numa transacao de teste e
observa a espera em `pg_stat_activity` antes de alterar o perfil do solicitante.
`AutenticacaoDesabilitadaIT` confirma o bloqueio do PATCH no modo demonstrativo.

O teste inicial falhou antes da implementacao ao esperar 200 do PATCH ainda
ausente (recebia 500): `target/atualizacao-red.log`. Os primeiros 33 testes
focados passaram em `target/atualizacao-focused.log`. O caso adicional de disputa
POST/PATCH foi incluido na verificacao completa.

```powershell
.\mvnw.cmd -B verify
docker build -t bloodell-backend:usuarios-card6 .
```

Exigem Docker, JDK 25, Python, make e compilador C11. Logs ignorados pelo Git em
`target/atualizacao-verify.log` e `target/atualizacao-docker-build.log`.

Resultado final em 30/10/2026: `mvnw -B verify` aprovado, com 88 testes unitarios
e 116 de integracao/aceitacao (204 no total), sem falhas ou testes pulados.
Testes C da AED U1 aprovados e quatro fixtures com saida identica em C/Java.
Imagem `bloodell-backend:usuarios-card6` construida com sucesso. Os Cards 4, 5 e
6 permanecem locais, sem commit, push ou deploy; containers existentes preservados.

## Limites

Este e o ultimo dos tres cards de cadastro/consulta/atualizacao do backend.
Exclusao, desativacao, troca/recuperacao de senha, cadastro publico, envio de
e-mail e telas administrativas nao fazem parte desta entrega. A matriz de
permissoes das demais operacoes segue no Card 2, e isolamento por instituicao
continua exigindo regras e vinculos adicionais.
