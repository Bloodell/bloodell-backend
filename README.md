# Bloodell — backend

## 1. Objetivo do repositório

O Bloodell é um backend acadêmico para demonstrar uma API de gestão,
distribuição e monitoramento de hemocomponentes. Este README documenta as
entregas técnicas já disponíveis na branch e fornece um roteiro reproduzível
para a demonstração.

Os dados usados no projeto são sintéticos e didáticos. Eles não representam
dados reais de doadores ou pacientes e não substituem protocolos clínicos.

## 2. Sumário das entregas

- [E1 — Build Docker com testes e imagem segura](#e1--build-docker-com-testes-e-imagem-segura)
- [Demais entregas](#demais-entregas)
  - [E2 — Modelo para preenchimento](#e2--modelo-para-preenchimento)
  - [E3 — Modelo para preenchimento](#e3--modelo-para-preenchimento)
- [Evidências e prints](#evidências-e-prints)

## 3. Como executar a demonstração

### 3.1 Pré-requisitos

Instalar:

- **Git for Windows**, para clonar o repositório e executar comandos Git.
- **Docker Desktop**, com o mecanismo Docker em execução.

O JDK não é necessário para construir a imagem, pois o Dockerfile usa a
imagem de JDK no estágio de construção. O Docker Desktop deve estar aberto
antes dos comandos abaixo.

### 3.2 Validar o Docker

No PowerShell, executar:

```powershell
docker --version
docker info
```

O primeiro comando exibe a versão instalada. O segundo confirma se o daemon
Docker está acessível. Se `docker info` falhar, iniciar o Docker Desktop antes
de continuar.

### 3.3 Obter o projeto

```powershell
git clone <URL_DO_REPOSITORIO> bloodell-backend
Set-Location bloodell-backend
```

### 3.4 Construir a imagem

```powershell
docker build -t bloodell-backend:e1 .
```

Durante a construção, o estágio de build baixa as dependências, disponibiliza
o código necessário e executa os testes antes de empacotar o JAR. Uma falha
de teste deve fazer o comando terminar com erro e impedir a criação de uma
imagem válida.

### 3.5 Conferir a imagem gerada

```powershell
docker image inspect bloodell-backend:e1
docker history bloodell-backend:e1
```

Esses comandos permitem registrar a imagem criada e suas camadas como
evidência da demonstração.

### 3.6 Executar a aplicação (opcional)

```powershell
docker run --rm -p 8080:8080 --name bloodell-e1 bloodell-backend:e1
```

Em outro PowerShell, verificar a aplicação:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

O resultado esperado é uma resposta de saúde da aplicação. Para encerrar,
pressionar `Ctrl+C` no terminal que executa o container.

## 4. E1 — Build Docker com testes e imagem segura

### Objetivo

Garantir que a imagem Docker só seja produzida depois dos testes unitários e
que a imagem final contenha apenas o necessário para executar a aplicação.
Essa separação reduz o tamanho da imagem e evita levar ferramentas ou arquivos
sensíveis para produção.

### Implementação

O [Dockerfile](./Dockerfile) possui dois estágios:

1. **`builder` (estágio de construção)** — no arquivo, identificado pelo
   alias `build`. Usa uma imagem JDK, prepara o Maven Wrapper e compila a
   aplicação.
2. **Imagem final** — usa uma imagem JRE, que contém somente o ambiente
   necessário para executar o JAR.

No estágio `builder`, são copiados:

- `src/`, com o código-fonte Java;
- `tests/parity/fixtures/`, com as entradas usadas pelo
  `ParidadeAedTest`.

As fixtures são necessárias porque o `ParidadeAedTest` lê esses arquivos para
executar cenários de paridade das estruturas de dados. Uma *fixture* é um
arquivo de entrada fixa usado para repetir o mesmo cenário de teste.

A ordem das etapas é importante:

```text
./mvnw -B -q dependency:go-offline
./mvnw -B test
./mvnw -B -DskipTests package
```

O comando `./mvnw test` é executado antes de gerar ou copiar o JAR. Portanto,
se um teste unitário falhar, o comando `docker build` falha e não deve
produzir uma imagem final considerada válida.

O [.dockerignore](./.dockerignore) exclui do contexto de build:

- `.env`, que pode conter variáveis sensíveis;
- `.git/`;
- artefatos locais, como `target/`, logs e saídas de testes;
- diretórios de build do código acadêmico;
- arquivos de saída de paridade e outros arquivos locais.

Na imagem final são copiados apenas o JRE da imagem-base, o JAR da aplicação e
as dependências necessárias ao runtime. O código-fonte e as ferramentas de
build permanecem no estágio intermediário e não são copiados para a imagem
final. Assim, a imagem final não deve conter:

- código-fonte;
- Maven ou o compilador JDK;
- `.env` ou `.git`;
- segredos ou arquivos pessoais.

### CI e testes de integração

Os testes de integração com PostgreSQL/Testcontainers continuam como gate
obrigatório da CI. *Gate* é uma etapa que precisa passar para que o pipeline
aceite a entrega.

Na configuração de CI, `./mvnw -B verify` executa os testes completos,
incluindo os testes de integração e a paridade C11 x Java. Depois disso, o job
de imagem executa o `docker build` e depende dos jobs de testes.

Os testes com Testcontainers ficam explicitamente na CI porque precisam de um
daemon Docker (o serviço que executa containers). Eles não devem depender de
Docker-in-Docker implícito dentro do build da imagem: o build valida os testes
unitários com `./mvnw test`, enquanto a CI fornece o ambiente apropriado para
PostgreSQL/Testcontainers e executa `./mvnw verify`.

![Workflow da CI aprovado: testes, paridade e build da imagem Docker](docs/evidencias/e1/07-ci-workflow-sucesso.png)

### Passo a passo da evidência

1. Executar `docker --version` e `docker info`.
2. Executar `docker build -t bloodell-backend:e1 .`.
3. Registrar a saída que mostra a execução de `./mvnw -B test`.
4. Registrar a conclusão do empacotamento e a criação da imagem.
5. Executar `docker image inspect` ou `docker history`.
6. Opcionalmente, executar o container e consultar o health endpoint.
7. Consultar a execução da CI para evidenciar os testes de integração e o
   build da imagem como etapas separadas.

### Resultado esperado

- O build termina com sucesso quando os testes unitários passam.
- A imagem `bloodell-backend:e1` é criada.
- A aplicação inicia usando o JAR em uma imagem JRE.
- O build falha se um teste unitário falhar.
- A CI mantém PostgreSQL/Testcontainers e a paridade como gates explícitos.
- A imagem final não leva código-fonte, Maven, compilador, `.env`, `.git` ou
  segredos.

### Evidências relacionadas

- [Dockerfile](./Dockerfile)
- [.dockerignore](./.dockerignore)
- [Workflow de CI](./.github/workflows/ci.yml)
- [Teste de paridade](./src/test/java/br/edu/cesar/bloodell/aed/paridade/ParidadeAedTest.java)
- [Teste de integração PostgreSQL](./src/test/java/br/edu/cesar/bloodell/suporte/TesteDeIntegracao.java)

#### Prints da demonstração

**1. Docker instalado**

![Docker instalado](docs/evidencias/e1/01-docker-version.png)

**2. Docker em execução**

![Docker em execução](docs/evidencias/e1/02-docker-info.png)

**3. Build executando os testes**

![Build executando os testes](docs/evidencias/e1/03-build-executando-testes.png)

**4. Testes concluídos com sucesso**

![Testes concluídos com sucesso](docs/evidencias/e1/04-build-testes-sucesso.png)

**5. Imagem criada no Docker Desktop**

![Imagem criada no Docker Desktop](docs/evidencias/e1/05-imagem-gerada-docker-desktop.png)

**6. Imagem final sem fontes, Maven, compilador, `.env` ou `.git`**

![Imagem final sem fontes, Maven, compilador, .env ou .git](docs/evidencias/e1/06-imagem-final-sem-fontes-ou-segredos.png)

## 5. Demais entregas

As seções abaixo são modelos reservados para as próximas demonstrações.
Preencher cada uma com objetivo, passos, resultado esperado e evidências,
mantendo a mesma linguagem técnica e didática da E1.

### E2 — Modelo para preenchimento

**Objetivo:**
Descrever aqui o objetivo da entrega.

**Passos:**
1. Descrever o primeiro passo.
2. Descrever o segundo passo.

**Resultado esperado:**
Descrever o resultado verificável.

**Evidências:**
Inserir links para arquivos, logs, testes e prints.

### E3 — Modelo para preenchimento

**Objetivo:**
Descrever aqui o objetivo da entrega.

**Passos:**
1. Descrever o primeiro passo.
2. Descrever o segundo passo.

**Resultado esperado:**
Descrever o resultado verificável.

**Evidências:**
Inserir links para arquivos, logs, testes e prints.

### E4 — Modelo para preenchimento

**Objetivo:**
Descrever aqui o objetivo da entrega.

**Passos:**
1. Descrever o primeiro passo.
2. Descrever o segundo passo.

**Resultado esperado:**
Descrever o resultado verificável.

**Evidências:**
Inserir links para arquivos, logs, testes e prints.

## 6. Evidências e prints

Salvar os prints em uma pasta versionada e exclusiva para a documentação, por
exemplo:

```text
docs/evidencias/e1/
docs/evidencias/e2/
docs/evidencias/e3/
```

Usar nomes descritivos e ordenados, como:

```text
01-docker-versao.png
02-docker-info.png
03-build-testes.png
04-imagem-gerada.png
05-ci-testcontainers.png
```

Cada print deve mostrar, quando possível, o comando executado e seu resultado.
Na seção da entrega correspondente, inserir uma legenda curta e o caminho
relativo do arquivo. Não salvar `.env`, tokens, senhas, logs com dados
sensíveis ou qualquer segredo nas evidências.
