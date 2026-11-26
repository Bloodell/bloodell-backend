# ADR 0002: Comunicação Híbrida e Saga Orquestrada na Projeção Distribuída
Status: proposto

## Contexto
Na projeção de microsserviços do Rota Vital com um banco de dados por serviço, o fluxo de alocação de hemocomponentes atravessa `Alocação`, `Estoque`, `Doação e Bolsa`, `Requisição` e `Rastreabilidade`. Sem uma transação ACID única entre bancos distintos, o sistema fica sujeito a reservas duplicadas sob concorrência, falhas parciais no meio do processo e dessincronização entre o estoque projetado e o estado real das bolsas.

## Decisão
Adotar comunicação híbrida — REST síncrono com idempotência para validação e reserva imediata de bolsas, eventos de domínio via broker com Transactional Outbox para projeções e auditoria, e fila dedicada para telemetria — coordenando o fluxo distribuído de alocação por meio de uma Saga Orquestrada pelo `servico-alocacao` com ações explícitas de compensação e TTL de reserva.

## Consequências
Melhora a rastreabilidade e o controle centralizado do processo Core, elimina bloqueios distribuídos de longa duração, desacopla consultas de estoque e auditoria do caminho crítico e assegura recuperação consistente em caso de falhas parciais. Piora a complexidade de implementação ao introduzir consistência eventual nas telas de estoque e rastreabilidade, exigindo chaves de idempotência, manutenção da tabela Outbox e tratamento de retries e Dead-Letter Queues (DLQ). Alternativas descartadas: transações distribuídas Two-Phase Commit (2PC), descartadas por degradarem disponibilidade e escalabilidade sob latência de rede; e Saga puramente coreografada por eventos, descartada por dispersar a lógica do núcleo de alocação e dificultar o diagnóstico de falhas em cadeia.