# S-02 — Isolamento por unidade

Estado: proposta para aprovação; código de produção ainda não alterado.

## Objetivo e decisão confirmada

Impedir que uma conta consulte ou altere dados de outra unidade, inclusive
substituindo IDs, omitindo filtros ou usando recursos relacionados.
O usuário confirmou: ADMIN mantém acesso global; os demais perfis são restritos
à unidade vinculada. Os testes devem usar JWT real e instituições diferentes.

## Evidência atual

- `Usuario` e o principal `UsuarioAtual` não possuem unidade.
- `IdentidadeJwt` já reconsulta a conta no banco a cada requisição.
- `RequisicaoService` aceita `hospitalId` e busca por ID sem conferir identidade.
- Bolsa, doação, estoque, veículo, entrega, telemetria, alertas, indicadores e
  algoritmos possuem consultas ou operações globais.
- Alocação escolhe o primeiro hemocentro cadastrado; entrega altera também
  requisição e veículo. Não existe autorização hospital–hemocentro no modelo.
- Os testes de negócio existentes usam autenticação desabilitada. Sua aprovação
  não demonstra isolamento institucional.

## Abordagens consideradas

1. Conferir IDs nos controllers: mudança menor, mas deixa consultas indiretas,
   agregações e chamadas entre serviços sem proteção suficiente.
2. Centralizar a política de acesso e aplicá-la nos serviços e consultas:
   recomendada, por cobrir o modelo atual e seus efeitos transacionais.
3. Isolamento no banco com políticas por linha: exigiria contexto por conexão,
   mudanças no processamento assíncrono e regras complexas para dados relacionados.

A proposta usa a segunda abordagem. A escolha técnica é recomendação do agente;
a regra ADMIN global foi confirmada pelo usuário.

## Identidade e cadastro

- Uma conta não administrativa pertence a uma única unidade nesta entrega.
- `USUARIO_HOSPITAL` exige Hospital; `OPERADOR_HEMOCENTRO` e `LOGISTICA`
  exigem Hemocentro. ADMIN pode ficar sem vínculo e sua autorização é global.
- Migration adiciona `usuarios.unidade_id`, FK com exclusão restrita e índice.
  Nenhuma conta antiga recebe uma unidade arbitrária.
- Contas antigas não administrativas sem vínculo continuam podendo autenticar
  e consultar a própria identidade, mas recebem 403 nas operações de negócio
  até um ADMIN associá-las. Não ficam com acesso global por falta de unidade.
- Cadastro, consulta e atualização administrativa aceitam/retornam `unidadeId`.
  Novo cadastro não administrativo exige vínculo válido. Uma atualização deve
  deixar perfil e unidade compatíveis; omissão preserva o valor existente.
  Remoção explícita do vínculo só é válida se o perfil final for ADMIN.
- A associação é lida do banco na autenticação, sem confiar em IDs de unidade
  enviados pelo cliente ou em claims antigos. Tokens existentes passam a usar
  o novo vínculo na próxima requisição após a atualização administrativa.
- Alterações de vínculo não transferem registros de negócio entre instituições.

## Política compartilhada

Um componente de autorização resolve o principal e sua unidade. ADMIN tem
escopo global; contas sem vínculo válido falham com 403. O modo local/teste
explicitamente sem autenticação preserva os cenários demonstrativos existentes.

Listagens e agregações restringem a consulta antes de paginar, contar e gerar
respostas. Filtro omitido usa a unidade da conta; filtro explícito de outra
unidade retorna 403. Por ID de recurso, consultas restritas retornam 404 tanto
para registro inexistente quanto para registro de outra unidade, evitando
confirmar a existência do recurso. Rejeições ocorrem antes de gravação,
enfileiramento ou mudança de estado.

## Escopo de recursos

| Recurso | Regra para conta não administrativa |
|---|---|
| Requisições, fila e indicadores de demanda/tempo | Hospital da requisição igual à unidade da conta; inclui criar, detalhar, atualizar e cancelar. |
| Doações e bolsas geradas | Hemocentro da doação igual à unidade; detalhe não revela bolsas fora do escopo. |
| Bolsas, estoque e indicadores de estoque | Unidade atual da bolsa/estoque igual à unidade da conta. |
| Rastreabilidade e snapshots de algoritmos | Filtrar os recursos pela unidade e suprimir referências ou detalhes de instituições externas; snapshots globais ficam com ADMIN quando não houver projeção segura. |
| Veículos | Hemocentro-base igual à unidade; troca de base para outra unidade é reservada ao ADMIN. |
| Telemetria | Conferir veículo e unidade antes de enfileirar e antes de consultar histórico. |
| Alertas | Somente recursos pertencentes à unidade; referências conflitantes ou sem proprietário comprovável não são expostas a contas restritas. Detecção disparada por consulta também é limitada ao escopo. |
| Unidades | Conta consulta/edita apenas a própria unidade; criação e exclusão são administrativas. |
| Demonstração e métricas operacionais globais | ADMIN quando autenticação estiver habilitada. |

