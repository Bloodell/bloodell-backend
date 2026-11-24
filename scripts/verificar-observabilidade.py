"""Confere as fontes reais pela rede interna usando o curl da imagem da API."""
import json
import subprocess
import time
from urllib.parse import urlencode


def buscar(url):
    resultado = subprocess.check_output(
        ["docker", "compose", "exec", "-T", "api", "curl", "-fsS", "--max-time", "8", url],
        text=True,
    )
    return json.loads(resultado)


def validar():
    alvos = buscar("http://prometheus:9090/api/v1/targets")["data"]["activeTargets"]
    ativos = {alvo["labels"]["job"] for alvo in alvos if alvo["health"] == "up"}
    assert ativos == {"bloodell-backend", "prometheus", "node-exporter", "cadvisor"}, ativos
    for expressao in (
        'bloodell_doacoes_registradas_total{application="bloodell-backend"} > 0',
        'bloodell_bolsas_geradas_total{application="bloodell-backend"} > 0',
        "http_server_requests_seconds_bucket", "hikaricp_connections_active",
        "node_memory_MemAvailable_bytes", 'container_memory_working_set_bytes{container_label_com_docker_compose_service="api"}',
    ):
        consulta = urlencode({"query": expressao})
        assert buscar("http://prometheus:9090/api/v1/query?" + consulta)["data"]["result"], expressao

    consulta = urlencode({"query": '{application="bloodell-backend"} |= "Rastreabilidade consultada"', "limit": 5})
    streams = buscar("http://loki:3100/loki/api/v1/query_range?" + consulta)["data"]["result"]
    assert streams, "Loki ainda sem eventos"
    trace_id = json.loads(streams[0]["values"][0][1])["traceId"]
    trace = buscar("http://tempo:3200/api/traces/" + trace_id)
    spans = [span for lote in trace["batches"] for grupo in lote["scopeSpans"] for span in grupo["spans"]]
    assert any(s.get("parentSpanId") and "rastreabilidade" in s["name"] for s in spans), "span filho ausente"
    assert len(spans) >= 2
    print("Targets, métricas, Loki e trace com spans confirmados:", trace_id)


if __name__ == "__main__":
    for tentativa in range(36):
        try:
            validar()
            break
        except (AssertionError, KeyError, ValueError, subprocess.CalledProcessError) as erro:
            if tentativa == 35:
                raise
            print("Aguardando dados:", erro, flush=True)
            time.sleep(5)
