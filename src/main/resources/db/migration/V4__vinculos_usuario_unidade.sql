-- Sem backfill: contas existentes precisam de atribuicao explicita por ADMIN.
CREATE TABLE usuario_unidades (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    unidade_id BIGINT NOT NULL REFERENCES unidades(id) ON DELETE RESTRICT,
    PRIMARY KEY (usuario_id, unidade_id)
);
CREATE INDEX idx_usuario_unidades_unidade ON usuario_unidades(unidade_id);
