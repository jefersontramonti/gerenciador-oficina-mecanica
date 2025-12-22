package com.pitstop.usuario.domain;

/**
 * Enum representando os perfis (roles) de usuário no sistema PitStop.
 *
 * Hierarquia de permissões (do mais restrito ao mais permissivo):
 * - MECANICO: Acesso apenas a suas ordens de serviço atribuídas
 * - ATENDENTE: CRUD de clientes, veículos, OS, visualização de estoque
 * - GERENTE: Todas as permissões de ATENDENTE + relatórios financeiros + aprovação de descontos
 * - ADMIN: Acesso total ao sistema incluindo gestão de usuários (dentro de uma oficina)
 * - SUPER_ADMIN: Dono do SaaS - gerencia oficinas, não tem acesso a dados de oficinas individuais
 */
public enum PerfilUsuario {
    /**
     * Mecânico - Visualiza e atualiza apenas suas ordens de serviço.
     */
    MECANICO("Mecânico", 1),

    /**
     * Atendente - Gerencia clientes, veículos e ordens de serviço.
     */
    ATENDENTE("Atendente", 2),

    /**
     * Gerente - Todas as permissões de atendente + relatórios e aprovações.
     */
    GERENTE("Gerente", 3),

    /**
     * Administrador - Acesso total incluindo gestão de usuários (dentro de uma oficina).
     */
    ADMIN("Administrador", 4),

    /**
     * Super Administrador (SaaS Owner) - Gerencia oficinas, planos, pagamentos.
     * NÃO tem acesso aos dados operacionais de oficinas individuais.
     * NÃO possui vínculo com oficina (oficinaId = null).
     */
    SUPER_ADMIN("Super Administrador", 5);

    private final String descricao;
    private final int nivel;

    PerfilUsuario(String descricao, int nivel) {
        this.descricao = descricao;
        this.nivel = nivel;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getNome() {
        return descricao;
    }

    public int getNivel() {
        return nivel;
    }

    /**
     * Verifica se este perfil tem nível igual ou superior ao perfil fornecido.
     *
     * @param perfil Perfil a comparar
     * @return true se este perfil tem permissões iguais ou superiores
     */
    public boolean temNivelSuperiorOuIgual(PerfilUsuario perfil) {
        return this.nivel >= perfil.nivel;
    }

    /**
     * Verifica se este perfil é SUPER_ADMIN (dono do SaaS).
     *
     * @return true se perfil é SUPER_ADMIN
     */
    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    /**
     * Verifica se este perfil pertence a uma oficina (não é SUPER_ADMIN).
     *
     * @return true se perfil está vinculado a uma oficina
     */
    public boolean isOficinaUser() {
        return this != SUPER_ADMIN;
    }
}
