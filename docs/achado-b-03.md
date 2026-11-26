# B-03 — colisão na geração de códigos

## Cenário reproduzível

O teste `ColisaoDeCodigosIT` dispara 16 criações ao mesmo tempo, com uma barreira
para que todas leiam o banco antes que as transações concorrentes sejam concluídas.
Ele cobre os dois geradores sequenciais da aplicação:

- doações: `DOA-%06d`;
- requisições: `REQ-%06d`.

Comando de execução:

```powershell
.\mvnw.cmd '-Dit.test=ColisaoDeCodigosIT' failsafe:integration-test failsafe:verify
```

## Hipótese avaliada

Os geradores usam `count() + 1` e verificam a existência do código antes do
`INSERT`. Essa sequência não é atômica: transações paralelas podem calcular o
mesmo próximo valor.

## Evidência esperada

O teste confirma o achado quando há tentativas rejeitadas, enquanto a tabela
mantém somente códigos distintos. Assim, a restrição `UNIQUE` do PostgreSQL
impede a duplicação persistida, mas não evita a colisão nem a falha de criação.
O log `[B-03]` apresenta tentativas, criações, rejeições, linhas persistidas e
códigos distintos para registrar o resultado da execução.

## Resultado observado

Execução concluída em 20/11/2026 com `ColisaoDeCodigosIT`: **2 testes, 0 falhas
e 0 erros**.

| Fluxo | Tentativas paralelas | Criadas | Rejeitadas | Códigos distintos persistidos |
| --- | ---: | ---: | ---: | ---: |
| Doações (`DOA-*`) | 16 | 2 | 14 | 2 |
| Requisições (`REQ-*`) | 16 | 4 | 12 | 4 |

**Conclusão:** o achado está confirmado. Não houve duplicação persistida porque
o índice `UNIQUE` rejeitou os `INSERT`s concorrentes, mas há colisão na geração
e perda de disponibilidade: parte das criações válidas falha sob concorrência.
