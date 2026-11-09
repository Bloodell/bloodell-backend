# S-02 Implementation Plan

> **For agentic workers:** Execute with superpowers:executing-plans. Track progress here.

**Goal:** Isolar dados por unidade, mantendo ADMIN global.
**Architecture:** Principal obtido do banco; política central de escopo aplicada nos serviços; consultas filtradas antes de paginação/agregação; propriedade histórica em telemetria/alertas.
**Tech Stack:** Java 25, Spring Boot 4, JPA/PostgreSQL, Flyway, JUnit/Testcontainers.
**Spec:** `docs/superpowers/specs/2026-11-06-s02-isolamento-unidades-design.md` (aprovado).

## Global Constraints

- Uma unidade por usuário não ADMIN; tipos compatíveis; legado sem vínculo bloqueado.
- ADMIN global. Alocação e mutações de entrega administrativas. Entrega compartilhada somente para leitura e sem rota livre.
- Filtro estrangeiro: 403; recurso por ID estrangeiro/inexistente: 404.
- Nenhum push; preservar modo demonstrativo explícito e executar suite completa.

## Review Focus

- Filtro omitido e totais de paginação: teste deve conter registros de ambas as unidades.
- Reutilização do JWT após troca de vínculo: próxima requisição observa o banco.
- Referências externas em histórico/entrega: campos livres não expõem parceiros.
- Telemetria pendente durante troca de base: worker rejeita leitura do vínculo antigo.
- Usuário sem vínculo e principal ausente: falha fechada com segurança habilitada.

## Task 1 — Identidade e política compartilhada

Files: `autenticacao/dominio/Usuario.java`, `autenticacao/apresentacao/{UsuarioDtos,AutenticacaoDtos}.java`, `autenticacao/aplicacao/{CadastroDeUsuarioService,AtualizacaoDeUsuarioService,AcessoUnidade}.java`, `autenticacao/infraestrutura/CadastroDeUsuarioRepository.java`, migration V4.
Interfaces: `AcessoUnidade.escopo(): Long` (null somente global), `filtrar(Long): Long`, `exigirUnidade(Long): void`, `exigirRecurso(Long,String,Long): void`, `exigirAdmin(): void`, `global(): boolean`.
- [ ] Testar conta legada sem vínculo recebendo 403 (RED).
- [ ] Implementar vínculo FK, DTOs, validação de tipo e política fail-closed.
- [ ] Testar criação inválida, leitura do vínculo e mudança com JWT existente.

## Task 2 — Recursos e consultas

Files: serviços/repositories de unidade, requisição, doação, bolsa, estoque, indicadores, algoritmos, veículo, entrega, alocação, demonstração.
Consumes: política da Task 1.
- [ ] Cobrir duas unidades e caminhos diretos/indiretos (RED).
- [ ] Aplicar escopo antes de consultas, paginação e efeitos colaterais.
- [ ] Reservar operações entre instituições ao ADMIN e projetar entrega compartilhada.
- [ ] Testar casos positivos e ausência de mutações após negativas.

## Task 3 — Telemetria e alertas

Files: serviços, entidades e repositories de telemetria/alerta; fila e worker; migration V5.
Interfaces: evento enfileirado preserva unidade autorizada; gravação serializa com mudança de base do veículo.
- [ ] Testar envio/histórico estrangeiro e evento com base antiga (RED).
- [ ] Persistir proprietário histórico, proteger leitura, fila e worker; legado desconhecido apenas ADMIN.
- [ ] Validar isolamento de alerta e transferência de veículo.

## Task 4 — Regressão, revisão e entrega

Files: `src/test/java/.../api/IsolamentoUnidadesIT.java`, fixtures existentes e documentação.
- [ ] Atualizar fixtures explícitas de usuários sem enfraquecer asserções existentes.
- [ ] Executar testes focados e revisão independente de bypass/regressão.
- [ ] Corrigir problemas comprovados e executar `mvn.cmd -B verify` completo.
- [ ] Conferir diff; registrar commit na branch S-02, sem push.

## Ledger

- Desenho aprovado pelo usuário; execução no checkout atual em branch própria, seguindo o fluxo autorizado no S-01.
- Investigação independente concluída antes do desenho: ausência de vínculo e bypasses indiretos confirmados estaticamente.
