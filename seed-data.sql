-- ============================================================================
-- Script de População do Banco de Dados - PitStop
-- ============================================================================
-- Este script popula o banco com dados de teste realistas
-- Execute via: psql ou ferramenta SQL de sua preferência
-- ============================================================================

-- Limpar dados existentes (cuidado em produção!)
-- DELETE FROM veiculos;
-- DELETE FROM clientes;
-- DELETE FROM usuarios WHERE email != 'admin@pitstop.com';

-- ============================================================================
-- USUÁRIOS
-- ============================================================================
-- Senha para todos: "senha123" (BCrypt hash com 12 rounds)
-- Você pode gerar novos hashes em: https://bcrypt-generator.com/

INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'Carlos Henrique Silva', 'gerente@pitstop.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lk3v8n7W4/zO', 'GERENTE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),
('e58ed763-928c-4155-bee9-fdbaaadc15f3', 'Maria Santos Oliveira', 'atendente@pitstop.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lk3v8n7W4/zO', 'ATENDENTE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),
('d364f5a2-5c1a-4c3e-8f9b-1e4d2c8a6b3c', 'João Pedro Almeida', 'mecanico@pitstop.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lk3v8n7W4/zO', 'MECANICO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),
('c273e4d1-4b0a-3b2e-7e8a-0d3c1b7a5a2b', 'Roberto Costa Lima', 'mecanico2@pitstop.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lk3v8n7W4/zO', 'MECANICO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
ON CONFLICT (email) DO NOTHING;

-- ============================================================================
-- CLIENTES - PESSOAS FÍSICAS
-- ============================================================================

INSERT INTO clientes (id, tipo, nome, cpf_cnpj, email, telefone, celular, logradouro, numero, complemento, bairro, cidade, estado, cep, ativo, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'PESSOA_FISICA', 'José da Silva', '12345678901', 'jose.silva@email.com', '1133334444', '11987654321', 'Rua das Flores', '123', 'Apto 101', 'Centro', 'São Paulo', 'SP', '01310100', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222222', 'PESSOA_FISICA', 'Ana Paula Souza', '23456789012', 'ana.souza@email.com', NULL, '11976543210', 'Av. Paulista', '1500', NULL, 'Bela Vista', 'São Paulo', 'SP', '01310200', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333', 'PESSOA_FISICA', 'Carlos Eduardo Mendes', '34567890123', 'carlos.mendes@email.com', '1144445555', '11965432109', 'Rua Augusta', '789', 'Casa', 'Consolação', 'São Paulo', 'SP', '01305000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('44444444-4444-4444-4444-444444444444', 'PESSOA_FISICA', 'Mariana Costa Lima', '45678901234', 'mariana.lima@email.com', NULL, '11954321098', 'Rua Oscar Freire', '456', 'Apto 502', 'Jardins', 'São Paulo', 'SP', '01426001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('55555555-5555-5555-5555-555555555555', 'PESSOA_FISICA', 'Roberto Alves Santos', '56789012345', 'roberto.santos@email.com', '1155556666', '11943210987', 'Av. Brigadeiro Faria Lima', '2000', 'Conj 801', 'Pinheiros', 'São Paulo', 'SP', '01452000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('66666666-6666-6666-6666-666666666666', 'PESSOA_FISICA', 'Fernanda Rodrigues', '67890123456', 'fernanda.rodrigues@email.com', NULL, '11932109876', 'Rua Haddock Lobo', '321', NULL, 'Cerqueira César', 'São Paulo', 'SP', '01414001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('77777777-7777-7777-7777-777777777777', 'PESSOA_FISICA', 'Lucas Ferreira', '78901234567', 'lucas.ferreira@email.com', '1166667777', '11921098765', 'Rua da Consolação', '567', 'Apto 302', 'Consolação', 'São Paulo', 'SP', '01301000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('88888888-8888-8888-8888-888888888888', 'PESSOA_FISICA', 'Juliana Martins', '89012345678', 'juliana.martins@email.com', NULL, '11910987654', 'Av. Rebouças', '1234', 'Bloco B', 'Pinheiros', 'São Paulo', 'SP', '05402000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (cpf_cnpj) DO NOTHING;

-- ============================================================================
-- CLIENTES - PESSOAS JURÍDICAS
-- ============================================================================

INSERT INTO clientes (id, tipo, nome, cpf_cnpj, email, telefone, celular, logradouro, numero, complemento, bairro, cidade, estado, cep, ativo, created_at, updated_at) VALUES
('99999999-9999-9999-9999-999999999999', 'PESSOA_JURIDICA', 'Transportadora RápidaLog Ltda', '12345678000190', 'contato@rapidalog.com.br', '1130001000', '11999887766', 'Av. dos Bandeirantes', '500', 'Galpão 3', 'Vila Mariana', 'São Paulo', 'SP', '04071000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'PESSOA_JURIDICA', 'Distribuidora Alimentos ABC S.A.', '23456789000191', 'financeiro@alimentosabc.com.br', '1131002000', '11988776655', 'Rua Vergueiro', '2500', NULL, 'Vila Mariana', 'São Paulo', 'SP', '04101000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'PESSOA_JURIDICA', 'Tech Solutions Informática ME', '34567890000192', 'ti@techsolutions.com.br', '1132003000', NULL, 'Av. Faria Lima', '3500', 'Sala 1205', 'Itaim Bibi', 'São Paulo', 'SP', '04538133', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'PESSOA_JURIDICA', 'Construtora Horizonte Ltda', '45678901000193', 'obras@horizonteconstrutora.com', '1133004000', '11977665544', 'Rua Funchal', '418', '10º andar', 'Vila Olímpia', 'São Paulo', 'SP', '04551060', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (cpf_cnpj) DO NOTHING;

-- ============================================================================
-- VEÍCULOS - Associados aos clientes cadastrados
-- ============================================================================

-- Veículos do José da Silva (11111111...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('v1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'ABC1234', 'Volkswagen', 'Gol', 2018, 'Prata', '9BWAA45U0JP123456', 85000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('v1111111-1111-1111-1111-111111111112', '11111111-1111-1111-1111-111111111111', 'DEF5678', 'Fiat', 'Uno', 2015, 'Branco', '9BD15842J28123457', 120000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Veículos da Ana Paula Souza (22222222...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('v2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'BRA2C34', 'Honda', 'Civic', 2020, 'Preto', '19XFC2F53LE123458', 45000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Veículos do Carlos Eduardo Mendes (33333333...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('v3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'XYZ9876', 'Toyota', 'Corolla', 2019, 'Branco', '9BR53ZEC7K0123459', 62000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('v3333333-3333-3333-3333-333333333334', '33333333-3333-3333-3333-333333333333', 'GHI3456', 'Chevrolet', 'Onix', 2021, 'Vermelho', '9BGKS69J0MG123460', 35000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Veículos da Mariana Costa Lima (44444444...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('v4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'JKL7890', 'Hyundai', 'HB20', 2022, 'Azul', '9BHBD41JCPK123461', 18000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Veículos do Roberto Alves Santos (55555555...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('v5555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'MNO1234', 'Jeep', 'Compass', 2021, 'Cinza', '3C4NJDCB4LT123462', 52000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('v5555555-5555-5555-5555-555555555556', '55555555-5555-5555-5555-555555555555', 'PQR5678', 'Ford', 'Ranger', 2017, 'Preto', '8AFJZZZ12AJ123463', 95000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Veículos da Fernanda Rodrigues (66666666...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('v6666666-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', 'STU9012', 'Renault', 'Sandero', 2020, 'Branco', '93Y5SRD63LJ123464', 48000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Veículos do Lucas Ferreira (77777777...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('v7777777-7777-7777-7777-777777777777', '77777777-7777-7777-7777-777777777777', 'BRA3E45', 'Nissan', 'Kicks', 2023, 'Vermelho', '3N1CN7AP3PL123465', 12000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Veículos da Juliana Martins (88888888...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('v8888888-8888-8888-8888-888888888888', '88888888-8888-8888-8888-888888888888', 'VWX3456', 'Peugeot', '208', 2019, 'Cinza', '9362PKF5WKP123466', 71000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- ============================================================================
-- VEÍCULOS - Empresas (frota)
-- ============================================================================

-- Frota da Transportadora RápidaLog (99999999...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('v9999999-9999-9999-9999-999999999991', '99999999-9999-9999-9999-999999999999', 'YZA7890', 'Mercedes-Benz', 'Sprinter', 2020, 'Branco', 'WDB9066331N123467', 125000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('v9999999-9999-9999-9999-999999999992', '99999999-9999-9999-9999-999999999999', 'BCD1234', 'Iveco', 'Daily', 2019, 'Branco', 'ZCFC35A0705123468', 145000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('v9999999-9999-9999-9999-999999999993', '99999999-9999-9999-9999-999999999999', 'EFG5678', 'Ford', 'Cargo', 2018, 'Branco', '9BFW4RHA5JB123469', 180000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Frota da Distribuidora Alimentos ABC (aaaaaaaa...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('vaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'HIJ9012', 'Volkswagen', 'Delivery', 2021, 'Branco', '9532832V0MR123470', 98000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'BRA4F56', 'Mercedes-Benz', 'Accelo', 2020, 'Branco', 'WDB9603381K123471', 110000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Frota da Tech Solutions (bbbbbbbb...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('vbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'KLM3456', 'Chevrolet', 'Spin', 2022, 'Prata', '9BGRB48A0N0123472', 32000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'NOP7890', 'Fiat', 'Fiorino', 2019, 'Branco', '9BD25544KF0123473', 88000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- Frota da Construtora Horizonte (cccccccc...)
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
('vcccccccc-cccc-cccc-cccc-cccccccccc01', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'QRS1234', 'Toyota', 'Hilux', 2021, 'Branco', '8AJFH22G8L4123474', 67000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vcccccccc-cccc-cccc-cccc-cccccccccc02', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'TUV5678', 'Mitsubishi', 'L200', 2020, 'Prata', '93XBN1CJXLE123475', 79000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vcccccccc-cccc-cccc-cccc-cccccccccc03', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'BRA5G67', 'Ford', 'F-250', 2019, 'Preto', '1FT7W2B68KEE123476', 102000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (placa) DO NOTHING;

-- ============================================================================
-- RESUMO DOS DADOS INSERIDOS
-- ============================================================================
-- Usuários: 4 (+ admin existente = 5 total)
--   - 1 Gerente
--   - 1 Atendente
--   - 2 Mecânicos
--
-- Clientes: 12 total
--   - 8 Pessoas Físicas
--   - 4 Pessoas Jurídicas
--
-- Veículos: 25 total
--   - 10 veículos de pessoas físicas
--   - 15 veículos de frotas empresariais
-- ============================================================================

SELECT 'População do banco concluída com sucesso!' AS status;
SELECT COUNT(*) AS total_usuarios FROM usuarios;
SELECT COUNT(*) AS total_clientes FROM clientes;
SELECT COUNT(*) AS total_veiculos FROM veiculos;
