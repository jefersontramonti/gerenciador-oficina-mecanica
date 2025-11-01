-- Migration: Create veiculos table
-- Description: Vehicle management with relationship to clientes
-- Author: PitStop Team
-- Date: 2025-10-31

CREATE TABLE veiculos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL,
    placa VARCHAR(7) NOT NULL UNIQUE,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    ano INTEGER NOT NULL,
    cor VARCHAR(30),
    chassi VARCHAR(17),
    quilometragem INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_veiculos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT chk_veiculos_ano CHECK (ano >= 1900 AND ano <= EXTRACT(YEAR FROM CURRENT_DATE) + 1),
    CONSTRAINT chk_veiculos_quilometragem CHECK (quilometragem >= 0),
    CONSTRAINT chk_veiculos_chassi CHECK (chassi IS NULL OR LENGTH(chassi) = 17),
    CONSTRAINT chk_veiculos_placa CHECK (LENGTH(placa) = 7)
);

-- Indexes for performance
CREATE INDEX idx_veiculos_cliente_id ON veiculos(cliente_id);
CREATE INDEX idx_veiculos_placa ON veiculos(placa);
CREATE INDEX idx_veiculos_marca_modelo ON veiculos(marca, modelo);
CREATE INDEX idx_veiculos_created_at ON veiculos(created_at DESC);

-- Comments
COMMENT ON TABLE veiculos IS 'Cadastro de veículos vinculados a clientes';
COMMENT ON COLUMN veiculos.cliente_id IS 'FK para clientes - relacionamento Many-to-One obrigatório';
COMMENT ON COLUMN veiculos.placa IS 'Placa sem hífen (7 caracteres) - formatos BR antigo (ABC1234) ou Mercosul (ABC1D23)';
COMMENT ON COLUMN veiculos.chassi IS 'Número do chassi (17 caracteres) - opcional';
COMMENT ON COLUMN veiculos.quilometragem IS 'Quilometragem atual do veículo (atualizada a cada OS)';
COMMENT ON CONSTRAINT fk_veiculos_cliente ON veiculos IS 'FK com ON DELETE RESTRICT - impede deleção de cliente com veículos cadastrados';
COMMENT ON CONSTRAINT chk_veiculos_ano ON veiculos IS 'Ano entre 1900 e ano atual + 1 (permite cadastrar veículos 0km do próximo ano)';
COMMENT ON CONSTRAINT chk_veiculos_placa ON veiculos IS 'Placa deve ter exatamente 7 caracteres (sem hífen)';
