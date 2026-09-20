# Concorrência — onde, por quê e como está testada

A disciplina de Infraestrutura de Software pede threads **com necessidade real**,
ganho demonstrado em números e ausência de condição de corrida evidenciada.
Concorrência colocada só para cumprir requisito atrapalha mais do que ajuda, e o
próprio blueprint lista isso como risco.

São três pontos, e só três.

## 1. Ingestão de telemetria (Unidade 1) — implementado

**Problema real:** vários veículos publicam posição e temperatura ao mesmo
tempo, a cada poucos segundos. Se a API gravasse cada leitura na hora, ficaria
presa esperando o banco e o agente simulador acumularia atraso.

**Solução:** produtor/consumidor.

```
POST /api/v1/telemetrias
   -> valida a entrada
   -> coloca na FilaDeIngestao (ArrayBlockingQueue limitada)
   -> responde 202 Accepted

ProcessadorDeTelemetria (N workers)
   -> tira um lote da fila
   -> GravadorDeLeituras grava o lote em UMA transação
   -> abre alerta se a temperatura saiu da faixa didática
```

**Por que responde 202 e não 201:** no momento da resposta a linha ainda não
existe no banco. Dizer "created" seria mentira.

**Por que a fila é limitada:** fila infinita só troca "recusar rápido" por
"cair depois sem memória". Cheia, a API responde 503 com `Retry-After`.

**Por que `AtomicLong` nas métricas:** vários workers incrementam o mesmo
contador. Com `long` comum, duas somas simultâneas se perdem e o número do
benchmark sai errado.

**Por que `ArrayBlockingQueue` é permitida aqui:** ela é infraestrutura de
concorrência, não estrutura de dados acadêmica. A estrutura avaliada é a
`FilaRequisicoes`, escrita à mão em `aed/u1`. As duas não se substituem.

**Evidência:** `IngestaoDeTelemetriaIT` — 8 produtores × 50 leituras; confere
que as 400 chegaram ao banco e imprime msg/s para o relatório.

**Medição sequencial × concorrente:** rodar o mesmo teste com
`bloodell.telemetria.quantidade-workers=1` e comparar com 4. Os dois números
entram no relatório de SO. `GET /api/v1/telemetrias/metricas` mostra os
contadores ao vivo durante a demo.

## 2. Reserva de bolsa (Unidade 2) — proteção já implementada

**Problema real:** dois operadores processam requisições ao mesmo tempo e o
algoritmo escolhe a mesma bolsa para as duas. Sem proteção, a bolsa é "entregue"
duas vezes — o pior erro possível neste domínio.

**Solução:** `@Version` na entidade `Bolsa` (bloqueio otimista). Quem grava
primeiro vence; o segundo recebe `OptimisticLockingFailureException`, que o
manipulador global traduz em **409 Conflict** com orientação para repetir.

**Por que otimista e não pessimista:** conflito real é raro. Travar a linha em
toda leitura penalizaria todas as alocações para proteger um caso incomum.

**Evidência:** `ConcorrenciaNaAlocacaoIT` — duas threads, uma única bolsa AB-,
exatamente um sucesso e uma falha.

**Como mostrar a corrida acontecendo:** remover `@Version` de `Bolsa` e rodar o
teste de novo. As duas alocações passam e a mesma bolsa é entregue duas vezes.
Vale gravar essa tela: é a evidência mais direta de "corrida demonstrada e
corrigida".

## 3. Fila de requisições com vários operadores (Unidade 2) — pendente

**Problema:** dois operadores processando a fila podem pegar a mesma requisição.

**Solução prevista:** a requisição muda para `EM_PROCESSAMENTO` na mesma
transação em que sai da fila; o segundo operador encontra um estado que não
permite processar e recebe 422. A transição de estado já está implementada em
`StatusRequisicao`; falta o teste de dois consumidores simultâneos.

## O que não usamos, e por quê

| Alternativa | Por que não agora |
|---|---|
| RabbitMQ / Kafka | fila em memória + workers resolve o volume do projeto; um broker adicionaria um componente que teria de existir no diagrama, no Compose e no deploy |
| Redis | não resolve nenhum problema atual e não pode substituir a tabela hash acadêmica |
| WebSocket | polling de 3 a 5 s atende a meta de alerta em até 10 s; medir antes de trocar |

Cada uma volta à mesa se a medição mostrar necessidade — não por preferência.
