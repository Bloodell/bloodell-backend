# Versionamento da API

Toda a API vive sob `/api/v1`. Este documento existe para responder a uma
pergunta que o blueprint deixou em aberto: **o que acontece quando um contrato
precisa mudar no meio do semestre?**

O risco é concreto — o frontend consome a API real desde a primeira história.
Uma mudança silenciosa no backend quebra a tela de alguém na véspera da entrega.

## A regra, em uma frase

**Mudança que quebra contrato exige versão nova ou depreciação anunciada. Nunca
alteração silenciosa.**

## O que pode mudar sem virar versão (mudança compatível)

- acrescentar um **campo novo** na resposta;
- acrescentar um **parâmetro opcional** de filtro;
- acrescentar um **endpoint novo**;
- acrescentar um **valor novo** em um enum de resposta — desde que o frontend
  trate valor desconhecido sem quebrar (regra do time: trate);
- corrigir bug de comportamento que contrariava a documentação.

## O que quebra contrato (e exige processo)

- remover ou renomear campo de resposta;
- mudar o tipo de um campo (`string` → `number`, item → lista);
- tornar obrigatório um campo de entrada que era opcional;
- mudar o código HTTP de um caso já existente (por exemplo, 200 → 204);
- mudar o significado de um valor de enum;
- mudar a rota de um recurso.

## O processo para mudança que quebra

1. **Abrir issue** com o rótulo `contrato-api`, descrevendo o antes e o depois.
2. **Avisar quem consome** no canal do time, marcando a área de frontend. Isso é
   obrigatório mesmo quando "só uma tela usa".
3. Escolher um caminho:
   - **Caminho A — adicionar em vez de trocar.** Novo campo/endpoint convive com
     o antigo; o antigo vira `@Deprecated` com prazo (mínimo uma semana) e some
     em um pull request próprio. **É o caminho padrão.**
   - **Caminho B — `/api/v2`.** Só quando várias mudanças incompatíveis chegarem
     juntas e o caminho A ficar confuso. `/api/v1` continua respondendo até o fim
     do semestre.
4. **Atualizar** a tabela de endpoints do README e a anotação OpenAPI no mesmo
   pull request. Contrato mudado sem documentação atualizada não é aprovado.

## Prática que evita o problema antes dele existir

Antes de criar um endpoint novo, combine o formato com quem vai consumi-lo. Um
comentário no issue com o JSON de exemplo custa cinco minutos e evita a troca de
contrato na semana seguinte.

## Depreciação: como sinalizar

Enquanto um campo estiver depreciado, a resposta traz o cabeçalho:

```
Deprecation: true
Sunset: <data em que sai do ar>
Link: <https://github.com/.../issues/NN>; rel="deprecation"
```

E o campo aparece marcado no Swagger, com a data de remoção.

## O que não conta como versão

Mudar mensagem de erro, ajustar log, mexer em índice do banco, refatorar por
dentro sem alterar entrada nem saída: nada disso é mudança de contrato.
