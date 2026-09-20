COMMENT ON COLUMN bolsas.sequencia_entrada IS
  'Ordem de chegada no estoque. Base da Lista (AED-U1) e do desempate por chegada. Nao confundir com FEFO (U2).';
COMMENT ON COLUMN bolsas.versao IS
  'Optimistic locking (JPA @Version). Duas alocacoes simultaneas da mesma bolsa: uma vence, a outra recebe 409.';
COMMENT ON TABLE estoque_itens IS
  'Projecao de contagem por unidade+tipo+componente. Reconstruivel a partir de bolsas.';
