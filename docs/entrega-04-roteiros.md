# Roteiros dos screencasts — Entrega 04

Gravar com áudio (ou legenda), publicar no YouTube (não listado serve) e colar os
links no README. Dados sempre sintéticos. Use o Swagger UI
(`/swagger-ui.html`) ou Postman/Insomnia; tenha um ADMIN já provisionado
(`docs/autenticacao-jwt-card-1.md`).

## Vídeo 1 — Uso do sistema (≈ 5–6 min)

| Tempo | Tela | Fala |
|---|---|---|
| 0:00 | Slide/README | "Somos a equipe Bloodell, projeto Rota Vital. Nesta entrega implementamos autenticação JWT e gestão de usuários." |
| 0:30 | Swagger, rota protegida sem token | Chamar `GET /api/v1/doacoes` sem token → **401**. "Com a autenticação ligada, a API não responde a anônimos." |
| 1:15 | `POST /auth/login` com e-mail/senha errados | → 401, mensagem genérica. "Não revela se o e-mail existe." |
| 1:45 | `POST /auth/login` como ADMIN | Mostrar `accessToken`, `expiresIn`, `perfil`. Clicar em Authorize e colar o token. |
| 2:30 | `GET /auth/me` | "Retorna o perfil lido do banco, não do token." |
| 3:00 | `POST /usuarios` (Card 4) | Cadastrar `integrante@example.test`, perfil `OPERADOR_HEMOCENTRO` → **201**, sem senha na resposta. Repetir → **409**. Enviar e-mail inválido → **400**. |
| 4:00 | Login com a conta nova | Mostrar que entra; tentar `POST /usuarios` com ela → **403**. |
| 4:30 | `GET /usuarios?page=0&size=20` e `/{id}` (Card 5) | Listagem paginada; id inexistente → **404**. |
| 5:00 | `PATCH /usuarios/{id}` (Card 6) | Mudar nome/perfil; tentar rebaixar o único ADMIN → **409**. |
| 5:30 | Frontend `/rastreamento` | Abrir a Central logística: mapa, caminhões em 3D, dados da API; mostrar o estado de erro com a API parada. |
| 5:50 | README/Issues | "Backlog e bugs no GitHub Issues; CI e deploy automáticos na EC2." Encerrar. |

## Vídeo 2 — Explicação do código (≈ 6–8 min)

| Tempo | Arquivo (abrir na IDE) | Pontos a explicar |
|---|---|---|
| 0:00 | Estrutura `autenticacao/` | Camadas `dominio`, `aplicacao`, `apresentacao`, `infraestrutura` dentro da feature. |
| 0:45 | `dominio/Usuario.java`, `Perfil.java` | Entidade, quatro perfis, e-mail normalizado (migration `V3`). |
| 1:30 | `aplicacao/AutenticacaoService.java` | BCrypt (custo 12), conta sem hash válido não entra, emissão do token. |
| 2:30 | `infraestrutura/ConfiguracaoJwt.java`, `PropriedadesJwt.java` | Chave externa ao repositório, validade, validação do token. |
| 3:15 | `infraestrutura/IdentidadeJwt.java`, `config/ConfiguracaoSeguranca.java` | Spring Security stateless; reconsulta o banco para o perfil; regra "ADMIN só em `/api/v1/usuarios/**`"; modo demonstrativo nunca libera usuários. |
| 4:15 | `CadastroDeUsuarioService`, `UsuarioController`, `UsuarioDtos` | Validações, 409 de e-mail duplicado, resposta sem hash, `Cache-Control: no-store`. |
| 5:00 | `ConsultaDeUsuarioService` | Paginação com limite de 100 e ordem fixa. |
| 5:30 | `AtualizacaoDeUsuarioService`, `BloqueioDeUsuarios` | PATCH parcial; proteção do último ADMIN também sob concorrência (bloqueio). |
| 6:15 | Frontend `features/rastreamento/` | `pagina-de-rastreamento.tsx`, `mapa-logistico.tsx`, `camada-de-caminhoes.ts` e `useRecurso` (carregando/erro/vazio). |
| 6:30 | `application-prod.yml`, `AutenticacaoProducaoIT` (PR #39) | JWT obrigatório em produção. |
| 7:00 | Testes: `AutenticacaoIT`, `CadastroDeUsuarioIT`, `AtualizacaoDeUsuarioIT` | Rodar `./mvnw -B verify` e mostrar verde; mostrar o CI no GitHub Actions. |
