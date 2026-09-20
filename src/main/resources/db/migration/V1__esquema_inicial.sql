
CREATE TABLE unidades (
    id              BIGSERIAL PRIMARY KEY,
    tipo            VARCHAR(20)  NOT NULL,
    nome            VARCHAR(120) NOT NULL,
    logradouro      VARCHAR(160),
    numero          VARCHAR(20),
    bairro          VARCHAR(80),
    cidade          VARCHAR(80)  NOT NULL,
    uf              CHAR(2)      NOT NULL,
    cep             VARCHAR(9),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_unidades_tipo      CHECK (tipo IN ('HEMOCENTRO', 'HOSPITAL')),
    CONSTRAINT ck_unidades_latitude  CHECK (latitude  BETWEEN -90  AND 90),
    CONSTRAINT ck_unidades_longitude CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX ix_unidades_tipo_nome ON unidades (tipo, nome);

CREATE TABLE doacoes (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(30)  NOT NULL UNIQUE,
    hemocentro_id   BIGINT       NOT NULL REFERENCES unidades (id) ON DELETE RESTRICT,
    data_hora       TIMESTAMPTZ  NOT NULL,
    tipo_abo        VARCHAR(2)   NOT NULL,
    tipo_rh         VARCHAR(1)   NOT NULL,
    volume_ml       INTEGER      NOT NULL,
    CONSTRAINT ck_doacoes_abo    CHECK (tipo_abo IN ('A', 'B', 'AB', 'O')),
    CONSTRAINT ck_doacoes_rh     CHECK (tipo_rh  IN ('+', '-')),
    CONSTRAINT ck_doacoes_volume CHECK (volume_ml BETWEEN 100 AND 1000)
);
CREATE INDEX ix_doacoes_hemocentro_data ON doacoes (hemocentro_id, data_hora);

CREATE TABLE bolsas (
    id                BIGSERIAL PRIMARY KEY,
    codigo            VARCHAR(30)  NOT NULL UNIQUE,
    tipo_abo          VARCHAR(2)   NOT NULL,
    tipo_rh           VARCHAR(1)   NOT NULL,
    componente        VARCHAR(30)  NOT NULL,
    data_coleta       DATE         NOT NULL,
    data_validade     DATE         NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    unidade_atual_id  BIGINT       NOT NULL REFERENCES unidades (id) ON DELETE RESTRICT,
    doacao_id         BIGINT       REFERENCES doacoes (id) ON DELETE RESTRICT,
    sequencia_entrada BIGINT       NOT NULL,
    versao            BIGINT       NOT NULL DEFAULT 0,
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_bolsas_abo        CHECK (tipo_abo IN ('A', 'B', 'AB', 'O')),
    CONSTRAINT ck_bolsas_rh         CHECK (tipo_rh  IN ('+', '-')),
    CONSTRAINT ck_bolsas_componente CHECK (componente IN ('CONCENTRADO_HEMACIAS', 'PLASMA', 'PLAQUETAS')),
    CONSTRAINT ck_bolsas_status     CHECK (status IN ('RECEBIDA','ARMAZENADA','DISPONIVEL','RESERVADA',
                                                      'ALOCADA','DESPACHADA','EM_TRANSPORTE','ENTREGUE',
                                                      'DESCARTADA','VENCIDA')),
    CONSTRAINT ck_bolsas_validade   CHECK (data_validade > data_coleta)
);
CREATE INDEX ix_bolsas_status_validade ON bolsas (status, data_validade);
CREATE INDEX ix_bolsas_unidade_tipo     ON bolsas (unidade_atual_id, tipo_abo, tipo_rh, componente);
CREATE INDEX ix_bolsas_sequencia        ON bolsas (sequencia_entrada);

CREATE TABLE estoque_itens (
    id            BIGSERIAL PRIMARY KEY,
    unidade_id    BIGINT      NOT NULL REFERENCES unidades (id) ON DELETE RESTRICT,
    tipo_abo      VARCHAR(2)  NOT NULL,
    tipo_rh       VARCHAR(1)  NOT NULL,
    componente    VARCHAR(30) NOT NULL,
    quantidade    INTEGER     NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_estoque_item        UNIQUE (unidade_id, tipo_abo, tipo_rh, componente),
    CONSTRAINT ck_estoque_quantidade  CHECK (quantidade >= 0)
);

CREATE TABLE requisicoes (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(30) NOT NULL UNIQUE,
    hospital_id BIGINT      NOT NULL REFERENCES unidades (id) ON DELETE RESTRICT,
    prioridade  VARCHAR(15) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    criada_em   TIMESTAMPTZ NOT NULL DEFAULT now(),
    atendida_em TIMESTAMPTZ,
    versao      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_requisicoes_prioridade CHECK (prioridade IN ('ELETIVA', 'URGENTE', 'EMERGENCIA')),
    CONSTRAINT ck_requisicoes_status     CHECK (status IN ('CRIADA','NA_FILA','EM_PROCESSAMENTO','ALOCADA',
                                                           'EM_ROTA','ATENDIDA','REJEITADA','CANCELADA'))
);
CREATE INDEX ix_requisicoes_status_prioridade ON requisicoes (status, prioridade, criada_em);

CREATE TABLE itens_requisicao (
    id                 BIGSERIAL PRIMARY KEY,
    requisicao_id      BIGINT      NOT NULL REFERENCES requisicoes (id) ON DELETE CASCADE,
    componente         VARCHAR(30) NOT NULL,
    tipo_abo           VARCHAR(2)  NOT NULL,
    tipo_rh            VARCHAR(1)  NOT NULL,
    quantidade         INTEGER     NOT NULL,
    quantidade_alocada INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT ck_itens_quantidade  CHECK (quantidade > 0),
    CONSTRAINT ck_itens_alocada     CHECK (quantidade_alocada >= 0 AND quantidade_alocada <= quantidade)
);
CREATE INDEX ix_itens_requisicao ON itens_requisicao (requisicao_id);

CREATE TABLE veiculos (
    id                  BIGSERIAL PRIMARY KEY,
    placa               VARCHAR(10) NOT NULL UNIQUE,
    hemocentro_base_id  BIGINT      NOT NULL REFERENCES unidades (id) ON DELETE RESTRICT,
    capacidade_bolsas   INTEGER     NOT NULL,
    status              VARCHAR(20) NOT NULL,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    CONSTRAINT ck_veiculos_status     CHECK (status IN ('DISPONIVEL', 'EM_ROTA', 'MANUTENCAO')),
    CONSTRAINT ck_veiculos_capacidade CHECK (capacidade_bolsas > 0)
);
CREATE INDEX ix_veiculos_status ON veiculos (status);

CREATE TABLE entregas (
    id                 BIGSERIAL PRIMARY KEY,
    requisicao_id      BIGINT      NOT NULL REFERENCES requisicoes (id) ON DELETE RESTRICT,
    veiculo_id         BIGINT      REFERENCES veiculos (id) ON DELETE RESTRICT,
    origem_id          BIGINT      NOT NULL REFERENCES unidades (id) ON DELETE RESTRICT,
    status             VARCHAR(20) NOT NULL,
    rota               JSONB,
    distancia_km       NUMERIC(10, 3),
    tempo_estimado_min INTEGER,
    iniciada_em        TIMESTAMPTZ,
    concluida_em       TIMESTAMPTZ,
    CONSTRAINT ck_entregas_status CHECK (status IN ('PLANEJADA','DESPACHADA','EM_TRANSPORTE','ENTREGUE','CANCELADA'))
);
CREATE INDEX ix_entregas_status     ON entregas (status);
CREATE INDEX ix_entregas_requisicao ON entregas (requisicao_id);

CREATE TABLE eventos_rastreabilidade (
    id              BIGSERIAL PRIMARY KEY,
    bolsa_id        BIGINT      NOT NULL REFERENCES bolsas (id) ON DELETE RESTRICT,
    status_anterior VARCHAR(20),
    status_novo     VARCHAR(20) NOT NULL,
    requisicao_id   BIGINT      REFERENCES requisicoes (id) ON DELETE RESTRICT,
    entrega_id      BIGINT      REFERENCES entregas (id) ON DELETE RESTRICT,
    descricao       VARCHAR(255),
    ocorrido_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_eventos_bolsa_momento ON eventos_rastreabilidade (bolsa_id, ocorrido_em);

CREATE TABLE leituras_telemetria (
    id             BIGSERIAL PRIMARY KEY,
    veiculo_id     BIGINT        NOT NULL REFERENCES veiculos (id) ON DELETE RESTRICT,
    entrega_id     BIGINT        REFERENCES entregas (id) ON DELETE RESTRICT,
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    temperatura_c  NUMERIC(5, 2) NOT NULL,
    status_veiculo VARCHAR(20)   NOT NULL,
    registrada_em  TIMESTAMPTZ   NOT NULL,
    recebida_em    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    correlacao     VARCHAR(40),
    CONSTRAINT ck_telemetria_temperatura CHECK (temperatura_c BETWEEN -40 AND 60)
);
CREATE INDEX ix_telemetria_veiculo_momento ON leituras_telemetria (veiculo_id, registrada_em);

CREATE TABLE alertas (
    id           BIGSERIAL PRIMARY KEY,
    tipo         VARCHAR(30)  NOT NULL,
    nivel        VARCHAR(15)  NOT NULL,
    mensagem     VARCHAR(255) NOT NULL,
    veiculo_id   BIGINT REFERENCES veiculos (id) ON DELETE RESTRICT,
    entrega_id   BIGINT REFERENCES entregas (id) ON DELETE RESTRICT,
    bolsa_id     BIGINT REFERENCES bolsas (id)   ON DELETE RESTRICT,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolvido_em TIMESTAMPTZ,
    CONSTRAINT ck_alertas_nivel CHECK (nivel IN ('INFO', 'ATENCAO', 'CRITICO'))
);
CREATE INDEX ix_alertas_abertos ON alertas (resolvido_em, criado_em);

CREATE TABLE usuarios (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL UNIQUE,
    perfil     VARCHAR(30)  NOT NULL,
    senha_hash VARCHAR(100),
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('OPERADOR_HEMOCENTRO', 'USUARIO_HOSPITAL', 'LOGISTICA', 'ADMIN'))
);

CREATE SEQUENCE seq_entrada_bolsa START 1 INCREMENT 1;
