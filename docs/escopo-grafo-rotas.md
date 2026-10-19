# W02 — Escopo do grafo de rotas

## Objetivo

Estimar a ordem de visita e a distância geográfica de uma entrega entre um
hemocentro de origem e o hospital associado à requisição. O sistema já mantém
coordenadas nas unidades e oferece a fórmula de Haversine em `CoordenadaGeo`.

## Modelo do grafo

- **Vértice:** local identificado por unidade, tipo, nome e coordenadas
  geográficas. No fluxo atual, os vértices da entrega são o hemocentro e um
  hospital.
- **Aresta:** ligação entre qualquer par de vértices; o grafo é completo e não
  direcionado.
- **Peso:** distância em quilômetros calculada por Haversine. O peso é simétrico
  e não negativo.
- **Origem e destino:** a origem é o hemocentro salvo na entrega; o destino é o
  hospital da requisição associada. A interface do roteirizador aceita vários
  destinos para permitir a ordenação genérica, mas a entrega atual contém um
  único hospital.

Este modelo representa distância em linha reta entre coordenadas. Não modela
ruas, sentidos de circulação, trânsito, restrições de veículo ou condições de
acesso; portanto, a distância calculada é uma estimativa geográfica, não a
distância rodoviária real.

## Algoritmo e contrato

`Roteirizador.calcularOrdemDeVisita(...)` aplica a heurística do vizinho mais
próximo:

1. começa na coordenada de origem;
2. seleciona o destino não visitado com menor distância a partir da posição
   atual;
3. em empate, escolhe o destino que aparece primeiro nos arrays de entrada;
4. repete até visitar todos os destinos e encerra no último, sem retornar à
   origem.

O retorno é um vetor de índices dos destinos, com base zero. Arrays de
coordenadas nulos, de comprimentos diferentes, valores não finitos ou
coordenadas fora dos intervalos válidos são rejeitados com
`IllegalArgumentException`. Nenhum destino é removido ou alterado.

Para `n` destinos, a heurística consome O(n²) tempo e O(n) espaço auxiliar. É
uma heurística de ordenação; não garante o menor percurso global.

## Integração com entregas

`POST /api/v1/entregas/{id}/roteirizacao` calcula e salva a rota apenas enquanto
a entrega está `PLANEJADA`. A resposta mantém o contrato `EntregaResposta`, com:

- `rota`: JSON com `paradas`, contendo ordem, tipo, ID, nome e coordenadas de
  cada unidade. A origem recebe ordem 0 e o hospital ordem 1;
- `distanciaKm`: distância Haversine entre a origem e o destino, arredondada a
  três casas decimais, de acordo com a precisão já usada pela entidade;
- `tempoEstimadoMin`: preservado como estava, pois não existe no domínio uma
  velocidade média ou fonte de trânsito para derivar esse valor.

O cálculo sobre uma entrega inexistente usa o erro padrão 404. A tentativa de
roteirizar uma entrega em outro estado segue a regra de negócio padrão e
responde 422. A rota pode ser recalculada enquanto a entrega continuar
planejada.
