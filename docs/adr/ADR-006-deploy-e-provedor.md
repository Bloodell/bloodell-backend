# ADR-006 — Deploy do backend no Render

**Data:** 20/10/2026 · **Status:** decidido para esta entrega

## Contexto e decisão

O desafio exige que o merge em `main` chegue a um endereço HTTPS público após a
esteira verde, sem copiar artefatos manualmente. Escolhemos um Blueprint do Render
(`render.yaml`) com um serviço web Docker e PostgreSQL gerenciado na mesma região.
O web service acompanha `main` e usa `autoDeployTrigger: checksPass`: o Render só
inicia o deploy quando as verificações do commit terminam com sucesso. O serviço
reconstrói a imagem a partir do mesmo `Dockerfile` validado na CI, executa Flyway
na inicialização e verifica `/actuator/health`.

## Razões e limites

- O Blueprint registra serviço, banco, porta, variáveis e política de deploy em
  código versionado, de modo que uma nova instalação possa repetir a configuração.
- O PostgreSQL usa URL interna e `ipAllowList: []`; credenciais são fornecidas pelo
  provedor como variáveis, sem senha no Git. O web service público usa HTTPS na
  borda do Render. O Compose local só publica portas em `127.0.0.1`.
- A API pública usa somente dados sintéticos. O endpoint que recria o cenário de
  demonstração fica desligado no Render. O MVP ainda não possui autenticação
  (ADR-004); antes de publicar dados reais ou manter a API como serviço aberto,
  a equipe precisa proteger as rotas de escrita.
- O pipeline da CI executa testes unitários, integração HTTP/PostgreSQL, paridade
  Java/C11 e build Docker. `checksPass` impede deploy se qualquer uma dessas
  verificações falhar. O fluxo vermelho/verde está em `docs/story-map-release-1.md`.
- O Render reconstrói a imagem do mesmo commit; a imagem não é promovida byte a
  byte da CI. Esta é uma limitação consciente desta configuração.
- O plano gratuito pode suspender o serviço por inatividade e o banco gratuito
  tem prazo de expiração. Antes de uma apresentação, é necessário abrir a URL e
  confirmar que a aplicação acordou e que o banco continua ativo.
- Um rollback usa o deploy anterior bem-sucedido no painel do Render. Como Flyway
  aplica migrações à frente, mudanças de schema incompatíveis exigem plano de
  restauração do banco; rollback do container sozinho pode não bastar.

## Operação pendente

O Blueprint precisa ser conectado à organização GitHub e aplicado uma vez por
uma pessoa com acesso ao Render. Depois da primeira implantação, conferir a URL
pública e registrar no README. Enquanto isso não ocorrer, E3 está implementada
em código, mas **não demonstrada em produção**. Não se deve apresentar a execução
verde de um workflow sem deploy como evidência de publicação.
