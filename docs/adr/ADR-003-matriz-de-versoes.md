# ADR-003 — Java 25, Spring Boot 4.1.1 e JUnit Jupiter 6

**Data:** 19/09/2026 · **Status:** aceito, com pendência registrada · **Decidem:** equipe

## Contexto

O blueprint cita "JUnit 5" (p.26) mas não fixa versão de Java nem de Spring
Boot. A linha 3.5 do Spring Boot encerrou as versões OSS em 25/06/2026. O Spring
Framework 7, base do Boot 4, integra testes com Jupiter 6.

## Decisão

`spring-boot-starter-parent` **4.1.1**, `java.version` **25**, JUnit Jupiter na
versão que o BOM do Boot 4.1 gerencia (linha 6).

## Por quê

É a linha atualmente suportada e é compatível com o JDK 25 já instalado nas
máquinas da equipe. Manter Boot 3.5 significaria começar o semestre em uma linha
sem atualização pública.

## Pendência (não é decisão do professor, é revisão da equipe)

A menção a "JUnit 5" no blueprint precisa ser atualizada para Jupiter 6, ou a
equipe precisa justificar a mudança na entrega. **Forçar Jupiter 5 sobre Boot 4
não resolve — é incompatível.**

## Como voltar atrás, se preciso

Trocar duas linhas no `pom.xml`: a `<version>` do parent para `3.5.x` e
`<java.version>` para `21`. Nenhuma classe do projeto usa API exclusiva do
Boot 4 — isso foi mantido de propósito.

## O que dispara revisão

- Orientação docente explícita sobre a versão de JUnit.
- Incompatibilidade de springdoc, Testcontainers ou Cucumber com a linha 4.1.
- Provedor de deploy que não ofereça runtime para Java 25.
