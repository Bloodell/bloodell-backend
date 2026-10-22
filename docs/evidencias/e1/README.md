# Evidências de container — E1-04

Saídas reais do ambiente local subindo com `docker compose`. O cabeçalho de cada
arquivo traz o commit, a branch, a data e o comando exato.

| Arquivo | O que mostra |
|---|---|
| [07-compose-ps.txt](07-compose-ps.txt) | `docker compose ps` com a API e o PostgreSQL `(healthy)` |
| [08-api-resposta.txt](08-api-resposta.txt) | `curl.exe -i` no health check (`UP`) e em `GET /api/v1/algoritmos/situacao` |

## O que fica de fora

Nenhuma variável de ambiente entra nas evidências. O script não roda
`docker compose config` nem `env`, que imprimiriam as senhas do `.env`, e o health
check não expõe detalhes do banco (`show-details` fica no padrão `never`).

## Como regenerar

Com o ambiente de pé (Windows, PowerShell):

```powershell
docker compose up --build -d --wait
.\scripts\gerar-evidencias.ps1
```

O script falha se os dois serviços não estiverem `(healthy)` ou se a API não
responder, então ele nunca grava uma evidência de execução quebrada.
