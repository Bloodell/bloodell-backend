"""Captura evidências datadas do Compose local na CI. Requer playwright e Chrome."""
from datetime import datetime, timezone
from html import escape
from pathlib import Path
from urllib.parse import urlencode
import json
import os
import subprocess
from playwright.sync_api import sync_playwright

PASTA = Path("docs/evidencias/observabilidade")
PASTA.mkdir(exist_ok=True, parents=True)
BASE = "http://localhost:3000"


def interno(url):
    return subprocess.check_output(["docker", "compose", "exec", "-T", "api", "curl", "-fsS", url], text=True)


with sync_playwright() as playwright:
    browser = playwright.chromium.launch(channel="chrome", args=["--no-sandbox"])
    page = browser.new_page(viewport={"width": 1600, "height": 1000}, device_scale_factor=1)
    login = page.request.post(BASE + "/login", data={"user": "admin", "password": os.environ["GRAFANA_ADMIN_PASSWORD"]})
    assert login.ok, login.status

    def salvar(nome):
        page.evaluate("""stamp => {
            document.getElementById('evidence-stamp')?.remove();
            const p = document.createElement('div');
            p.id = 'evidence-stamp';
            p.textContent = stamp;
            p.style.cssText = 'position:fixed;bottom:4px;right:4px;background:white;color:black;padding:6px;z-index:99999;font:16px monospace';
            document.body.appendChild(p);
        }""", "BLOODELL · " + datetime.now(timezone.utc).isoformat(timespec="seconds"))
        page.screenshot(path=PASTA / (nome + ".png"))

    def texto(nome, linhas):
        page.set_content("<body style='font:20px monospace;padding:22px'><h2>" + escape(nome) +
                         "</h2><pre style='white-space:pre-wrap'>" + escape(linhas) + "</pre></body>")
        salvar(nome)

    texto("01-Compose", subprocess.check_output(["docker", "compose", "ps", "--format",
          "table {{.Service}}\t{{.State}}\t{{.Status}}"], text=True))
    scrape = interno("http://localhost:8081/actuator/prometheus")
    linhas = scrape.splitlines()
    amostras = []
    for nome in ("bloodell_doacoes_registradas_total", "bloodell_bolsas_geradas_total",
                 "jvm_threads_live_threads", "process_uptime_seconds", "hikaricp_connections_active",
                 "http_server_requests_seconds_count"):
        amostras.append(next(l for l in linhas if l.startswith(nome + "{")))
    texto("02-Actuator", "\n\n".join(amostras))
    targets = json.loads(interno("http://prometheus:9090/api/v1/targets"))["data"]["activeTargets"]
    texto("03-Prometheus-targets", "\n\n".join(t["scrapeUrl"] + "  " + t["health"] for t in targets))
    query = "sum by (job) (up)"
    dados = json.loads(interno("http://prometheus:9090/api/v1/query?" + urlencode({"query": query})))
    texto("04-PromQL", query + "\n\n" + "\n".join(
        f'{amostra["metric"]["job"]} = {amostra["value"][1]}' for amostra in dados["data"]["result"]))
    for uid in ("aplicacao", "infraestrutura"):
        page.goto(f"{BASE}/d/bloodell-{uid}/{uid}?from=now-15m&to=now")
        page.get_by_text("Requisições por rota" if uid == "aplicacao" else "Disponibilidade dos targets", exact=True).first.wait_for()
        page.wait_for_timeout(3500)
        salvar("05-Dashboard-" + uid)
    consulta = '{application="bloodell-backend"} |= "Rastreabilidade consultada"'
    stream = json.loads(interno("http://loki:3100/loki/api/v1/query_range?" + urlencode({"query": consulta, "limit": 1})))
    evento = json.loads(stream["data"]["result"][0]["values"][0][1])
    texto("06-Loki", consulta + "\n\n" + json.dumps(evento, ensure_ascii=False, indent=2))
    query = {"refId": "A", "datasource": {"type": "loki", "uid": "loki"}, "expr": consulta, "queryType": "range"}
    pane = {"logs": {"datasource": "loki", "queries": [query], "range": {"from": "now-15m", "to": "now"}}}
    page.goto(BASE + "/explore?" + urlencode({"schemaVersion": "1", "panes": json.dumps(pane)}))
    page.get_by_text("Rastreabilidade consultada", exact=False).last.wait_for()
    salvar("06-Explore-LogQL")
    trace_id = evento["traceId"]
    trace = json.loads(interno("http://tempo:3200/api/traces/" + trace_id))
    spans = [s for lote in trace["batches"] for grupo in lote["scopeSpans"] for s in grupo["spans"]]
    texto("07-Tempo", trace_id + "\n\n" + "\n".join(s["name"] + "  " + s["spanId"] for s in spans))
    query = {"refId": "A", "datasource": {"type": "tempo", "uid": "tempo"}, "queryType": "traceql", "query": trace_id}
    pane = {"trace": {"datasource": "tempo", "queries": [query], "range": {"from": "now-15m", "to": "now"}}}
    page.goto(BASE + "/explore?" + urlencode({"schemaVersion": "1", "panes": json.dumps(pane)}))
    page.get_by_text("bloodell.bolsa.rastreabilidade", exact=False).first.wait_for()
    salvar("07-Explore-Tempo")
    browser.close()
