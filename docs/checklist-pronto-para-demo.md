# Checklist "pronto para apresentar"

Rodar **um dia antes** de cada entrega ou apresentação, na máquina que vai ser
usada, e com a mesma rede da sala quando possível.

Isto é diferente do Definition of Done técnico. CI verde prova que o código
funciona; este checklist prova que **a demonstração** funciona. Já aconteceu em
todo projeto: build verde, apresentação travada.

Responsável do dia: _______________ · Data: ____/____/______

## 1. O ambiente sobe do zero

- [ ] `git clone` em uma pasta nova, `cp .env.example .env`, preencher as senhas
- [ ] `docker compose up --build -d` termina sem erro
- [ ] `docker compose ps` mostra os dois serviços saudáveis
- [ ] `GET /actuator/health` responde `UP`
- [ ] Frontend: `npm ci && npm run build && npm run dev` abre a aplicação

> Se algum passo falhar, é aqui que se descobre — não na frente da banca.

## 2. Os dados da demonstração existem

- [ ] `POST /api/v1/demonstracoes/cenarios` com `{"cenario":"aula","semente":42}` responde 201
- [ ] A tela de estoque mostra bolsas reais, vindas da API
- [ ] A fila mostra requisições e a ordem está coerente com as prioridades
- [ ] Nenhuma tela mostra número mockado ou "carregando" eterno

## 3. O roteiro inteiro roda, do começo ao fim

- [ ] Registrar uma doação nova pela interface e ver a bolsa aparecer no estoque
- [ ] Criar uma requisição e vê-la entrar na fila na posição certa
- [ ] Processar a requisição e ver os passos reais da decisão
- [ ] Abrir o visualizador e mostrar lista, fila e pilha
- [ ] Abrir os indicadores e mostrar os números
- [ ] Ensaiado com cronômetro, dentro do tempo combinado

## 4. Os limites acadêmicos estão visíveis

- [ ] A alocação aparece rotulada como **provisória da Unidade 1** na tela
- [ ] `GET /api/v1/algoritmos/situacao` mostra o que é stub da Unidade 2
- [ ] O aviso de dados sintéticos aparece nos painéis
- [ ] Ninguém do time vai chamar "primeira da lista" de FEFO durante a fala

## 5. O que fazer quando der errado

- [ ] Screencast completo gravado e **aberto em outra aba**, pronto para usar
- [ ] Deploy online acessível como alternativa ao ambiente local
- [ ] Um print de cada tela principal, caso a aplicação não suba
- [ ] Alguém do time sabe reiniciar tudo em menos de 2 minutos

## 6. Evidência da entrega

- [ ] CI verde no commit que vai ser apresentado (inclusive a paridade C++ × Java)
- [ ] README atualizado com a seção da entrega e o link do screencast
- [ ] Tag criada (`v0.1-u1`, `v1.0-u2`)
- [ ] Commits distribuídos entre os integrantes na semana
- [ ] Quadro do projeto atualizado

## 7. A sala

- [ ] Notebook no carregador, modo de suspensão desligado
- [ ] Zoom do navegador em nível legível no telão (125% a 150%)
- [ ] Abas desnecessárias, notificações e mensagens fechadas
- [ ] Testado no projetor, se houver chance de testar antes
- [ ] Cada integrante sabe qual parte vai apresentar

---

Um item marcado sem ter sido realmente executado é pior do que item não marcado:
cria confiança falsa. Na dúvida, execute.
