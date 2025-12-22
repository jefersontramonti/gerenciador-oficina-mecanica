-- V030: Fix existing admin user to have oficinaId
-- This migration ensures that the admin user from the seed has a valid oficina_id

-- First, ensure the oficina exists (from DatabaseSeeder)
INSERT INTO oficinas (
    id, cnpj_cpf, tipo_pessoa, razao_social, nome_fantasia, inscricao_estadual, inscricao_municipal,
    regime_tributario, nome_responsavel, telefone_celular, email,
    endereco_logradouro, endereco_numero, endereco_complemento, endereco_bairro,
    endereco_cidade, endereco_estado, endereco_cep,
    plano, status, valor_mensalidade, data_assinatura, data_vencimento_plano,
    created_at, updated_at, ativo
) VALUES (
      CAST('0f111111-1111-1111-1111-111111111111' AS UUID),
    '12345678000190', 'PESSOA_JURIDICA', 'Auto Center São Paulo Ltda', 'Auto Center SP',
    '123456789', '987654321', 'SIMPLES_NACIONAL',
    'João Silva', '1133334444', 'contato@autocentrosp.com.br',
    'Av. Paulista', '1000', 'Sala 501', 'Bela Vista',
    'São Paulo', 'SP', '01310100',
    'PROFISSIONAL', 'ATIVA', 199.90,
    CURRENT_DATE, CURRENT_DATE + 30,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true
)
ON CONFLICT (cnpj_cpf) DO NOTHING;

-- Update the admin user to reference this oficina
UPDATE usuarios
SET oficina_id = CAST('0f111111-1111-1111-1111-111111111111' AS UUID)
WHERE email = 'admin@pitstop.com' AND oficina_id IS NULL;
