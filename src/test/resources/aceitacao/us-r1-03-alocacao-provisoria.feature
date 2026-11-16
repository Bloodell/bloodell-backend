# language: pt
@US-R1-03
Funcionalidade: Processar alocação provisória U1
  Como operador, quero processar uma requisição e consultar o resultado
  persistido para acompanhar a reserva das bolsas.

  Contexto:
    Dado um hemocentro cadastrado
    E um hospital cadastrado

  @R1-03B
  Cenário: aloca a primeira bolsa do mesmo tipo pela ordem de chegada, sem FEFO
    Dado as doações registradas no hemocentro, nesta ordem:
      | bolsa    | tipo | componente           | coletada ha dias |
      | primeira | A+   | CONCENTRADO_HEMACIAS | 1                |
      | segunda  | A+   | CONCENTRADO_HEMACIAS | 10               |
    E o hospital cria uma requisição "URGENTE" de 1 "CONCENTRADO_HEMACIAS" "A+"
    Quando processo a alocação da requisição
    Então a API responde 201
    E a política aplicada é "PROVISORIA_U1"
    E a bolsa alocada é "primeira"
    E ao consultar novamente a requisição está com status "ALOCADA"
    E a bolsa "primeira" está com status "ALOCADA"
    E a bolsa "segunda" está com status "DISPONIVEL"

  @R1-03B
  Cenário: tipo diferente não é usado, porque a U1 não aplica compatibilidade ABO/Rh
    Dado as doações registradas no hemocentro, nesta ordem:
      | bolsa     | tipo | componente           | coletada ha dias |
      | universal | O-   | CONCENTRADO_HEMACIAS | 1                |
    E o hospital cria uma requisição "EMERGENCIA" de 1 "CONCENTRADO_HEMACIAS" "A+"
    Quando processo a alocação da requisição
    Então a API responde 409
    E a resposta explica que "continua na fila"
    E ao consultar novamente a requisição está com status "NA_FILA"
    E a bolsa "universal" está com status "DISPONIVEL"

  @R1-03B
  Cenário: bolsa vencida é recusada e a requisição continua na fila
    Dado as doações registradas no hemocentro, nesta ordem:
      | bolsa   | tipo | componente | coletada ha dias |
      | vencida | B+   | PLAQUETAS  | 10               |
    E o hospital cria uma requisição "URGENTE" de 1 "PLAQUETAS" "B+"
    Quando processo a alocação da requisição
    Então a API responde 409
    E a resposta explica que "continua na fila"
    E ao consultar novamente a requisição está com status "NA_FILA"
    E a bolsa "vencida" está com status "DISPONIVEL"

  @R1-03B
  Cenário: estoque vazio não registra alocação e a requisição continua na fila
    Dado o hospital cria uma requisição "ELETIVA" de 2 "PLASMA" "AB-"
    Quando processo a alocação da requisição
    Então a API responde 409
    E a resposta explica que "continua na fila"
    E ao consultar novamente a requisição está com status "NA_FILA"
