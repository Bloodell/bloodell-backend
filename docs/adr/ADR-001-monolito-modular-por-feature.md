# ADR-001 — Monólito modular organizado por feature

**Data:** 19/09/2026 · **Status:** aceito · **Decidem:** equipe

## Contexto

Seis pessoas, dezessete semanas, e uma nota que avalia principalmente a
**integração** entre as disciplinas. O blueprint já indica monólito modular
(§15). A árvore de pastas da p.19, porém, começa pelas camadas (`dominio/`,
`aplicacao/`, `api/`) embora o texto chame a organização de "por feature".

## Decisão

Organizar o backend **primeiro por feature, depois por camada**:

```
bolsa/
  dominio/           Bolsa, EstadoBolsa, FabricaDeBolsas
  aplicacao/         BolsaService
  apresentacao/      BolsaController, DTOs
  infraestrutura/    BolsaRepository
```

As camadas controller/service/repository continuam existindo e visíveis — só
que dentro de cada feature.

## Por quê

Quando alguém mexe em "bolsa", tudo de bolsa está junto. Na organização por
camada, a mesma tarefa abre quatro pastas distantes, e o custo disso cresce
justamente quando seis pessoas trabalham em paralelo. Também fica mais fácil
apontar, na apresentação, onde mora cada caso de uso.

## Consequências

- O módulo `aed/` **não** segue essa divisão: ele é acadêmico, é Java puro e
  fica separado de propósito.
- `compartilhado/` guarda apenas o que é realmente comum (Value Objects,
  exceções, envelope de erro). Não é um depósito de utilidades.

## O que dispara revisão

- Se o professor de Infraestrutura de Software confirmar exigência de DDD tático
  (conflito C3), rever a nomenclatura de agregados e a documentação, **não** a
  estrutura de pastas.
- Se uma feature passar de ~15 classes, avaliar se ela é mais de uma feature.
