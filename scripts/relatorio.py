"""Gera o relatório PDF. Uso: python scripts/relatorio.py (requer reportlab)."""
from io import BytesIO
from pathlib import Path
from xml.sax.saxutils import escape
import os
from reportlab.graphics.shapes import Drawing, String, Rect, Line, Polygon
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Image

ROOT = Path(__file__).resolve().parents[1]
EVIDENCIAS = ROOT / "docs/evidencias/observabilidade"
destino = ROOT / "docs/relatorio.pdf"
folha = landscape(A4)
doc = SimpleDocTemplate(str(destino), pagesize=folha, title="BLOODELL | Observabilidade",
                        leftMargin=40, rightMargin=40, topMargin=36, bottomMargin=36)
estilos = getSampleStyleSheet()
itens = []


def linha(texto, estilo="BodyText"):
    itens.extend([Paragraph(texto, estilos[estilo]), Spacer(1, 6)])


def tabela(dados, larguras):
    conteudo = [[Paragraph(escape(celula), estilos["BodyText"]) for celula in row] for row in dados]
    tabela = Table(conteudo, colWidths=larguras, hAlign="LEFT")
    tabela.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#d5eee9")),
                                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f1f5f5")]),
                                ("TOPPADDING", (0, 0), (-1, -1), 7), ("BOTTOMPADDING", (0, 0), (-1, -1), 7)]))
    itens.append(tabela)


linha("BLOODELL", "Title")
linha("Infraestrutura de Comunicação · Projeto Integrador", "Heading1")
linha("CESAR School · Análise e Desenvolvimento de Sistemas · 2026.2")
linha("Equipe", "Heading2")
linha("Alisson da Silva Santana · Arthur Abelardo Campos da Costa · Carlos Henrique Cavalcante Farias · "
      "Danilo Gabriel Diniz e Silva · Gabriel Gomes de Andrade · Roberto Henrique Cavalcanti Freitas")
linha('<link href="https://github.com/Bloodell/bloodell-backend">github.com/Bloodell/bloodell-backend</link>')
linha("Aplicação instrumentada", "Heading2")
linha("A API Java registra doações sintéticas, gera bolsas, movimenta estoque e mantém a rastreabilidade. "
      "Uma consulta de histórico atravessa o servidor HTTP e o serviço de bolsas. O mesmo traceId identifica "
      "o registro estruturado no Loki e os spans exportados por OTLP ao Tempo.")
linha("Acesso remoto", "Heading2")
linha("Endereço previsto: https://34.235.158.105/grafana/. A equipe entrega as credenciais Viewer pelo Classroom. "
      "Janela oficial e validação externa da conta pendentes de confirmação.")
itens.append(PageBreak())
linha("Arquitetura e fluxos de comunicação", "Heading1")
mapa = Drawing(725, 88)
for indice, nome in enumerate(["Navegador", "Nginx", "Grafana", "Prometheus", "API e exporters"]):
    x = 145 * indice
    mapa.add(Rect(x, 26, 120, 34, fillColor=colors.HexColor("#e9f4f2"), strokeColor=colors.HexColor("#1c6267")))
    mapa.add(String(x + 60, 39, nome, fontSize=12, textAnchor="middle"))
    if indice < 4:
        mapa.add(Line(x + 120, 43, x + 142, 43))
        mapa.add(Polygon([x + 143, 43, x + 136, 46, x + 136, 40], fillColor=colors.black))
itens.append(mapa)
tabela([["Origem", "Fluxo", "Destino"],
        ["Professor", "HTTPS 443 → Nginx → loopback 3000", "Grafana"],
        ["Gerador de carga", "HTTP 8080 → API BLOODELL → SQL 5432", "PostgreSQL"],
        ["Prometheus", "Scrape a cada 15 s → Actuator 8081, Node Exporter 9100 e cAdvisor 8080", "Métricas da JVM, host e contêineres"],
        ["API BLOODELL", "Arquivo JSON → Alloy → HTTP 3100", "Loki"],
        ["API BLOODELL", "Spans HTTP e serviço → OTLP/HTTP 4318", "Tempo"],
        ["Grafana", "Consulta → Prometheus 9090, Loki 3100, Tempo 3200", "Dashboards e investigação"]], [145, 310, 275])
linha("Métricas de negócio", "Heading2")
tabela([["Série Micrometer no Prometheus", "Tipo", "Evento de domínio"],
        ["bloodell_doacoes_registradas_total", "Counter", "Uma doação confirmada pelo banco"],
        ["bloodell_bolsas_geradas_total", "Counter", "Número de bolsas geradas pela doação confirmada"]], [295, 100, 335])
itens.append(PageBreak())
linha("Validação e lições da integração", "Heading1")
linha("O healthcheck que existia no workflow usava uma rota pública. Separar a gestão na porta 8081 exigiu "
      "alterar a segurança da aplicação, o healthcheck Docker e os critérios do deploy no mesmo conjunto de mudanças. "
      "A validação externa exige resposta do Grafana e bloqueio de /actuator/health.")
linha("O teste de integração cria uma doação com dois componentes e força o rollback de outra transação. "
      "Os incrementos esperados são uma doação e duas bolsas. O listener após commit evita contabilizar tentativas revertidas.")
linha("Na primeira execução Linux, os quatro targets responderam, mas faltava a série do contêiner da API "
      "com o label do Compose no cAdvisor. Por isso a verificação consulta as séries por serviço e abre no Tempo "
      "o identificador extraído de um log real. Um target UP, sozinho, comprova apenas a comunicação com o exporter.")
linha("As primeiras capturas foram feitas logo após a subida: painéis com rate() mostravam \"No data\" porque "
      "ainda não havia duas coletas na janela. A captura agora aguarda histórico de coleta, tráfego HTTP e bolsas "
      "geradas antes de abrir os dashboards, e a conta Viewer é verificada por script (papel, leitura e gravação negada).")
linha('<link href="https://github.com/Bloodell/bloodell-backend/actions/runs/45408465874">Primeira execução Linux e resultados da integração Java e C11</link>')
imagens = sorted(EVIDENCIAS.glob("*.png"))
if not imagens:
    linha("Evidências visuais pendentes", "Heading2")
    linha("Esta edição parcial documenta a implementação. As capturas datadas da stack e a confirmação "
          "do acesso externo Viewer são necessárias para a entrega final.")
def fatias(caminho):
    """Divide capturas altas (dashboards inteiros) em partes legíveis na página."""
    from PIL import Image as Pil
    figura = Pil.open(caminho)
    passo = int(figura.width * 445 / 744)
    if figura.height <= passo * 1.15:
        return [figura]
    return [figura.crop((0, topo, figura.width, min(topo + passo, figura.height)))
            for topo in range(0, figura.height, passo)]


for caminho in imagens:
    partes = fatias(caminho)
    for indice, parte in enumerate(partes, 1):
        itens.append(PageBreak())
        sufixo = f" ({indice}/{len(partes)})" if len(partes) > 1 else ""
        linha(escape(caminho.stem.replace("-", " ")) + sufixo, "Heading2")
        largura, altura = parte.size
        escala = min(744 / largura, 445 / altura)
        buffer = BytesIO()
        parte.save(buffer, format="PNG")
        buffer.seek(0)
        itens.append(Image(buffer, largura * escala, altura * escala))
if os.getenv("GITHUB_RUN_ID"):
    linha("Execução CI: " + escape(os.environ["GITHUB_RUN_ID"]))
doc.build(itens)
print(destino)
