-- V062: Adiciona campo visivel_cliente na tabela anexos
-- Permite que a oficina marque quais anexos serao visiveis
-- para o cliente na pagina publica de aprovacao de orcamento

ALTER TABLE anexos
ADD COLUMN visivel_cliente BOOLEAN NOT NULL DEFAULT FALSE;

-- Indice para busca eficiente de anexos visiveis por entidade
CREATE INDEX idx_anexos_visivel_cliente
    ON anexos(oficina_id, entidade_tipo, entidade_id)
    WHERE visivel_cliente = TRUE AND ativo = TRUE;

COMMENT ON COLUMN anexos.visivel_cliente IS
    'Indica se o anexo sera visivel para o cliente na pagina publica de aprovacao de orcamento';
