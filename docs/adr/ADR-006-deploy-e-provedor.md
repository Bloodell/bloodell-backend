# ADR-006 — Deploy: decisão adiada, pipeline preparado

**Data:** 19/09/2026 · **Status:** pendente · **Decidem:** equipe (semana 7)

## Contexto

A Unidade 1 de Infraestrutura de Software cobra pipeline de CI/CD **e** um
primeiro deploy acessível. O provedor ainda não foi escolhido.

## Situação atual

- `ci.yml` está completo e rodando: build, testes, integração com Testcontainers,
  paridade C++ × Java e build da imagem Docker.
- `deploy.yml` existe com os passos de publicação **comentados**.

## Por que os passos estão comentados

Um deploy meio configurado falha em todo push para `main` e transforma o mural
de CI em ruído vermelho — exatamente o contrário do que a rubrica pede ao falar
em "pipeline que não falha aleatoriamente".

## O que falta decidir (semana 7)

1. provedor: Render, Railway ou Fly.io (todos têm camada gratuita suficiente);
2. onde fica o PostgreSQL gerenciado (Render, Neon ou Supabase);
3. criar o secret `DEPLOY_TOKEN` no repositório;
4. descomentar os passos e conferir o health check pós-deploy.

## Restrição que não muda

Borda pública em **HTTPS/443**. Banco em sub-rede privada, sem porta publicada.
HTTP/80 exposto conta como erro de segurança na avaliação de RSD.
