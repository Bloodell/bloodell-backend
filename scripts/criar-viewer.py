"""Cria (ou atualiza) a conta Viewer do professor no Grafana e comprova o acesso somente leitura.

Uso na EC2, na raiz do repositório:
    set -a; . ./.env; set +a; python3 scripts/criar-viewer.py

Verificação externa (sem senha de administrador), por exemplo de outra rede:
    GRAFANA_URL=https://HOST/grafana GRAFANA_VIEWER_LOGIN=professor GRAFANA_VIEWER_SENHA=... \
        python3 scripts/criar-viewer.py --somente-verificar

Variáveis: GRAFANA_ADMIN_PASSWORD, GRAFANA_VIEWER_LOGIN, GRAFANA_VIEWER_SENHA,
GRAFANA_URL (padrão: loopback 3000 com o subcaminho de GRAFANA_ROOT_URL).
"""
import base64
import json
import os
import sys
from urllib.error import HTTPError
from urllib.parse import quote, urlparse
from urllib.request import Request, urlopen


def base_padrao():
    caminho = urlparse(os.getenv("GRAFANA_ROOT_URL", "http://localhost:3000/")).path.rstrip("/")
    return "http://127.0.0.1:3000" + caminho


BASE = os.getenv("GRAFANA_URL", base_padrao()).rstrip("/")
LOGIN = os.getenv("GRAFANA_VIEWER_LOGIN", "professor")
SENHA = os.getenv("GRAFANA_VIEWER_SENHA", "")


def chamar(metodo, rota, usuario, senha, dados=None):
    credencial = base64.b64encode(f"{usuario}:{senha}".encode()).decode()
    corpo = json.dumps(dados).encode() if dados is not None else None
    requisicao = Request(BASE + rota, data=corpo, method=metodo, headers={
        "Authorization": "Basic " + credencial, "Content-Type": "application/json"})
    try:
        with urlopen(requisicao, timeout=15) as resposta:
            return resposta.status, json.loads(resposta.read() or b"null")
    except HTTPError as erro:
        return erro.code, None


def provisionar():
    admin = os.environ["GRAFANA_ADMIN_PASSWORD"]
    status, usuario = chamar("GET", "/api/users/lookup?loginOrEmail=" + quote(LOGIN), "admin", admin)
    if status == 404:
        status, criado = chamar("POST", "/api/admin/users", "admin", admin,
                                {"name": "Professor (Viewer)", "login": LOGIN, "password": SENHA})
        assert status == 200, f"falha ao criar a conta: HTTP {status}"
        identificador = criado["id"]
        print("Conta criada:", LOGIN)
    else:
        assert status == 200, f"falha ao consultar a conta: HTTP {status}"
        identificador = usuario["id"]
        status, _ = chamar("PUT", f"/api/admin/users/{identificador}/password", "admin", admin, {"password": SENHA})
        assert status == 200, f"falha ao redefinir a senha: HTTP {status}"
        print("Conta existente atualizada:", LOGIN)
    status, _ = chamar("PATCH", f"/api/org/users/{identificador}", "admin", admin, {"role": "Viewer"})
    assert status == 200, f"falha ao aplicar o papel Viewer: HTTP {status}"


def verificar():
    status, organizacoes = chamar("GET", "/api/user/orgs", LOGIN, SENHA)
    assert status == 200, f"login do Viewer falhou: HTTP {status}"
    papeis = {org["role"] for org in organizacoes}
    assert papeis == {"Viewer"}, f"papel inesperado: {papeis}"
    for uid in ("bloodell-aplicacao", "bloodell-infraestrutura"):
        status, painel = chamar("GET", "/api/dashboards/uid/" + uid, LOGIN, SENHA)
        assert status == 200, f"dashboard {uid} inacessível: HTTP {status}"
        assert not painel["meta"].get("canSave"), f"Viewer pode salvar {uid}"
    status, _ = chamar("POST", "/api/dashboards/db", LOGIN, SENHA,
                       {"dashboard": {"title": "teste-viewer"}, "overwrite": False})
    assert status == 403, f"Viewer conseguiu salvar um dashboard: HTTP {status}"
    consulta = {"from": "now-5m", "to": "now", "queries": [{
        "refId": "A", "datasource": {"uid": "prometheus"}, "instant": True,
        "expr": 'time() - max(timestamp(up{job="bloodell-backend"} == 1))'}]}
    status, resultado = chamar("POST", "/api/ds/query", LOGIN, SENHA, consulta)
    assert status == 200, f"Viewer não consulta o Prometheus: HTTP {status}"
    idade = resultado["results"]["A"]["frames"][0]["data"]["values"][1][0]
    assert idade < 120, f"última coleta da API tem {idade:.0f}s"
    print(f"Viewer OK em {BASE}: papel Viewer, dashboards legíveis, gravação negada (403), "
          f"última coleta da API há {idade:.0f}s.")


if __name__ == "__main__":
    if not SENHA:
        sys.exit("defina GRAFANA_VIEWER_SENHA")
    if "--somente-verificar" not in sys.argv:
        provisionar()
    verificar()
