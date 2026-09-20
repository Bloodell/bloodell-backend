# ADR-005 — DTOs como `record`, entidades escritas por extenso

**Data:** 19/09/2026 · **Status:** aceito · **Decidem:** equipe

## Contexto

O projeto proíbe Lombok e geradores automáticos de boilerplate. Surge a dúvida:
`record` do Java entra nessa proibição?

## Decisão

- **Entidades, Value Objects e classes de domínio:** escritas por extenso —
  construtor, getters, `equals`, `hashCode` e validações à mão.
- **DTOs de entrada e saída da API:** `record`.

## Por quê

A proibição é de **ferramenta externa que gera código** (Lombok, MapStruct,
scaffold). `record` é sintaxe da própria linguagem, compilada pelo `javac`, sem
dependência nenhuma no `pom.xml` e visível na leitura do arquivo.

E onde a rubrica de POO olha — encapsulamento, invariantes, herança,
polimorfismo — o código continua explícito: é no domínio, não no DTO, que a
orientação a objetos precisa aparecer.

## O que dispara revisão

Se o professor de POO entender que `record` também está vedado, a conversão é
mecânica e local: os DTOs viram classes com construtor e getters, e nenhuma
outra camada muda.
