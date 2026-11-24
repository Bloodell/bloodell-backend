"""Gera tráfego sintético na rede interna do Compose."""
import json
import os
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

API = "http://api:8080/api/v1"
EMAIL = os.getenv("CARGA_EMAIL")
INTERVALO = max(60, int(os.getenv("CARGA_INTERVALO_DOACAO", "3600")))


def chamar(rota, dados=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    corpo = json.dumps(dados).encode() if dados is not None else None
    with urlopen(Request(API + rota, data=corpo, headers=headers), timeout=12) as resposta:
        return json.load(resposta)


def executar():
    token = None
    renovar = 0
    proxima_doacao = 0
    bolsa = None
    ciclo = 0
    while True:
        try:
            agora = time.monotonic()
            if EMAIL and agora >= renovar:
                login = chamar("/auth/login", {"email": EMAIL, "senha": os.environ["CARGA_SENHA"]})
                token = login["accessToken"]
                renovar = agora + max(1, login["expiresIn"] - 30)

            if agora >= proxima_doacao:
                unidade = os.getenv("CARGA_HEMOCENTRO_ID")
                if not unidade:
                    unidades = chamar("/hemocentros", token=token)
                    if not unidades and os.getenv("CARGA_SEED") == "true":
                        chamar("/demonstracoes/cenarios", {"cenario": "aula", "semente": 42}, token)
                        unidades = chamar("/hemocentros", token=token)
                    if os.getenv("CARGA_SEED") != "true":
                        raise ValueError("configure a unidade sintética")
                    unidade = next((u["id"] for u in unidades if u["nome"] == "Hemocentro Central Recife"), None)
                    if unidade is None:
                        raise ValueError("configure a unidade sintética")
                doacao = chamar("/doacoes", {
                    "hemocentroId": int(unidade), "tipoSanguineo": "O+", "volumeMl": 450,
                    "componentes": ["PLASMA", "CONCENTRADO_HEMACIAS"]
                }, token)
                bolsa = doacao["bolsasGeradas"][0]["id"]
                proxima_doacao = agora + INTERVALO
            chamar(f"/bolsas/{bolsa}/rastreabilidade", token=token)
            ciclo += 1
            if ciclo % 10 == 0:
                try:
                    chamar("/bolsas/0/rastreabilidade", token=token)
                except HTTPError as erro:
                    if erro.code != 404:
                        raise
        except HTTPError as erro:
            print("API respondeu", erro.code, flush=True)
            if erro.code == 401:
                renovar = 0
        except (IndexError, KeyError, ValueError, URLError, TimeoutError) as erro:
            print("Carga aguardando a API:", type(erro).__name__, flush=True)
        time.sleep(30)


if __name__ == "__main__":
    executar()
