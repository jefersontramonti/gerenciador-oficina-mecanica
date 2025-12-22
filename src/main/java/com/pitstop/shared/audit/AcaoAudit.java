package com.pitstop.shared.audit;

/**
 * Enum representing auditable actions in the Sa

aS system.
 * Used for legal defense and compliance.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public enum AcaoAudit {
    // Oficinas
    CRIAR_OFICINA("Criar oficina"),
    EDITAR_OFICINA("Editar oficina"),
    ATIVAR_OFICINA("Ativar oficina"),
    SUSPENDER_OFICINA("Suspender oficina"),
    CANCELAR_OFICINA("Cancelar oficina"),
    ALTERAR_PLANO("Alterar plano de assinatura"),

    // Pagamentos
    REGISTRAR_PAGAMENTO("Registrar pagamento"),
    ESTORNAR_PAGAMENTO("Estornar pagamento"),

    // Usuários
    CRIAR_USUARIO_ADMIN("Criar usuário administrador"),
    DESATIVAR_USUARIO("Desativar usuário"),
    ALTERAR_SENHA("Alterar senha de usuário"),

    // Sistema
    LOGIN_SUPER_ADMIN("Login de Super Administrador"),
    LOGOUT_SUPER_ADMIN("Logout de Super Administrador"),
    ACESSO_NEGADO("Tentativa de acesso não autorizado");

    private final String descricao;

    AcaoAudit(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
