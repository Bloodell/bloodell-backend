# Gera as evidencias de container da E1-04 em docs/evidencias/e1/ (PowerShell).
#
# Pressupoe o ambiente ja de pe e saudavel:
#
#   docker compose up --build -d --wait
#   .\scripts\gerar-evidencias.ps1
#
# Cada arquivo leva o commit e a data no cabecalho. Nada de ambiente entra aqui:
# nao chamamos `docker compose config` nem `env`, que imprimiriam as senhas do .env.

$ErrorActionPreference = 'Stop'
$raiz = Resolve-Path (Join-Path $PSScriptRoot '..')
$destino = Join-Path $raiz 'docs\evidencias\e1'
New-Item -ItemType Directory -Force $destino | Out-Null

$commit = git -C $raiz rev-parse --short HEAD
$assunto = git -C $raiz log -1 --format=%s
$branch = git -C $raiz rev-parse --abbrev-ref HEAD
# So conta o que entra no build: mudanca em docs nao altera o que esta rodando.
$sujo = if (git -C $raiz status --porcelain -- src pom.xml Dockerfile compose.yaml .mvn) { ' (com alteracoes locais)' } else { '' }
$data = Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'

function Cabecalho($titulo, $comando) {
    @(
        "# $titulo"
        "# Commit : $commit$sujo - $assunto"
        "# Branch : $branch"
        "# Data   : $data"
        "# Comando: $comando"
        ''
    )
}

# Remove qualquer coisa que pareca segredo, por garantia.
function Higienizar($linhas) {
    $linhas | ForEach-Object { $_ -replace '(?i)(password|senha|secret|token)(\s*[=:]\s*)\S+', '$1$2***' }
}

function Gravar($arquivo, $conteudo) {
    $caminho = Join-Path $destino $arquivo
    [IO.File]::WriteAllLines($caminho, [string[]](Higienizar $conteudo), (New-Object Text.UTF8Encoding $false))
    Write-Host "gerado: docs/evidencias/e1/$arquivo"
}

$ps = docker compose --project-directory $raiz ps --format 'table {{.Service}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
if ($LASTEXITCODE -ne 0) { throw 'docker compose ps falhou' }
if (($ps | Select-String '\(healthy\)').Count -lt 2) {
    throw "API e Postgres precisam estar (healthy). Rode: docker compose up --build -d --wait"
}
Gravar '07-compose-ps.txt' ((Cabecalho 'API e PostgreSQL saudaveis' 'docker compose ps') + $ps)

$saida = @()
foreach ($rota in '/actuator/health', '/api/v1/algoritmos/situacao') {
    $saida += "PS> curl.exe -i http://127.0.0.1:8080$rota"
    $saida += curl.exe --silent --show-error --include "http://127.0.0.1:8080$rota"
    if ($LASTEXITCODE -ne 0) { throw "a API nao respondeu em $rota" }
    $saida += ''
    $saida += ''
}
Gravar '08-api-resposta.txt' ((Cabecalho 'API respondendo' 'curl.exe -i (health check e rota da API)') + $saida)