Dados globais estáticos sem informação institucional, como descrição dos
algoritmos disponíveis, podem continuar acessíveis aos autenticados.

## Operações entre instituições — decisão proposta

Para respeitar a restrição confirmada sem inventar uma rede de atendimento:

- Alocação que altera estoque de um hemocentro e requisição de um hospital
  fica restrita a ADMIN nesta entrega.
- Criação, planejamento, despacho, conclusão e cancelamento de entregas
  ficam com ADMIN, pois alteram ou relacionam dados de instituições diferentes.
- Cada participante pode consultar a própria entrega: hospital destinatário
  ou hemocentro de origem. A resposta expõe somente os dados necessários dessa
  entrega compartilhada, sem conceder acesso geral aos recursos do parceiro.
  A proposta trata a entrega como recurso compartilhado por seus dois
  participantes; essa regra de leitura está sujeita à aprovação deste desenho.
- Na leitura compartilhada são permitidos ID da entrega, ID/código da
  requisição, ID/placa do veículo, ID/nome da origem, status, distância, tempo
  estimado e datas. O campo `rota`, atualmente texto livre, é retornado como
  nulo para não administradores. ADMIN mantém a resposta integral. Os IDs
  informados não concedem permissão de consultar os registros relacionados.
- Requisições de outro hospital, frota de outro hemocentro e histórico completo
  de telemetria do fornecedor continuam inacessíveis.
- A criação administrativa de entrega valida a coerência entre origem e base
  do veículo; a coerência com a origem das bolsas alocadas deve ser verificada
  usando os relacionamentos disponíveis antes de persistir.

Dar autonomia a operadores para atender outros hospitais exige um vínculo de
atendimento explícito, que não faz parte desta proposta. Essa limitação é
relevante para aprovação do desenho.

## Processamento assíncrono e concorrência

A autorização da telemetria acontece no recebimento, antes da fila. O evento
carrega o vínculo validado necessário para que o worker não aplique uma leitura
a um veículo transferido para outra unidade entre aceite e processamento.
Não transportar nem depender do SecurityContext da thread HTTP no worker.

Leituras e alertas novos armazenam também a unidade proprietária no momento
da gravação. Transferir um veículo não transfere esse histórico: a nova base
só consulta leituras capturadas para ela, e nunca herda alertas da antiga.
O endpoint de histórico continua exigindo acesso ao veículo atual; a unidade
anterior perde essa consulta após a transferência, e ADMIN preserva a consulta
integral. Alertas históricos permanecem visíveis apenas à unidade registrada.
Registros legados sem proprietário comprovável ficam visíveis somente ao ADMIN;
não inferir propriedade histórica a partir da base atual. Atualizações de base
e gravações de telemetria devem serializar a verificação desse vínculo para
evitar que uma leitura aceita antes da transferência seja aplicada depois dela.

As demais verificações e mutações ocorrem na transação do serviço, rechecando
o proprietário após locks quando aplicável. A semântica de mudança do vínculo
da conta vale para a próxima requisição; não cancela retroativamente operações
já autorizadas e em execução.

## Testes e validação

Fixtures com dois hospitais, dois hemocentros, usuários vinculados, uma conta
sem vínculo e ADMIN. Tokens assinados e banco PostgreSQL via Testcontainers.

- Operações legítimas na própria unidade e acesso global de ADMIN.
- Bloqueio cruzado por query, corpo, ID direto e IDs de recursos relacionados.
- Omissão de filtros não amplia escopo, inclusive páginas, totais e agregações.
- Bloqueio sem alteração persistida, sem consumo de estoque e sem evento na fila.
- Perfil/unidade incompatíveis, unidade inexistente, vínculo omitido e conta legada.
- Troca administrativa de vínculo refletida ao reutilizar um JWT emitido antes.
- Cobertura de cada família de endpoints, incluindo caminhos alternativos.
- Testes de produção do S-01 continuam exigindo 401 sem JWT.
- Executar testes focados, revisar possíveis desvios de autorização e então
  `mvn verify` completo, incluindo integrações, aceitação e paridade C/Java.

## Entrega e limites

Migration, associação nas APIs de usuário, política central, consultas/serviços
protegidos, testes multiunidade e documentação de migração. Sem alteração de
frontend, publicação ou push. A implementação só será declarada corrigida após
validar bloqueios e controles legítimos e revisar os caminhos indiretos.
