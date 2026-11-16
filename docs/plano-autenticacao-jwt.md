# Plano de autenticacao JWT — Entrega 03

Estado: Card 1 implementado localmente, com validacao HTTP em 30/10/2026; escopo e configuracao em
[`autenticacao-jwt-card-1.md`](autenticacao-jwt-card-1.md). O roteiro abaixo registra
o planejamento original dos cards e nao declara os passos posteriores concluidos. Branch:
`feature/autenticacao-jwt`, criada a partir de `dev`.

## Escopo combinado

Implementar login e autorizacao no backend existente (Java 25, Spring Boot
4.1.1, PostgreSQL e Flyway). Reutilizar a tabela `usuarios`; nao criar cadastro
publico, nao implementar a Central de Operacoes nem migrar o deploy nesta tarefa.
O filtro `PontoDeExtensaoDeAutenticacao` atual somente registra um aviso e nao
protege a API, mesmo quando sua propriedade esta habilitada.

W06/W08 permanecem pendentes conforme a documentacao do projeto. Este plano
nao declara esses requisitos concluidos nem antecipa o diferencial da central.

## Historias propostas para o backlog

- AUTH-01: Como integrante autorizado, quero entrar com e-mail e senha para
  acessar minha conta e consultar minha identidade autenticada.
- AUTH-02: Como responsavel pelo hemocentro, quero restringir operacoes por
  perfil e identificar o operador para impedir acoes sem permissao e permitir
  a rastreabilidade das futuras decisoes.

Confirmar os identificadores e o enquadramento dessas historias no backlog da
equipe antes de apresenta-las como as duas historias da Entrega 03.

## Passos de implementacao e aceite

1. Escrever testes de login valido/invalido, token ausente/invalido/expirado e
   perfil insuficiente. Executar antes da implementacao e preservar a evidencia
   real de falha. Os testes antigos de negocio nao comprovam autenticacao.
2. Mapear `Usuario`, `Perfil` e repositorio para a tabela existente. Armazenar
   somente hash de senha com PasswordEncoder BCrypt. Contas sem hash valido
   nao podem entrar. Provisionar a primeira conta de forma administrativa, sem
   senha fixa no Git e sem cadastro publico.
3. Implementar POST `/api/v1/auth/login` com JSON `{email, senha}` e retorno
   `{accessToken, tokenType, expiresIn, usuario}`. Credenciais invalidas retornam
   401 com mensagem generica. Nenhum retorno inclui senha/hash. Aplicar
   `Cache-Control: no-store` na resposta de login.
4. Usar Spring Security e seu suporte JWT para emitir/validar assinatura,
   emissor, audiencia e expiracao. Chave somente em variavel/secrets, sem
   fallback fixo. Requisicoes enviam `Authorization: Bearer <token>`.
5. Implementar GET `/api/v1/auth/me` com `{id, nome, email, perfil}` do usuario
   autenticado. Fornecer a identidade autenticada aos servicos; nunca aceitar
   o nome/id do operador fornecido no corpo como prova de identidade.
6. Substituir o filtro que apenas avisa pela cadeia de seguranca. Adotar
   acesso restrito por padrao; liberar explicitamente login, saude e os futuros
   snapshots publicos, revisados como agregados sinteticos. Nao liberar todos
   os GET existentes. Sem autenticacao: 401; autenticado sem permissao: 403.
7. Definir e testar a matriz completa de rotas com a equipe antes de ativar:
   ADMIN administra usuarios/cenarios/configuracoes; OPERADOR_HEMOCENTRO atua
   em estoque/doacoes/alocacao; USUARIO_HOSPITAL em requisicoes; LOGISTICA em
   entregas/telemetria/alertas. Confirmar o acesso administrativo e a separacao
   de decisoes logisticas e do hemocentro. Essas decisoes ainda nao existem.
8. Integrar o frontend separado: login real, envio do Bearer, tratamento de
   401/403 e expiracao. Nao publicar a protecao na main antes de o fluxo estar
   coordenado com o frontend. O login atual do site pode precisar ser adaptado.
9. Rodar testes unitarios e integracao com PostgreSQL, paridade AED e build
   Docker na CI. Registrar cenarios publicos sem dados pessoais, CORS e
   regressao das funcionalidades existentes. Fazer PR para dev e code review.
10. Antes do deploy, provisionar contas e chave na EC2, documentar configuracao
    sem revelar valores e validar HTTPS. Seguir a esteira existente dev → main.

JWT controla autenticacao/perfil; restricoes por hospital ou unidade exigem
vinculo de usuario com unidade e regras adicionais. Nao declarar isolamento
entre instituicoes implementado somente por existir o perfil.

## Evidencias da Entrega 03

- No README: secao Entrega 03, historias implementadas no formato POST-IT,
  criterios atendidos e links para commits/PRs/testes.
- Video do sistema funcionando, com audio ou legenda: login, consulta de
  identidade, acao permitida, 401 sem token e 403 com perfil incorreto.
- Video explicando codigo: usuarios/hash, login, emissao/validacao do JWT,
  autorizacao e testes. Nunca mostrar chaves, tokens reais ou senhas.
- Bug tracker do GitHub atualizado, com captura no README.
- Commits de codigo frequentes e evidencias verdadeiras da jornada dos testes.

## Fontes tecnicas

- https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
