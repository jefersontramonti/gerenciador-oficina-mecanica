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

            // First, delete any existing admin user to ensure clean state
            jdbcTemplate.update("DELETE FROM usuarios WHERE email = 'admin@pitstop.com'");

            // Then insert the new admin user
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('a0000000-0000-0000-0000-000000000001', 'Administrador do Sistema', 'admin@pitstop.com', ?, 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                """, senhaHashAdmin);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "success");
            resultado.put("mensagem", "Usuário ADMIN criado com sucesso!");
            resultado.put("credenciais", "admin@pitstop.com / admin123");

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

            // ====== USUÁRIOS ======
            // ADMIN (senha: admin123)
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('a0000000-0000-0000-0000-000000000001', 'Administrador do Sistema', 'admin@pitstop.com', ?, 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHashAdmin);

            // GERENTE (senha: senha123)
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'Carlos Henrique Silva', 'gerente@pitstop.com', ?, 'GERENTE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            jdbcTemplate.update("""
                INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('e58ed763-928c-4155-bee9-fdbaaadc15f3', 'Maria Santos Oliveira', 'atendente@pitstop.com', ?, 'ATENDENTE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            jdbcTemplate.update("""
                INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('d364f5a2-5c1a-4c3e-8f9b-1e4d2c8a6b3c', 'João Pedro Almeida', 'mecanico@pitstop.com', ?, 'MECANICO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            jdbcTemplate.update("""
                INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
                ('c273e4d1-4b0a-3b2e-7e8a-0d3c1b7a5a2b', 'Roberto Costa Lima', 'mecanico2@pitstop.com', ?, 'MECANICO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO NOTHING
                """, senhaHash);

            // ====== CLIENTES - PESSOAS FÍSICAS ======
            String[] clientesPF = {
                "('11111111-1111-1111-1111-111111111111', 'PESSOA_FISICA', 'José da Silva', '12345678901', 'jose.silva@email.com', '1133334444', '11987654321', 'Rua das Flores', '123', 'Apto 101', 'Centro', 'São Paulo', 'SP', '01310100', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('22222222-2222-2222-2222-222222222222', 'PESSOA_FISICA', 'Ana Paula Souza', '23456789012', 'ana.souza@email.com', NULL, '11976543210', 'Av. Paulista', '1500', NULL, 'Bela Vista', 'São Paulo', 'SP', '01310200', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('33333333-3333-3333-3333-333333333333', 'PESSOA_FISICA', 'Carlos Eduardo Mendes', '34567890123', 'carlos.mendes@email.com', '1144445555', '11965432109', 'Rua Augusta', '789', 'Casa', 'Consolação', 'São Paulo', 'SP', '01305000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('44444444-4444-4444-4444-444444444444', 'PESSOA_FISICA', 'Mariana Costa Lima', '45678901234', 'mariana.lima@email.com', NULL, '11954321098', 'Rua Oscar Freire', '456', 'Apto 502', 'Jardins', 'São Paulo', 'SP', '01426001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('55555555-5555-5555-5555-555555555555', 'PESSOA_FISICA', 'Roberto Alves Santos', '56789012345', 'roberto.santos@email.com', '1155556666', '11943210987', 'Av. Brigadeiro Faria Lima', '2000', 'Conj 801', 'Pinheiros', 'São Paulo', 'SP', '01452000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('66666666-6666-6666-6666-666666666666', 'PESSOA_FISICA', 'Fernanda Rodrigues', '67890123456', 'fernanda.rodrigues@email.com', NULL, '11932109876', 'Rua Haddock Lobo', '321', NULL, 'Cerqueira César', 'São Paulo', 'SP', '01414001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('77777777-7777-7777-7777-777777777777', 'PESSOA_FISICA', 'Lucas Ferreira', '78901234567', 'lucas.ferreira@email.com', '1166667777', '11921098765', 'Rua da Consolação', '567', 'Apto 302', 'Consolação', 'São Paulo', 'SP', '01301000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('88888888-8888-8888-8888-888888888888', 'PESSOA_FISICA', 'Juliana Martins', '89012345678', 'juliana.martins@email.com', NULL, '11910987654', 'Av. Rebouças', '1234', 'Bloco B', 'Pinheiros', 'São Paulo', 'SP', '05402000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            };

            for (String cliente : clientesPF) {
                jdbcTemplate.update("""
                    INSERT INTO clientes (id, tipo, nome, cpf_cnpj, email, telefone, celular, logradouro, numero, complemento, bairro, cidade, estado, cep, ativo, created_at, updated_at) VALUES
                    """ + cliente + " ON CONFLICT (cpf_cnpj) DO NOTHING");
            }

            // ====== CLIENTES - PESSOAS JURÍDICAS ======
            String[] clientesPJ = {
                "('99999999-9999-9999-9999-999999999999', 'PESSOA_JURIDICA', 'Transportadora RápidaLog Ltda', '12345678000190', 'contato@rapidalog.com.br', '1130001000', '11999887766', 'Av. dos Bandeirantes', '500', 'Galpão 3', 'Vila Mariana', 'São Paulo', 'SP', '04071000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'PESSOA_JURIDICA', 'Distribuidora Alimentos ABC S.A.', '23456789000191', 'financeiro@alimentosabc.com.br', '1131002000', '11988776655', 'Rua Vergueiro', '2500', NULL, 'Vila Mariana', 'São Paulo', 'SP', '04101000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'PESSOA_JURIDICA', 'Tech Solutions Informática ME', '34567890000192', 'ti@techsolutions.com.br', '1132003000', NULL, 'Av. Faria Lima', '3500', 'Sala 1205', 'Itaim Bibi', 'São Paulo', 'SP', '04538133', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('cccccccc-cccc-cccc-cccc-cccccccccccc', 'PESSOA_JURIDICA', 'Construtora Horizonte Ltda', '45678901000193', 'obras@horizonteconstrutora.com', '1133004000', '11977665544', 'Rua Funchal', '418', '10º andar', 'Vila Olímpia', 'São Paulo', 'SP', '04551060', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            };

            for (String cliente : clientesPJ) {
                jdbcTemplate.update("""
                    INSERT INTO clientes (id, tipo, nome, cpf_cnpj, email, telefone, celular, logradouro, numero, complemento, bairro, cidade, estado, cep, ativo, created_at, updated_at) VALUES
                    """ + cliente + " ON CONFLICT (cpf_cnpj) DO NOTHING");
            }

            // ====== VEÍCULOS ======
            String[] veiculos = {
                // José da Silva
                "('v1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'ABC1234', 'Volkswagen', 'Gol', 2018, 'Prata', '9BWAA45U0JP123456', 85000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v1111111-1111-1111-1111-111111111112', '11111111-1111-1111-1111-111111111111', 'DEF5678', 'Fiat', 'Uno', 2015, 'Branco', '9BD15842J28123457', 120000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Ana Paula Souza
                "('v2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'BRA2C34', 'Honda', 'Civic', 2020, 'Preto', '19XFC2F53LE123458', 45000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Carlos Eduardo Mendes
                "('v3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'XYZ9876', 'Toyota', 'Corolla', 2019, 'Branco', '9BR53ZEC7K0123459', 62000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v3333333-3333-3333-3333-333333333334', '33333333-3333-3333-3333-333333333333', 'GHI3456', 'Chevrolet', 'Onix', 2021, 'Vermelho', '9BGKS69J0MG123460', 35000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Mariana Costa Lima
                "('v4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'JKL7890', 'Hyundai', 'HB20', 2022, 'Azul', '9BHBD41JCPK123461', 18000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Roberto Alves Santos
                "('v5555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'MNO1234', 'Jeep', 'Compass', 2021, 'Cinza', '3C4NJDCB4LT123462', 52000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v5555555-5555-5555-5555-555555555556', '55555555-5555-5555-5555-555555555555', 'PQR5678', 'Ford', 'Ranger', 2017, 'Preto', '8AFJZZZ12AJ123463', 95000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Fernanda Rodrigues
                "('v6666666-6666-6666-6666-666666666666', '66666666-6666-6666-6666-666666666666', 'STU9012', 'Renault', 'Sandero', 2020, 'Branco', '93Y5SRD63LJ123464', 48000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Lucas Ferreira
                "('v7777777-7777-7777-7777-777777777777', '77777777-7777-7777-7777-777777777777', 'BRA3E45', 'Nissan', 'Kicks', 2023, 'Vermelho', '3N1CN7AP3PL123465', 12000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Juliana Martins
                "('v8888888-8888-8888-8888-888888888888', '88888888-8888-8888-8888-888888888888', 'VWX3456', 'Peugeot', '208', 2019, 'Cinza', '9362PKF5WKP123466', 71000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Frota Transportadora
                "('v9999999-9999-9999-9999-999999999991', '99999999-9999-9999-9999-999999999999', 'YZA7890', 'Mercedes-Benz', 'Sprinter', 2020, 'Branco', 'WDB9066331N123467', 125000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v9999999-9999-9999-9999-999999999992', '99999999-9999-9999-9999-999999999999', 'BCD1234', 'Iveco', 'Daily', 2019, 'Branco', 'ZCFC35A0705123468', 145000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('v9999999-9999-9999-9999-999999999993', '99999999-9999-9999-9999-999999999999', 'EFG5678', 'Ford', 'Cargo', 2018, 'Branco', '9BFW4RHA5JB123469', 180000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Frota Distribuidora
                "('vaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'HIJ9012', 'Volkswagen', 'Delivery', 2021, 'Branco', '9532832V0MR123470', 98000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('vaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'BRA4F56', 'Mercedes-Benz', 'Accelo', 2020, 'Branco', 'WDB9603381K123471', 110000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Frota Tech Solutions
                "('vbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'KLM3456', 'Chevrolet', 'Spin', 2022, 'Prata', '9BGRB48A0N0123472', 32000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('vbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'NOP7890', 'Fiat', 'Fiorino', 2019, 'Branco', '9BD25544KF0123473', 88000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                // Frota Construtora
                "('vcccccccc-cccc-cccc-cccc-cccccccccc01', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'QRS1234', 'Toyota', 'Hilux', 2021, 'Branco', '8AJFH22G8L4123474', 67000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('vcccccccc-cccc-cccc-cccc-cccccccccc02', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'TUV5678', 'Mitsubishi', 'L200', 2020, 'Prata', '93XBN1CJXLE123475', 79000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "('vcccccccc-cccc-cccc-cccc-cccccccccc03', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'BRA5G67', 'Ford', 'F-250', 2019, 'Preto', '1FT7W2B68KEE123476', 102000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            };

            for (String veiculo : veiculos) {
                jdbcTemplate.update("""
                    INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, ano, cor, chassi, quilometragem, created_at, updated_at) VALUES
                    """ + veiculo + " ON CONFLICT (placa) DO NOTHING");
            }

            // Contar registros inseridos
            Long totalUsuarios = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
            Long totalClientes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clientes", Long.class);
            Long totalVeiculos = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM veiculos", Long.class);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "success");
            resultado.put("mensagem", "Banco de dados populado com sucesso!");
            resultado.put("totalUsuarios", totalUsuarios);
            resultado.put("totalClientes", totalClientes);
            resultado.put("totalVeiculos", totalVeiculos);
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
}
