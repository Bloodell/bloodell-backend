# language: pt
@US-R1-01
Funcionalidade: Registrar doação e disponibilizar bolsas
  Como operador do hemocentro, quero registrar uma doação e consultar
  as bolsas geradas para acompanhar sua disponibilidade no estoque.

  Contexto:
    Dado um hemocentro cadastrado

  @R1-01B
  Cenário: a doação gera bolsas com validade por componente e elas entram no estoque
    Quando registro no hemocentro uma doação "O-" de 450 ml com os componentes:
      | CONCENTRADO_HEMACIAS |
      | PLASMA               |
    Então a API responde 201
    E a doação gera 2 bolsas com status "DISPONIVEL"
    E a bolsa de "CONCENTRADO_HEMACIAS" vence 42 dias após a coleta
    E a bolsa de "PLASMA" vence 365 dias após a coleta
    E ao consultar a doação novamente ela tem 2 bolsas
    E o estoque do hemocentro tem 1 bolsa "O-" de "CONCENTRADO_HEMACIAS"
    E o estoque do hemocentro tem 1 bolsa "O-" de "PLASMA"

  @R1-01B
  Cenário: tipo sanguíneo inválido é rejeitado sem gravar nada
    Quando registro no hemocentro uma doação "X+" de 450 ml com os componentes:
      | PLASMA |
    Então a API responde 400
    E a resposta aponta erro no campo "tipoSanguineo"
    E nenhuma doação foi registrada

  @R1-01B
  Cenário: doação em unidade que não é hemocentro é recusada pela regra de negócio
    Dado um hospital cadastrado
    Quando registro no hospital uma doação "A+" de 450 ml com os componentes:
      | PLASMA |
    Então a API responde 422
    E a resposta explica que "so pode ser registrada em um hemocentro"
    E nenhuma doação foi registrada
