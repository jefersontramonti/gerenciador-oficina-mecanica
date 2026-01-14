-- =====================================================
-- V061: Criação da tabela de anexos (imagens/documentos)
-- =====================================================
-- Suporta anexos para: OrdemServico, Cliente, Peca
-- Multi-tenant: cada anexo pertence a uma oficina
-- Soft delete: ativo=false ao invés de deletar
-- =====================================================

-- Tabela de anexos
CREATE TABLE anexos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Multi-tenant
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Referência à entidade pai (polimórfico)
    entidade_tipo VARCHAR(50) NOT NULL,
    entidade_id UUID NOT NULL,

    -- Categoria do anexo
    categoria VARCHAR(50) DEFAULT 'OUTROS',

    -- Metadados do arquivo
    nome_original VARCHAR(255) NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    caminho_arquivo VARCHAR(500) NOT NULL,

    -- Descrição opcional
    descricao TEXT,

    -- Auditoria
    uploaded_by UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Soft delete
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    -- Constraints
    CONSTRAINT chk_entidade_tipo CHECK (entidade_tipo IN ('ORDEM_SERVICO', 'CLIENTE', 'PECA')),
    CONSTRAINT chk_categoria CHECK (categoria IN (
        'FOTO_VEICULO', 'DIAGNOSTICO', 'AUTORIZACAO', 'LAUDO_TECNICO',
        'DOCUMENTO_PESSOAL', 'DOCUMENTO_EMPRESA', 'CONTRATO', 'DOCUMENTO_VEICULO',
        'FOTO_PECA', 'NOTA_FISCAL', 'CERTIFICADO', 'OUTROS'
    )),
    CONSTRAINT chk_tamanho_positivo CHECK (tamanho_bytes > 0),
    CONSTRAINT chk_mime_type CHECK (mime_type IN (
        'image/jpeg', 'image/png', 'image/webp', 'application/pdf'
    ))
);

-- Índices para performance
-- Índice principal: busca por oficina + entidade (multi-tenant + polimórfico)
CREATE INDEX idx_anexos_oficina_entidade
    ON anexos(oficina_id, entidade_tipo, entidade_id, ativo);

-- Índice para busca por entidade sem filtro de oficina (admin/suporte)
CREATE INDEX idx_anexos_entidade
    ON anexos(entidade_tipo, entidade_id);

-- Índice para ordenação por data de upload
CREATE INDEX idx_anexos_uploaded_at
    ON anexos(uploaded_at DESC);

-- Índice para busca por caminho (verificar duplicatas)
CREATE UNIQUE INDEX idx_anexos_caminho_unico
    ON anexos(caminho_arquivo);

-- Índice para limpeza de anexos inativos (job de limpeza)
CREATE INDEX idx_anexos_inativos
    ON anexos(ativo, uploaded_at)
    WHERE ativo = FALSE;

-- Comentários
COMMENT ON TABLE anexos IS 'Armazena metadados de anexos (imagens e documentos) associados a entidades do sistema';
COMMENT ON COLUMN anexos.entidade_tipo IS 'Tipo da entidade: ORDEM_SERVICO, CLIENTE, PECA';
COMMENT ON COLUMN anexos.entidade_id IS 'ID da entidade pai (ordem_servico.id, cliente.id ou peca.id)';
COMMENT ON COLUMN anexos.categoria IS 'Categoria do anexo para organização';
COMMENT ON COLUMN anexos.nome_original IS 'Nome original do arquivo enviado pelo usuário';
COMMENT ON COLUMN anexos.nome_arquivo IS 'Nome do arquivo no storage (UUID gerado)';
COMMENT ON COLUMN anexos.caminho_arquivo IS 'Caminho relativo no storage (oficina-X/tipo/ano/mes/arquivo)';
COMMENT ON COLUMN anexos.ativo IS 'FALSE para soft delete';
