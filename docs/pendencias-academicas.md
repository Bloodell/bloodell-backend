# Pendências acadêmicas — nada decidido silenciosamente

Onde o blueprint tem conflito ou lacuna, o código adotou a interpretação mais
conservadora — a que atende às duas versões em disputa — e registrou o ponto
aqui. Este arquivo é a lista que a equipe leva aos professores.

## Conflitos já mapeados no blueprint (seção 4)

| # | Pergunta ao professor | O que o código faz enquanto não há resposta |
|---|---|---|
| C1 | A rubrica de AED U2 é a da planilha (compatibilidade 45 / integração 35 / complexidade 20)? | Não afeta o código da U1. Afeta o backlog da U2. |
| C2 | Qual rubrica de SO U1 vale? "Robustez do pipeline" será avaliada? | O pipeline foi feito robusto (sem dependência de rede externa nos testes, seed fixo, Testcontainers): atende às duas versões. |
| C3 | DDD tático é exigido pela disciplina de SO? | O domínio já tem entidades, Value Objects e agregados visíveis. Se for exigido, falta apenas **documentar**, não reescrever. |
| C4 | A evidência de AED U1 pede "rota e priorização"? Rota não é U2? | A U1 entrega estruturas + testes. Rota **não** foi implementada. Priorização de requisições existe (é da fila) e não se confunde com FEFO. |
| C5 | Conjunto final de métricas do benchmark de RSD? | Ainda não coletado (é da U2). As métricas de ingestão já existem em `/api/v1/telemetrias/metricas`. |
| C6 | O pipeline da U1 pode antecipar conteúdo da semana 14? | O pipeline já está pronto. Se a resposta for negativa, nada se perde. |
| C7 | A apresentação de POO (09/11) é separada da final do PI? | Afeta o cronograma, não o código. |
| C8 | No checkpoint U1, AED entrega só lista/fila/pilha? | É exatamente o que existe. `aed/u2` tem só interface e stub. |
| C9 | Células com número serial no roadmap. | Cosmético. Avisar o professor de Projeto. |
| C10 | Semanas 10–11 são o início da U2 e 15–16 o fechamento? | Afeta o cronograma. |

## Pontos que este código levantou (novos)

| # | Ponto | Decisão conservadora adotada | Onde |
|---|---|---|---|
| N1 | O blueprint escreve `CONCENTRADO_HEMACOCIAS`, aparentemente erro de digitação de "hemácias" | Enum usa `CONCENTRADO_HEMACIAS`. Confirmar antes da entrega, pois aparece na migration e na API | `TipoComponente` |
| N2 | A tabela `doacoes` tem coluna `componente` (p.14), mas o UC1 diz que uma doação gera 1..n bolsas de componentes diferentes | O componente ficou na bolsa. A doação não tem componente único | `V1__esquema_inicial.sql` |
| N3 | A rota proposta `POST /api/v1/demo/seed?cenario=&seed=` usa verbo e abreviação, contra a regra de RSD | Virou `POST /api/v1/demonstracoes/cenarios`. Mesmo comportamento. Confirmar com RSD | `DemonstracaoController` |
| N4 | O blueprint alterna a grafia `Bloodell` e `bloodel` | Código e pacotes usam `bloodell`. O nome de arquivo exigido pela entrega de RSD (`arquitetura-bloodel-...`) mantém a grafia prescrita | pacote `br.edu.cesar.bloodell` |
| N5 | "FIFO com urgência" admite duas implementações (três filas FIFO ou inserção ordenada) com a **mesma ordem de saída** e Big-O diferente | Três filas FIFO, O(1). A equivalência está provada por teste; trocar não afeta nenhuma outra classe | `FilaRequisicoes` |
| N6 | O blueprint cita JUnit 5, mas não fixa Java nem Spring Boot; Boot 4 exige Jupiter 6 | Boot 4.1.1 + Java 25 + Jupiter 6, com instruções de reversão em duas linhas | ADR-003 |
| N7 | `Usuario` está no schema, mas login está fora do MVP | Tabela criada; **sem** entidade, endpoint ou tela. Ponto de extensão preparado e desligado | ADR-004 |
| N8 | O blueprint não define de qual hemocentro sai a bolsa quando há mais de um | A U1 usa o primeiro cadastrado e **diz isso** nos passos da decisão. Escolher por distância depende de roteirização (U2) | `AlocacaoService` |
| N9 | Não havia formato de corpo de erro definido | Envelope único inspirado na RFC 7807 | `ProblemaApi` |
| N10 | Não havia estratégia de migração de banco | Flyway com SQL versionado e `ddl-auto=validate` | ADR-002 |

## Como usar esta lista

1. Levar como uma página por disciplina, na reunião com cada professor.
2. Ao receber a resposta, **atualizar a linha aqui** e abrir issue se houver
   trabalho decorrente.
3. Uma reunião curta de 15 minutos do time a cada resposta recebida, para decidir
   o impacto no backlog. Resposta de professor que fica só na memória de quem
   perguntou vira retrabalho depois.
