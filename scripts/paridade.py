#!/usr/bin/env python3

import argparse
import json
import pathlib
import shutil
import subprocess
import sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent
FIXTURES = RAIZ / "tests" / "parity" / "fixtures"
SAIDA = RAIZ / "target" / "paridade"
CPP = RAIZ / "academic" / "cpp"
RUNNER = CPP / "build" / "runner"

ETAPAS = {"U1": 1, "U2": 2}


def compilar_runner() -> None:
    if shutil.which("make") is None or (shutil.which("g++") is None and shutil.which("clang++") is None):
        erro("Compilador C++ ou make nao encontrado no PATH.\n"
             "        Instale build-essential (Linux), Xcode Command Line Tools (macOS)\n"
             "        ou MSYS2/MinGW (Windows). Para pular a paridade em uma maquina sem\n"
             "        compilador, use: mvnw verify -Psem-paridade (a etapa fica sem evidencia).")
    print("[paridade] compilando o runner C++...")
    resultado = subprocess.run(["make", "tudo"], cwd=CPP, capture_output=True, text=True)
    if resultado.returncode != 0:
        erro("Falha ao compilar o runner C++:\n" + resultado.stdout + resultado.stderr)


def rodar_testes_cpp() -> None:
    print("[paridade] rodando os testes C++ das estruturas...")
    resultado = subprocess.run(["make", "testar"], cwd=CPP, capture_output=True, text=True)
    if resultado.returncode != 0:
        erro("Os testes C++ falharam:\n" + resultado.stdout + resultado.stderr)
    print("          " + resultado.stdout.strip().splitlines()[-1])


def executar_no_cpp(comandos: list[str]) -> list[str]:
    entrada = "\n".join(comandos) + "\n"
    resultado = subprocess.run([str(RUNNER)], input=entrada, capture_output=True, text=True)
    if resultado.returncode != 0:
        erro("O runner C++ terminou com erro:\n" + resultado.stderr)
    return [linha.rstrip() for linha in resultado.stdout.strip().splitlines()]


def comparar(nome: str, cpp: list[str], java: list[str]) -> list[str]:
    divergencias = []
    total = max(len(cpp), len(java))
    for indice in range(total):
        linha_cpp = cpp[indice] if indice < len(cpp) else "<sem linha>"
        linha_java = java[indice] if indice < len(java) else "<sem linha>"
        if linha_cpp != linha_java:
            divergencias.append(
                f"  linha {indice + 1}: C++ = {linha_cpp!r} | Java = {linha_java!r}")
            if len(divergencias) >= 10:
                divergencias.append("  ... (mostrando apenas as 10 primeiras divergencias)")
                break
    return divergencias


def erro(mensagem: str) -> None:
    print("\n[paridade] ERRO: " + mensagem, file=sys.stderr)
    sys.exit(1)


def main() -> None:
    parser = argparse.ArgumentParser(description="Paridade C++ <-> Java das estruturas da AED")
    parser.add_argument("--etapa", default="U1", choices=sorted(ETAPAS),
                        help="etapa academica vigente (padrao: U1)")
    argumentos = parser.parse_args()
    limite = ETAPAS[argumentos.etapa]

    arquivos = sorted(FIXTURES.glob("*.json"))
    if not arquivos:
        erro(f"Nenhuma fixture encontrada em {FIXTURES}.")

    compilar_runner()
    rodar_testes_cpp()
    SAIDA.mkdir(parents=True, exist_ok=True)

    verificadas = 0
    falhas = []

    for arquivo in arquivos:
        fixture = json.loads(arquivo.read_text(encoding="utf-8"))
        nome = fixture["nome"]
        if ETAPAS.get(fixture.get("etapa", "U1"), 9) > limite:
            print(f"[paridade] {nome}: pulada (etapa {fixture['etapa']}, vigente {argumentos.etapa})")
            continue

        saida_java = SAIDA / f"{nome}.java.out"
        if not saida_java.exists():
            falhas.append(
                f"{nome}: saida Java ausente em {saida_java}.\n"
                f"  O teste ParidadeAedTest nao rodou. Execute 'mvnw verify' (e nao apenas 'mvnw test -Dtest=...').")
            continue

        linhas_cpp = executar_no_cpp(fixture["comandos"])
        (SAIDA / f"{nome}.cpp.out").write_text("\n".join(linhas_cpp) + "\n", encoding="utf-8")
        linhas_java = [linha.rstrip() for linha in
                       saida_java.read_text(encoding="utf-8").strip().splitlines()]

        divergencias = comparar(nome, linhas_cpp, linhas_java)
        if divergencias:
            falhas.append(f"{nome}: {len(divergencias)} divergencia(s)\n" + "\n".join(divergencias))
        else:
            verificadas += 1
            print(f"[paridade] {nome}: OK ({len(linhas_cpp)} linhas conferem)")

    if falhas:
        erro("paridade C++ <-> Java quebrada.\n\n" + "\n\n".join(falhas))

    if verificadas == 0:
        erro("nenhuma fixture foi verificada. Paridade sem caso executado nao e evidencia.")

    print(f"\n[paridade] {verificadas} fixture(s) com saida identica nas duas implementacoes.")


if __name__ == "__main__":
    main()
