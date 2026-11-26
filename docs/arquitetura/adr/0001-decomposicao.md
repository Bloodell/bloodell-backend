# ADR 0001: Decomposição do Rota Vital
Status: proposto

## Contexto
O Bloodell (Rota Vital) é desenvolvido por um squad de quatro integrantes sob prazo acadêmico curto (Unidade 2 / Parte 3), operando com dados 100% sintéticos, carga inicial baixa/moderada e infraestrutura enxuta em uma instância EC2 com PostgreSQL 17. Ao mesmo tempo, o domínio de alocação de hemocomponentes exige consistência rigorosa (invariantes INV-01 a INV-05) e o sistema precisa estar estruturado para crescer caso a ingestão de telemetria ou o número de times aumente.

## Decisão
Manter o backend como monólito modular organizado internamente pelos seis bounded contexts e pelas três camadas lógicas durante a Parte 3, adiando a decomposição física em microsserviços até que o volume de carga ou a expansão para múltiplos times justifique — definindo `Telemetria e Alertas` como primeiro candidato à extração independente.

## Consequências
Melhora a velocidade de entrega da equipe, preserva transações ACID diretas no fluxo crítico de alocação, reduz custos de infraestrutura em nuvem e simplifica a operação (deploy único e depuração local). Piora o isolamento de recursos em runtime (rajadas de telemetria compartilham CPU/RAM com o restante da API) e exige disciplina de revisão de código para impedir acoplamento indevido entre pacotes de contextos distintos. Alternativa descartada: quebrar imediatamente os seis contextos em microsserviços na Parte 3, descartada por introduzir alta complexidade operacional, custo de infraestrutura distribuída e consistência eventual prematura sem demanda real de múltiplos times.