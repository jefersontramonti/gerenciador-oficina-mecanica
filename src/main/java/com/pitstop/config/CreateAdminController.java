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
 * Endpoint temporário para criar usuário ADMIN
 * ⚠️ REMOVER EM PRODUÇÃO!
 */
@RestController
@RequestMapping("/api/admin/create-admin")
public class CreateAdminController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAdmin() {
        try {
            // Hash da senha "admin123"
            String senhaHashAdmin = passwordEncoder.encode("admin123");

            // Criar usuário ADMIN
            jdbcTemplate.update("""
                INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso)
                VALUES ('a0000000-0000-0000-0000-000000000001', 'Administrador do Sistema', 'admin@pitstop.com', ?, 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                ON CONFLICT (email) DO UPDATE SET senha = EXCLUDED.senha, updated_at = CURRENT_TIMESTAMP
                """, senhaHashAdmin);

            // Verificar se foi criado
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE email = 'admin@pitstop.com'",
                Long.class
            );

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "success");
            resultado.put("mensagem", "Usuário ADMIN criado com sucesso!");
            resultado.put("email", "admin@pitstop.com");
            resultado.put("senha", "admin123");
            resultado.put("existe", count > 0);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("status", "error");
            erro.put("mensagem", e.getMessage());
            return ResponseEntity.internalServerError().body(erro);
        }
    }
}
