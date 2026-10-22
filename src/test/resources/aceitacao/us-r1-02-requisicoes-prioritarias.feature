# language: pt
@US-R1-02
Funcionalidade: Criar e consultar requisições prioritárias
  Como operador, quero registrar demandas hospitalares e consultar a fila
  para visualizar a ordem de atendimento por prioridade e chegada.

  Contexto:
    Dado um hospital cadastrado

  @R1-02B
  Cenário: a fila ordena por prioridade e preserva a chegada no empate
    Quando o hospital cria as requisições, nesta ordem:
      | requisicao | prioridade |
      | A          | ELETIVA    |
      | B          | URGENTE    |
      | C          | EMERGENCIA |
      | D          | URGENTE    |
    Então a fila de atendimento é "C, B, D, A"
    E cada requisição criada pode ser consultada com status "NA_FILA"

  @R1-02B
  Cenário: a fila vazia é consultada sem erro
    Então a fila de atendimento está vazia

  @R1-02B
  Cenário: requisição sem itens é rejeitada e não entra na fila
    Quando o hospital cria uma requisição "URGENTE" sem itens
    Então a API responde 400
    E a resposta aponta erro no campo "itens"
    E a fila de atendimento está vazia

  @R1-02B
  Cenário: quantidade zero é rejeitada e não entra na fila
    Quando o hospital cria uma requisição "EMERGENCIA" de 0 "CONCENTRADO_HEMACIAS" "A+"
    Então a API responde 400
    E a resposta aponta erro no campo "itens[0].quantidade"
    E a fila de atendimento está vazia
