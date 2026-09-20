# ADR-004 — Autenticação fora do MVP, com a porta pronta

**Data:** 19/09/2026 · **Status:** aceito · **Decidem:** equipe

## Contexto

O blueprint coloca login e perfis **fora do MVP** (pp.7 e 21), mas lista a
entidade `Usuario` com quatro perfis. Existe um risco dos dois lados: implementar
login agora é fazer o que o escopo não pediu; não preparar nada é descobrir na
semana 13 que ligar autenticação obriga a mexer em todos os controllers.

## Decisão

1. **Não** existe login, token, tela de acesso nem verificação de perfil.
2. A tabela `usuarios` é criada pela migration (ela está no schema do
   blueprint), mas **não** há entidade JPA, service nem endpoint para ela.
3. Existe um filtro, `PontoDeExtensaoDeAutenticacao`, já posicionado na cadeia,
   que hoje apenas deixa a requisição passar.
4. A chave `bloodell.seguranca.autenticacao-habilitada` existe e vale `false`.
   Ligada sem implementação, ela **registra um aviso no log** em vez de fingir
   que a API está protegida.

## Como ligar depois (mudança explícita de escopo)

1. implementar `autenticar()` lendo o cabeçalho `Authorization` — proposta: JWT
   simples assinado pela própria API, sem servidor de identidade externo;
2. mapear perfil → rotas sensíveis (alocar bolsa, despachar entrega, criar
   cenário de demonstração);
3. ligar a chave por ambiente.

Nenhum controller muda.

## O que dispara revisão

Pedido explícito de professor ou decisão da equipe registrada em ata. Exposição
pública da API na internet também muda a conversa: enquanto o deploy é
demonstrativo e os dados são sintéticos, o risco é baixo — com uma URL pública
divulgada, a proteção das rotas de escrita passa a ser necessária.
