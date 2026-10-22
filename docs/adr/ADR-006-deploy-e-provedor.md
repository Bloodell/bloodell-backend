# ADR-006 — Deploy do backend na EC2

**Data:** 22/10/2026 · **Status:** decidido; primeira publicação automática pendente

## Contexto e decisão

O desafio exige que cada merge em `main` chegue sozinho a uma URL pública,
somente após testes verdes. A equipe já opera uma EC2 com Docker Compose,
PostgreSQL e Nginx; por isso escolheu GitHub Actions e a EC2 existente, em vez
de provisionar outro serviço no Render.

O job de imagem em `ci.yml` depende dos testes Java/PostgreSQL/C11. Em pushes
para `main`, ele transfere a imagem Docker identificada pelo SHA do commit à
EC2 via SSH, atualiza o checkout para o mesmo SHA, executa Compose com
`compose.deploy.yaml` e verifica `/actuator/health`. Um PR roda as validações,
mas não publica. A `main` protegida recebe mudanças após revisão da equipe.

## Segredos, operação e limites

- `EC2_SSH_KEY` fica em GitHub Actions Secrets. `EC2_HOST` e a chave pública do
  servidor em `EC2_KNOWN_HOSTS` ficam nas variáveis do repositório.
- A senha PostgreSQL permanece em `~/bloodell-backend/.env`, ignorado pelo Git.
  O banco usa volume Docker e a porta fica vinculada ao loopback da EC2.
- O IP público automático muda após parada e partida da instância. Atualizar
  `EC2_HOST`, `EC2_KNOWN_HOSTS` e a URL documentada caso isso ocorra.
- O frontend vive em outro repositório e não é publicado por este workflow.
- O Nginx atual serve HTTP/80. HTTPS/443 ainda precisa ser configurado para
  atender também ao critério de segurança de RSD; não alegar que já existe TLS.
- A API não tem autenticação. O ambiente público é somente para dados sintéticos.

## Verificação e rollback

Uma execução verde em PR comprova testes, não publicação. A primeira execução
verde de push em `main` deve mostrar a transferência da imagem, Compose saudável
e a resposta da URL pública. Guardar também um link de execução vermelha que
bloqueou esses passos.

Para rollback, escolher o SHA do deploy anterior saudável e executar na EC2
`BLOODELL_IMAGE_TAG=SHA_ANTERIOR docker compose -f compose.yaml -f compose.deploy.yaml up -d --no-build --wait`,
substituindo `SHA_ANTERIOR` pelo hash real.
As imagens antigas precisam permanecer na instância. Migrações Flyway
incompatíveis exigem plano de restauração do banco, pois reverter a imagem não
reverte o schema.
