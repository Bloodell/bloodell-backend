# ADR-002 — Estruturas da AED montadas por execução (e migrations versionadas)

**Data:** 19/09/2026 · **Status:** aceito · **Decidem:** equipe

## Contexto

O blueprint diz que as estruturas manuais são "hidratadas no boot e após cada
escrita" e que o visualizador mostra "os mesmos objetos" (§15.2). Isso sugere
manter lista, fila e pilha vivas em memória durante toda a execução da
aplicação. Mas o blueprint não define o que acontece quando uma transação é
desfeita.

## Decisão A — estruturas por execução

Cada caso de uso monta a estrutura a partir de um retrato do banco, executa o
algoritmo e descarta a estrutura. O visualizador mostra o retrato de uma
execução real, com o instante em que foi capturada.

**Por quê:** se a estrutura vivesse em memória e a transação falhasse depois de
o nó já ter sido removido, a lista mostraria uma bolsa que o banco ainda tem —
ou esconderia uma que existe. Consertar isso exigiria coordenar memória e
transação à mão, que é trabalho de sobra para o benefício. Com estrutura por
execução, o banco é sempre a verdade e a estrutura nunca "mente".

**Custo assumido:** montar a lista é O(n) a cada consulta. Para o volume deste
projeto (dezenas a poucos milhares de bolsas), isso é irrelevante.

**O que dispara revisão:** se o volume crescer a ponto de a montagem aparecer no
tempo de resposta medido, ou se o professor de AED exigir identidade permanente
do objeto no visualizador.

## Decisão B — migrations versionadas com Flyway

O schema muda por arquivo novo em `db/migration`, nunca por alteração manual no
banco de alguém. O Hibernate roda com `ddl-auto=validate`: ele confere se o
mapeamento bate com o schema, e não cria nada.

**Por quê:** sem isso, cada integrante recria o banco do zero quando algo
quebra, e os dados de teste somem toda semana. Com migrations, o banco de
qualquer máquina chega ao mesmo estado na mesma ordem — e a revisão de schema
acontece no pull request, junto com o código.

**O que dispara revisão:** exigência docente de outra ferramenta.
