# Verificacao completa do backend, no Windows (PowerShell).
#
# Roda tudo o que o CI roda e guarda a saida em um arquivo de log, para que dê
# para mandar o erro inteiro para alguém quando algo quebrar.
#
#   cd C:\dev\bloodell\bloodell-backend
#   .\scripts\verificar.ps1
#
# O log fica em .\verificacao.log (ignorado pelo Git).

$ErrorActionPreference = 'Continue'
$log = Join-Path $PSScriptRoot '..\verificacao.log'
Remove-Item $log -ErrorAction SilentlyContinue

function Etapa($titulo, $bloco) {
    Write-Host ""
    Write-Host "=== $titulo ===" -ForegroundColor Cyan
    "`n=== $titulo ===" | Out-File -FilePath $log -Append -Encoding utf8
    & $bloco 2>&1 | Tee-Object -FilePath $log -Append
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FALHOU: $titulo (codigo $LASTEXITCODE)" -ForegroundColor Red
        Write-Host "Log completo em: $log"
        exit $LASTEXITCODE
    }
}

Etapa "Versoes das ferramentas" { java -version; .\mvnw.cmd -v }

# 'verify' encadeia: testes rapidos -> testes de integracao (precisam de Docker)
# -> paridade C++ x Java. A primeira execucao baixa o Maven e as dependencias.
Etapa "Build, testes e paridade" { .\mvnw.cmd -B verify }

Write-Host ""
Write-Host "Tudo verde." -ForegroundColor Green
Write-Host "Log em: $log"
