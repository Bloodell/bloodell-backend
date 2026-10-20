# Roteiro de apresentação — Container, TDD e deploy

Cada integrante deve conseguir explicar o fluxo inteiro, mesmo que a demonstração
seja dividida entre pessoas. Reservar cerca de 5–7 minutos para a demonstração e
deixar os links de evidência abertos antes da aula.

1. **E1 — subir do zero.** Mostrar `Dockerfile` multi-stage, `compose.yaml` e
   `.env.example`. Em uma pasta limpa, copiar o exemplo para `.env`, ajustar a
   senha e executar `docker compose up --build -d`. Confirmar
   `docker compose ps`, `/actuator/health` com status `UP` e uma chamada da API.
   Explicar que Postgres fica na rede interna, com porta local limitada a
   `127.0.0.1`, e que o volume preserva os dados.
2. **E2 — story map e TDD.** Abrir `docs/story-map-release-1.md`; percorrer as
   três histórias e os testes de unidade, integração e aceitação correspondentes.
   Mostrar a [CI vermelha](https://github.com/Bloodell/bloodell-backend/actions/runs/39361848840),
   o teste que falhou, o commit de correção `94c9e41` e a
   [CI verde](https://github.com/Bloodell/bloodell-backend/actions/runs/39363541316).
   Explicar a lição: validar componentes repetidos antes de persistir evita bolsas
   fictícias no estoque. Distinguir essa regra nova das regras antigas cobertas
   posteriormente por testes.
3. **E3 — pipeline para produção.** Mostrar os jobs em `ci.yml` e o bloqueio do
   build da imagem na execução vermelha. Mostrar `render.yaml` e a política
   `checksPass`; abrir o deploy real do commit em `main`, a URL HTTPS pública e o
   `/actuator/health`. Explicar Flyway, variáveis de ambiente e rollback pelo
   deploy anterior. **Esta última demonstração só pode ser feita depois da
   ativação do Blueprint e de um deploy real.**
4. **Fechar com decisões.** Usar a tabela do README e a ADR-006 para justificar
   escolha de runtime, banco, níveis de teste, segurança e limites do plano
   gratuito. Declarar o apoio de IA e a revisão da equipe.

Antes da apresentação: preencher os integrantes reais no README, anexar a URL
pública e registrar o link de uma execução de deploy de `main`. Se o serviço
gratuito estiver adormecido, abrir a URL com antecedência e aguardar a ativação.
