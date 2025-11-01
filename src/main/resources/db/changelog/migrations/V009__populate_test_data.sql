-- ============================================================================
-- Populate test data for clientes and veiculos
-- CPFs must be 11 digits, CNPJs must be 14 digits
-- ============================================================================

-- CLIENTES - PESSOAS FÍSICAS (CPF formatado: 000.000.000-00)
INSERT INTO clientes (id, tipo, nome, cpf_cnpj, email, telefone, celular, logradouro, numero, complemento, bairro, cidade, estado, cep, ativo, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'PESSOA_FISICA', 'José da Silva', '111.222.333-44', 'jose.silva@email.com', '1133334444', '11987654321', 'Rua das Flores', '123', 'Apto 101', 'Centro', 'São Paulo', 'SP', '01310100', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222222', 'PESSOA_FISICA', 'Ana Paula Souza', '222.333.444-55', 'ana.souza@email.com', NULL, '11976543210', 'Av. Paulista', '1500', NULL, 'Bela Vista', 'São Paulo', 'SP', '01310200', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333', 'PESSOA_FISICA', 'Carlos Eduardo Mendes', '333.444.555-66', 'carlos.mendes@email.com', '1144445555', '11965432109', 'Rua Augusta', '789', 'Casa', 'Consolação', 'São Paulo', 'SP', '01305000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (cpf_cnpj) DO NOTHING;

-- VEÍCULOS
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
-- José da Silva
('v1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'ABC1234', 'Volkswagen', 'Gol', 2018, 'Prata', '9BWAA45U0JP123456', 85000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Ana Paula Souza
('v2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'BRA2C34', 'Honda', 'Civic', 2020, 'Preto', '19XFC2F53LE123458', 45000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Carlos Eduardo Mendes
('v3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'XYZ9876', 'Toyota', 'Corolla', 2019, 'Branco', '9BR53ZEC7K0123459', 62000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;
