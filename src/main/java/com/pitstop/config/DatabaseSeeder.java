package com.pitstop.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint temporário para popular o banco de dados com dados de teste.
 * ⚠️ REMOVER EM PRODUÇÃO!
 */
@RestController
@RequestMapping("/api/admin/seed")
public class DatabaseSeeder {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/admin-only")
    public ResponseEntity<Map<String, Object>> createAdminOnly() {
        try {
            String senhaHashAdmin = passwordEncoder.encode("admin123");

            // First, create a default oficina if it doesn't exist
            jdbcTemplate.update("""
                INSERT INTO oficinas (
                    id, cnpj_cpf, tipo_pessoa, razao_social, nome_fantasia, inscricao_estadual, inscricao_municipal,
                    regime_tributario, contato_nome, contato_telefone, contato_email, contato_cargo, logradouro,
                    numero, complemento, bairro, cidade, estado, cep, plano, status, valor_mensalidade,
                    data_assinatura, data_vencimento_plano, created_at, updated_at
                ) VALUES (
                    '0f111111-1111-1111-1111-111111111111',
                    '12345678000190', 'JURIDICA', 'Auto Center São Paulo Ltda', 'Auto Center SP',
                    '123456789', '987654321', 'SIMPLES_NACIONAL',
                    'João Silva', '1133334444', 'contato@autocentrosp.com.br', 'Proprietário',
                    'Av. Paulista', '1000', 'Sala 501', 'Bela Vista', 'São Paulo', 'SP', '01310100',
                    'PROFISSIONAL', 'ATIVA', 199.90,
                    CURRENT_DATE, CURRENT_DATE + 30,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                ON CONFLICT (cnpj_cpf) DO NOTHING
                """);

            // Delete any existing admin user to ensure clean state
            jdbcTemplate.update("DELETE FROM usuarios WHERE email = 'admin@pitstop.com'");

            // Then insert the new admin user with oficina_id
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('a0000000-0000-0000-0000-000000000001', '0f111111-1111-1111-1111-111111111111', 'Administrador do Sistema', 'admin@pitstop.com', ?, 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                """, senhaHashAdmin);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "success");
            resultado.put("mensagem", "Oficina e usuário ADMIN criados com sucesso!");
            resultado.put("credenciais", "admin@pitstop.com / admin123");
            resultado.put("oficinaId", "0f111111-1111-1111-1111-111111111111");

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("status", "error");
            erro.put("mensagem", e.getMessage());
            return ResponseEntity.internalServerError().body(erro);
        }
    }

    @PostMapping("/super-admin-only")
    public ResponseEntity<Map<String, Object>> createSuperAdminOnly() {
        try {
            String senhaHashSuperAdmin = passwordEncoder.encode("SuperSecure2025!");

            // Delete any existing SUPER_ADMIN user to ensure clean state
            jdbcTemplate.update("DELETE FROM usuarios WHERE email = 'superadmin@pitstop.com'");

            // Insert the SUPER_ADMIN user WITHOUT oficina_id (SaaS admin)
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('00000000-0000-0000-0000-000000000000', NULL, 'Super Administrador PitStop', 'superadmin@pitstop.com', ?, 'SUPER_ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                """, senhaHashSuperAdmin);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "success");
            resultado.put("mensagem", "Usuário SUPER_ADMIN criado com sucesso!");
            resultado.put("credenciais", "superadmin@pitstop.com / SuperSecure2025!");
            resultado.put("perfil", "SUPER_ADMIN (gerencia todas as oficinas SaaS)");

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("status", "error");
            erro.put("mensagem", e.getMessage());
            return ResponseEntity.internalServerError().body(erro);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> seedDatabase() {
        try {
            // Hash da senha "senha123" para os usuários
            String senhaHash = passwordEncoder.encode("senha123");

            // Hash da senha "admin123" para o admin
            String senhaHashAdmin = passwordEncoder.encode("admin123");

            // ====== OFICINAS ======
            // Oficina 1: Auto Center São Paulo
            jdbcTemplate.update("""
                INSERT INTO oficinas (
                    id, cnpj_cpf, tipo_pessoa, razao_social, nome_fantasia, inscricao_estadual, inscricao_municipal,
                    regime_tributario, contato_nome, contato_telefone, contato_email, contato_cargo, logradouro,
                    numero, complemento, bairro, cidade, estado, cep, plano, status, valor_mensalidade,
                    data_assinatura, data_vencimento_plano, created_at, updated_at
                ) VALUES (
                    '0f111111-1111-1111-1111-111111111111',
                    '12345678000190', 'JURIDICA', 'Auto Center São Paulo Ltda', 'Auto Center SP',
                    '123456789', '987654321', 'SIMPLES_NACIONAL',
                    'João Silva', '1133334444', 'contato@autocentrosp.com.br', 'Proprietário',
                    'Av. Paulista', '1000', 'Sala 501', 'Bela Vista', 'São Paulo', 'SP', '01310100',
                    'PROFISSIONAL', 'ATIVA', 199.90,
                    CURRENT_DATE, CURRENT_DATE + 30,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                ON CONFLICT (cnpj_cpf) DO NOTHING
                """);

            // Oficina 2: Mecânica Premium
            jdbcTemplate.update("""
                INSERT INTO oficinas (
                    id, cnpj_cpf, tipo_pessoa, razao_social, nome_fantasia, inscricao_estadual, inscricao_municipal,
                    regime_tributario, contato_nome, contato_telefone, contato_email, contato_cargo, logradouro,
                    numero, complemento, bairro, cidade, estado, cep, plano, status, valor_mensalidade,
                    data_assinatura, data_vencimento_plano, created_at, updated_at
                ) VALUES (
                    '0f222222-2222-2222-2222-222222222222',
                    '98765432000180', 'JURIDICA', 'Mecânica Premium Serviços Automotivos Ltda', 'Mecânica Premium',
                    '987654321', '123456789', 'SIMPLES_NACIONAL',
                    'Maria Santos', '1144445555', 'contato@mecanicapremium.com.br', 'Gerente',
                    'Av. Faria Lima', '2500', 'Loja 10', 'Pinheiros', 'São Paulo', 'SP', '05426000',
                    'TURBINADO', 'ATIVA', 399.90,
                    CURRENT_DATE, CURRENT_DATE + 30,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                ON CONFLICT (cnpj_cpf) DO NOTHING
                """);

            // ====== USUÁRIOS ======
            // Hash da senha "SuperSecure2025!" para o SUPER_ADMIN
            String senhaHashSuperAdmin = passwordEncoder.encode("SuperSecure2025!");

            // SUPER_ADMIN (senha: SuperSecure2025!) - SEM OFICINA (gerencia o SaaS)
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('00000000-0000-0000-0000-000000000000', NULL, 'Super Administrador PitStop', 'superadmin@pitstop.com', ?, 'SUPER_ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHashSuperAdmin);

            // ADMIN (senha: admin123) - Oficina 1
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('a0000000-0000-0000-0000-000000000001', '0f111111-1111-1111-1111-111111111111', 'Administrador do Sistema', 'admin@pitstop.com', ?, 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHashAdmin);

            // GERENTE (senha: senha123) - Oficina 1
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('f47ac10b-58cc-4372-a567-0e02b2c3d479', '0f111111-1111-1111-1111-111111111111', 'Carlos Henrique Silva', 'gerente@pitstop.com', ?, 'GERENTE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            // ATENDENTE (senha: senha123) - Oficina 1
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('e58ed763-928c-4155-bee9-fdbaaadc15f3', '0f111111-1111-1111-1111-111111111111', 'Maria Santos Oliveira', 'atendente@pitstop.com', ?, 'ATENDENTE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            // MECANICO 1 (senha: senha123) - Oficina 1
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('d364f5a2-5c1a-4c3e-8f9b-1e4d2c8a6b3c', '0f111111-1111-1111-1111-111111111111', 'João Pedro Almeida', 'mecanico@pitstop.com', ?, 'MECANICO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            // MECANICO 2 (senha: senha123) - Oficina 2
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('c273e4d1-4b0a-3b2e-7e8a-0d3c1b7a5a2b', '0f222222-2222-2222-2222-222222222222', 'Roberto Costa Lima', 'mecanico2@pitstop.com', ?, 'MECANICO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            // ====== CLIENTES - PESSOAS FÍSICAS ======
            // Oficina 1 - Auto Center SP
            String[] clientesPF_Of1 = {
                "('11111111-1111-1111-1111-111111111111', '0f111111-1111-1111-1111-111111111111', 'PESSOA_FISICA', 'José da Silva', '12345678901', 'jose.silva@email.com', '1133334444', '11987654321', 'Rua das Flores', '123', 'Apto 101', 'Centro', 'São Paulo', 'SP', '01310100', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('22222222-2222-2222-2222-222222222222', '0f111111-1111-1111-1111-111111111111', 'PESSOA_FISICA', 'Ana Paula Souza', '23456789012', 'ana.souza@email.com', NULL, '11976543210', 'Av. Paulista', '1500', NULL, 'Bela Vista', 'São Paulo', 'SP', '01310200', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('33333333-3333-3333-3333-333333333333', '0f111111-1111-1111-1111-111111111111', 'PESSOA_FISICA', 'Carlos Eduardo Mendes', '34567890123', 'carlos.mendes@email.com', '1144445555', '11965432109', 'Rua Augusta', '789', 'Casa', 'Consolação', 'São Paulo', 'SP', '01305000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('44444444-4444-4444-4444-444444444444', '0f111111-1111-1111-1111-111111111111', 'PESSOA_FISICA', 'Mariana Costa Lima', '45678901234', 'mariana.lima@email.com', NULL, '11954321098', 'Rua Oscar Freire', '456', 'Apto 502', 'Jardins', 'São Paulo', 'SP', '01426001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            };

            // Oficina 2 - Mecânica Premium
            String[] clientesPF_Of2 = {
                "('55555555-5555-5555-5555-555555555555', '0f222222-2222-2222-2222-222222222222', 'PESSOA_FISICA', 'Roberto Alves Santos', '56789012345', 'roberto.santos@email.com', '1155556666', '11943210987', 'Av. Brigadeiro Faria Lima', '2000', 'Conj 801', 'Pinheiros', 'São Paulo', 'SP', '01452000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('66666666-6666-6666-6666-666666666666', '0f222222-2222-2222-2222-222222222222', 'PESSOA_FISICA', 'Fernanda Rodrigues', '67890123456', 'fernanda.rodrigues@email.com', NULL, '11932109876', 'Rua Haddock Lobo', '321', NULL, 'Cerqueira César', 'São Paulo', 'SP', '01414001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('77777777-7777-7777-7777-777777777777', '0f222222-2222-2222-2222-222222222222', 'PESSOA_FISICA', 'Lucas Ferreira', '78901234567', 'lucas.ferreira@email.com', '1166667777', '11921098765', 'Rua da Consolação', '567', 'Apto 302', 'Consolação', 'São Paulo', 'SP', '01301000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('88888888-8888-8888-8888-888888888888', '0f222222-2222-2222-2222-222222222222', 'PESSOA_FISICA', 'Juliana Martins', '89012345678', 'juliana.martins@email.com', NULL, '11910987654', 'Av. Rebouças', '1234', 'Bloco B', 'Pinheiros', 'São Paulo', 'SP', '05402000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            };

            for (String cliente : clientesPF_Of1) {
                jdbcTemplate.update("""
                    INSERT INTO clientes (id, oficina_id, tipo, nome, cpf_cnpj, email, telefone, celular, logradouro, numero, complemento, bairro, cidade, estado, cep, ativo, created_at, updated_at) VALUES
                    """ + cliente + " ON CONFLICT (cpf_cnpj) DO NOTHING");
            }

            for (String cliente : clientesPF_Of2) {
                jdbcTemplate.update("""
                    INSERT INTO clientes (id, oficina_id, tipo, nome, cpf_cnpj, email, telefone, celular, logradouro, numero, complemento, bairro, cidade, estado, cep, ativo, created_at, updated_at) VALUES
                    """ + cliente + " ON CONFLICT (cpf_cnpj) DO NOTHING");
            }

            // ====== CLIENTES - PESSOAS JURÍDICAS ======
            // Oficina 1 - Auto Center SP
            String[] clientesPJ_Of1 = {
                "('99999999-9999-9999-9999-999999999999', '0f111111-1111-1111-1111-111111111111', 'PESSOA_JURIDICA', 'Transportadora RápidaLog Ltda', '12345678000190', 'contato@rapidalog.com.br', '1130001000', '11999887766', 'Av. dos Bandeirantes', '500', 'Galpão 3', 'Vila Mariana', 'São Paulo', 'SP', '04071000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '0f111111-1111-1111-1111-111111111111', 'PESSOA_JURIDICA', 'Distribuidora Alimentos ABC S.A.', '23456789000191', 'financeiro@alimentosabc.com.br', '1131002000', '11988776655', 'Rua Vergueiro', '2500', NULL, 'Vila Mariana', 'São Paulo', 'SP', '04101000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            };

            // Oficina 2 - Mecânica Premium
            String[] clientesPJ_Of2 = {
                "('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '0f222222-2222-2222-2222-222222222222', 'PESSOA_JURIDICA', 'Tech Solutions Informática ME', '34567890000192', 'ti@techsolutions.com.br', '1132003000', NULL, 'Av. Faria Lima', '3500', 'Sala 1205', 'Itaim Bibi', 'São Paulo', 'SP', '04538133', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('cccccccc-cccc-cccc-cccc-cccccccccccc', '0f222222-2222-2222-2222-222222222222', 'PESSOA_JURIDICA', 'Construtora Horizonte Ltda', '45678901000193', 'obras@horizonteconstrutora.com', '1133004000', '11977665544', 'Rua Funchal', '418', '10º andar', 'Vila Olímpia', 'São Paulo', 'SP', '04551060', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            };

            for (String cliente : clientesPJ_Of1) {
                jdbcTemplate.update("""
                    INSERT INTO clientes (id, oficina_id, tipo, nome, cpf_cnpj, email, telefone, celular, logradouro, numero, complemento, bairro, cidade, estado, cep, ativo, created_at, updated_at) VALUES
                    """ + cliente + " ON CONFLICT (cpf_cnpj) DO NOTHING");
            }

            for (String cliente : clientesPJ_Of2) {
                jdbcTemplate.update("""
                    INSERT INTO clientes (id, oficina_id, tipo, nome, cpf_cnpj, email, telefone, celular, logradouro, numero, complemento, bairro, cidade, estado, cep, ativo, created_at, updated_at) VALUES
                    """ + cliente + " ON CONFLICT (cpf_cnpj) DO NOTHING");
            }

            // ====== VEÍCULOS ======
            // Oficina 1 - Auto Center SP
            String[] veiculos_Of1 = {
                // José da Silva
                "('v1111111-1111-1111-1111-111111111111', '0f111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'ABC1234', 'Volkswagen', 'Gol', 2018, 'Prata', '9BWAA45U0JP123456', 85000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v1111111-1111-1111-1111-111111111112', '0f111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'DEF5678', 'Fiat', 'Uno', 2015, 'Branco', '9BD15842J28123457', 120000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Ana Paula Souza
                "('v2222222-2222-2222-2222-222222222222', '0f111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'BRA2C34', 'Honda', 'Civic', 2020, 'Preto', '19XFC2F53LE123458', 45000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Carlos Eduardo Mendes
                "('v3333333-3333-3333-3333-333333333333', '0f111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'XYZ9876', 'Toyota', 'Corolla', 2019, 'Branco', '9BR53ZEC7K0123459', 62000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v3333333-3333-3333-3333-333333333334', '0f111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'GHI3456', 'Chevrolet', 'Onix', 2021, 'Vermelho', '9BGKS69J0MG123460', 35000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Mariana Costa Lima
                "('v4444444-4444-4444-4444-444444444444', '0f111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', 'JKL7890', 'Hyundai', 'HB20', 2022, 'Azul', '9BHBD41JCPK123461', 18000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Frota Transportadora (PJ Of1)
                "('v9999999-9999-9999-9999-999999999991', '0f111111-1111-1111-1111-111111111111', '99999999-9999-9999-9999-999999999999', 'YZA7890', 'Mercedes-Benz', 'Sprinter', 2020, 'Branco', 'WDB9066331N123467', 125000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v9999999-9999-9999-9999-999999999992', '0f111111-1111-1111-1111-111111111111', '99999999-9999-9999-9999-999999999999', 'BCD1234', 'Iveco', 'Daily', 2019, 'Branco', 'ZCFC35A0705123468', 145000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v9999999-9999-9999-9999-999999999993', '0f111111-1111-1111-1111-111111111111', '99999999-9999-9999-9999-999999999999', 'EFG5678', 'Ford', 'Cargo', 2018, 'Branco', '9BFW4RHA5JB123469', 180000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Frota Distribuidora (PJ Of1)
                "('vaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', '0f111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'HIJ9012', 'Volkswagen', 'Delivery', 2021, 'Branco', '9532832V0MR123470', 98000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('vaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02', '0f111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'BRA4F56', 'Mercedes-Benz', 'Accelo', 2020, 'Branco', 'WDB9603381K123471', 110000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            };

            // Oficina 2 - Mecânica Premium
            String[] veiculos_Of2 = {
                // Roberto Alves Santos
                "('v5555555-5555-5555-5555-555555555555', '0f222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', 'MNO1234', 'Jeep', 'Compass', 2021, 'Cinza', '3C4NJDCB4LT123462', 52000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v5555555-5555-5555-5555-555555555556', '0f222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', 'PQR5678', 'Ford', 'Ranger', 2017, 'Preto', '8AFJZZZ12AJ123463', 95000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Fernanda Rodrigues
                "('v6666666-6666-6666-6666-666666666666', '0f222222-2222-2222-2222-222222222222', '66666666-6666-6666-6666-666666666666', 'STU9012', 'Renault', 'Sandero', 2020, 'Branco', '93Y5SRD63LJ123464', 48000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Lucas Ferreira
                "('v7777777-7777-7777-7777-777777777777', '0f222222-2222-2222-2222-222222222222', '77777777-7777-7777-7777-777777777777', 'BRA3E45', 'Nissan', 'Kicks', 2023, 'Vermelho', '3N1CN7AP3PL123465', 12000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Juliana Martins
                "('v8888888-8888-8888-8888-888888888888', '0f222222-2222-2222-2222-222222222222', '88888888-8888-8888-8888-888888888888', 'VWX3456', 'Peugeot', '208', 2019, 'Cinza', '9362PKF5WKP123466', 71000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Frota Tech Solutions (PJ Of2)
                "('vbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', '0f222222-2222-2222-2222-222222222222', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'KLM3456', 'Chevrolet', 'Spin', 2022, 'Prata', '9BGRB48A0N0123472', 32000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('vbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', '0f222222-2222-2222-2222-222222222222', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'NOP7890', 'Fiat', 'Fiorino', 2019, 'Branco', '9BD25544KF0123473', 88000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Frota Construtora (PJ Of2)
                "('vcccccccc-cccc-cccc-cccc-cccccccccc01', '0f222222-2222-2222-2222-222222222222', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'QRS1234', 'Toyota', 'Hilux', 2021, 'Branco', '8AJFH22G8L4123474', 67000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('vcccccccc-cccc-cccc-cccc-cccccccccc02', '0f222222-2222-2222-2222-222222222222', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'TUV5678', 'Mitsubishi', 'L200', 2020, 'Prata', '93XBN1CJXLE123475', 79000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('vcccccccc-cccc-cccc-cccc-cccccccccc03', '0f222222-2222-2222-2222-222222222222', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'BRA5G67', 'Ford', 'F-250', 2019, 'Preto', '1FT7W2B68KEE123476', 102000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            };

            for (String veiculo : veiculos_Of1) {
                jdbcTemplate.update("""
                    INSERT INTO veiculos (id, oficina_id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
                    """ + veiculo + " ON CONFLICT (placa) DO NOTHING");
            }

            for (String veiculo : veiculos_Of2) {
                jdbcTemplate.update("""
                    INSERT INTO veiculos (id, oficina_id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
                    """ + veiculo + " ON CONFLICT (placa) DO NOTHING");
            }

            // Contar registros inseridos
            Long totalUsuarios = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
            Long totalClientes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clientes", Long.class);
            Long totalVeiculos = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM veiculos", Long.class);
            Long totalOficinas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oficinas", Long.class);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "success");
            resultado.put("mensagem", "Banco de dados populado com sucesso!");
            resultado.put("totalUsuarios", totalUsuarios);
            resultado.put("totalClientes", totalClientes);
            resultado.put("totalVeiculos", totalVeiculos);
            resultado.put("totalOficinas", totalOficinas);
            resultado.put("credenciais", Map.of(
                "admin", "admin@pitstop.com / admin123",
                "gerente", "gerente@pitstop.com / senha123",
                "atendente", "atendente@pitstop.com / senha123",
                "mecanico", "mecanico@pitstop.com / senha123",
                "mecanico2", "mecanico2@pitstop.com / senha123"
            ));

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("status", "error");
            erro.put("mensagem", e.getMessage());
            return ResponseEntity.internalServerError().body(erro);
        }
    }

    @PostMapping("/fix-inconsistent-data")
    public ResponseEntity<Map<String, Object>> fixInconsistentData() {
        try {
            // Update vehicles pointing to non-existent client 33333333-3333-3333-3333-333333333333
            // to point to any existing client
            int updatedVeiculos = jdbcTemplate.update("""
                UPDATE veiculos
                SET cliente_id = (SELECT id FROM clientes WHERE cpf_cnpj = '345.678.901-23' LIMIT 1)
                WHERE cliente_id = '33333333-3333-3333-3333-333333333333'
                """);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "success");
            resultado.put("mensagem", "Dados inconsistentes corrigidos!");
            resultado.put("veiculosAtualizados", updatedVeiculos);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("status", "error");
            erro.put("mensagem", e.getMessage());
            return ResponseEntity.internalServerError().body(erro);
        }
    }

    /**
     * Seeds SaaS-specific data for demonstration and testing.
     *
     * Creates 3 example workshops (TRIAL, ATIVA, SUSPENSA) with payment history
     * and audit logs to populate the SaaS dashboard.
     *
     * ⚠️ REMOVE IN PRODUCTION!
     */
    @PostMapping("/saas-data")
    public ResponseEntity<Map<String, Object>> seedSaasData() {
        try {
            // ====== OFICINA 3: TRIAL (Nova, sem pagamentos) ======
            jdbcTemplate.update("""
                INSERT INTO oficinas (
                    id, cnpj_cpf, tipo_pessoa, razao_social, nome_fantasia, inscricao_estadual, inscricao_municipal,
                    regime_tributario, contato_nome, contato_telefone, contato_email, contato_cargo, logradouro,
                    numero, complemento, bairro, cidade, estado, cep, plano, status, valor_mensalidade,
                    data_assinatura, data_inicio_trial, data_fim_trial, data_vencimento_plano,
                    created_at, updated_at
                ) VALUES (
                    '0f333333-3333-3333-3333-333333333333',
                    '11122233000155', 'JURIDICA', 'AutoPeças Veloz Comércio Ltda', 'AutoPeças Veloz',
                    '112233445', '556677889', 'SIMPLES_NACIONAL',
                    'Pedro Henrique', '1155557777', 'contato@autopecasveloz.com.br', 'Sócio',
                    'Rua dos Automóveis', '789', NULL, 'Industrial', 'Guarulhos', 'SP', '07111000',
                    'BASICO', 'TRIAL', 99.90,
                    CURRENT_DATE - 10, CURRENT_DATE - 10, CURRENT_DATE + 20, CURRENT_DATE + 20,
                    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP
                )
                ON CONFLICT (cnpj_cpf) DO NOTHING
                """);

            // Admin for Oficina 3
            String senhaHash = passwordEncoder.encode("senha123");
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at) VALUES
                ('a3333333-3333-3333-3333-333333333333', '0f333333-3333-3333-3333-333333333333',
                 'Pedro Henrique Silva', 'admin.veloz@pitstop.com', ?, 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            // ====== OFICINA 4: SUSPENSA (Inadimplente) ======
            jdbcTemplate.update("""
                INSERT INTO oficinas (
                    id, cnpj_cpf, tipo_pessoa, razao_social, nome_fantasia, inscricao_estadual, inscricao_municipal,
                    regime_tributario, contato_nome, contato_telefone, contato_email, contato_cargo, logradouro,
                    numero, complemento, bairro, cidade, estado, cep, plano, status, valor_mensalidade,
                    data_assinatura, data_inicio_trial, data_fim_trial, data_vencimento_plano,
                    created_at, updated_at
                ) VALUES (
                    '0f444444-4444-4444-4444-444444444444',
                    '22233344000166', 'JURIDICA', 'Oficina Central Express ME', 'Central Express',
                    '223344556', '667788990', 'MEI',
                    'Ana Carolina', '1166668888', 'contato@centralexpress.com.br', 'Proprietária',
                    'Av. Industrial', '1500', 'Galpão 5', 'Barra Funda', 'São Paulo', 'SP', '01138000',
                    'PROFISSIONAL', 'SUSPENSA', 199.90,
                    CURRENT_DATE - 90, CURRENT_DATE - 90, CURRENT_DATE - 60, CURRENT_DATE - 20,
                    CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '5 days'
                )
                ON CONFLICT (cnpj_cpf) DO NOTHING
                """);

            // Admin for Oficina 4
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, oficina_id, nome, email, senha, perfil, ativo, created_at, updated_at) VALUES
                ('a4444444-4444-4444-4444-444444444444', '0f444444-4444-4444-4444-444444444444',
                 'Ana Carolina Souza', 'admin.express@pitstop.com', ?, 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            // ====== PAGAMENTOS SAAS ======
            // Oficina 1 (Auto Center SP) - 3 pagamentos (todos em dia)
            jdbcTemplate.update("""
                INSERT INTO saas_pagamentos (id, oficina_id, mes_referencia, valor, data_pagamento, data_vencimento, forma_pagamento, observacao, created_at)
                VALUES
                ('p1111111-1111-1111-1111-111111111111', '0f111111-1111-1111-1111-111111111111',
                 '2025-09', 199.90, '2025-10-08', '2025-10-10', 'PIX', 'Pagamento em dia', CURRENT_TIMESTAMP - INTERVAL '75 days'),
                ('p1111111-1111-1111-1111-111111111112', '0f111111-1111-1111-1111-111111111111',
                 '2025-10', 199.90, '2025-11-09', '2025-11-10', 'Cartão de Crédito', NULL, CURRENT_TIMESTAMP - INTERVAL '42 days'),
                ('p1111111-1111-1111-1111-111111111113', '0f111111-1111-1111-1111-111111111111',
                 '2025-11', 199.90, '2025-12-10', '2025-12-10', 'PIX', NULL, CURRENT_TIMESTAMP - INTERVAL '11 days')
                ON CONFLICT (id) DO NOTHING
                """);

            // Oficina 2 (Mecânica Premium) - 4 pagamentos (1 atrasado)
            jdbcTemplate.update("""
                INSERT INTO saas_pagamentos (id, oficina_id, mes_referencia, valor, data_pagamento, data_vencimento, forma_pagamento, observacao, created_at)
                VALUES
                ('p2222222-2222-2222-2222-222222222221', '0f222222-2222-2222-2222-222222222222',
                 '2025-08', 399.90, '2025-09-10', '2025-09-10', 'Boleto', NULL, CURRENT_TIMESTAMP - INTERVAL '103 days'),
                ('p2222222-2222-2222-2222-222222222222', '0f222222-2222-2222-2222-222222222222',
                 '2025-09', 399.90, '2025-10-15', '2025-10-10', 'PIX', 'Pagamento com 5 dias de atraso', CURRENT_TIMESTAMP - INTERVAL '67 days'),
                ('p2222222-2222-2222-2222-222222222223', '0f222222-2222-2222-2222-222222222222',
                 '2025-10', 399.90, '2025-11-08', '2025-11-10', 'Transferência', NULL, CURRENT_TIMESTAMP - INTERVAL '43 days'),
                ('p2222222-2222-2222-2222-222222222224', '0f222222-2222-2222-2222-222222222222',
                 '2025-11', 399.90, '2025-12-09', '2025-12-10', 'PIX', NULL, CURRENT_TIMESTAMP - INTERVAL '12 days')
                ON CONFLICT (id) DO NOTHING
                """);

            // Oficina 4 (Central Express - SUSPENSA) - 2 pagamentos antigos, depois inadimplência
            jdbcTemplate.update("""
                INSERT INTO saas_pagamentos (id, oficina_id, mes_referencia, valor, data_pagamento, data_vencimento, forma_pagamento, observacao, created_at)
                VALUES
                ('p4444444-4444-4444-4444-444444444441', '0f444444-4444-4444-4444-444444444444',
                 '2025-08', 199.90, '2025-09-10', '2025-09-10', 'PIX', 'Primeiro pagamento', CURRENT_TIMESTAMP - INTERVAL '103 days'),
                ('p4444444-4444-4444-4444-444444444442', '0f444444-4444-4444-4444-444444444444',
                 '2025-09', 199.90, '2025-10-10', '2025-10-10', 'Boleto', NULL, CURRENT_TIMESTAMP - INTERVAL '72 days')
                ON CONFLICT (id) DO NOTHING
                """);
            // Nota: Oficina 4 não tem pagamento de outubro/novembro (motivo da suspensão)

            // ====== AUDIT LOGS (simulando ações do SUPER_ADMIN) ======
            jdbcTemplate.update("""
                INSERT INTO audit_logs (id, acao, entidade, entidade_id, usuario_email, detalhes, ip_address, user_agent, timestamp)
                VALUES
                ('l0000001-0001-0001-0001-000000000001', 'CREATE_OFICINA', 'Oficina', '0f333333-3333-3333-3333-333333333333',
                 'superadmin@pitstop.com', 'Created workshop: AutoPeças Veloz (CNPJ: 11122233000155, Plan: BASICO)',
                 '192.168.1.100', 'Mozilla/5.0', CURRENT_TIMESTAMP - INTERVAL '10 days'),

                ('l0000002-0002-0002-0002-000000000002', 'REGISTER_PAYMENT', 'SaasPagamento', 'p1111111-1111-1111-1111-111111111111',
                 'superadmin@pitstop.com', 'Registered payment for Auto Center SP - 2025-09: R$ 199.90',
                 '192.168.1.100', 'Mozilla/5.0', CURRENT_TIMESTAMP - INTERVAL '75 days'),

                ('l0000003-0003-0003-0003-000000000003', 'REGISTER_PAYMENT', 'SaasPagamento', 'p2222222-2222-2222-2222-222222222222',
                 'superadmin@pitstop.com', 'Registered payment for Mecânica Premium - 2025-09: R$ 399.90',
                 '192.168.1.101', 'Mozilla/5.0', CURRENT_TIMESTAMP - INTERVAL '67 days'),

                ('l0000004-0004-0004-0004-000000000004', 'SUSPEND_OFICINA', 'Oficina', '0f444444-4444-4444-4444-444444444444',
                 'superadmin@pitstop.com', 'Suspended workshop: Central Express',
                 '192.168.1.100', 'Mozilla/5.0', CURRENT_TIMESTAMP - INTERVAL '5 days'),

                ('l0000005-0005-0005-0005-000000000005', 'UPDATE_OFICINA', 'Oficina', '0f222222-2222-2222-2222-222222222222',
                 'superadmin@pitstop.com', 'Updated workshop: Mecânica Premium (Plan changed to: TURBINADO)',
                 '192.168.1.100', 'Mozilla/5.0', CURRENT_TIMESTAMP - INTERVAL '30 days')
                ON CONFLICT (id) DO NOTHING
                """);

            // Contar registros criados
            Long totalOficinas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oficinas WHERE id IN ('0f333333-3333-3333-3333-333333333333', '0f444444-4444-4444-4444-444444444444')",
                Long.class
            );
            Long totalPagamentos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM saas_pagamentos",
                Long.class
            );
            Long totalAuditLogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE usuario_email = 'superadmin@pitstop.com'",
                Long.class
            );

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "success");
            resultado.put("mensagem", "Dados SaaS populados com sucesso!");
            resultado.put("oficinasCriadas", totalOficinas);
            resultado.put("totalPagamentos", totalPagamentos);
            resultado.put("totalAuditLogs", totalAuditLogs);
            resultado.put("resumo", Map.of(
                "Oficina TRIAL", "AutoPeças Veloz (20 dias restantes)",
                "Oficina ATIVA", "Auto Center SP e Mecânica Premium (com pagamentos em dia)",
                "Oficina SUSPENSA", "Central Express (inadimplente há 20 dias)",
                "Credenciais SUPER_ADMIN", "superadmin@pitstop.com / SuperSecure2025!"
            ));

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("status", "error");
            erro.put("mensagem", e.getMessage());
            return ResponseEntity.internalServerError().body(erro);
        }
    }
}
