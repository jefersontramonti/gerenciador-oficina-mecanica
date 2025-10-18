package com.pitstop.usuario.domain;

/**
 * Enum representando os perfis (roles) de usuário no sistema PitStop.
 *
 * Hierarquia de permissões (do mais restrito ao mais permissivo):
 * - MECANICO: Acesso apenas a suas ordens de serviço atribuídas
 * - ATENDENTE: CRUD de clientes, veículos, OS, visualização de estoque
 * - GERENTE: Todas as permissões de ATENDENTE + relatórios financeiros + aprovação de descontos
 * - ADMIN: Acesso total ao sistema incluindo gestão de usuários
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
     * Administrador - Acesso total incluindo gestão de usuários.
     */
    ADMIN("Administrador", 4);

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
}
