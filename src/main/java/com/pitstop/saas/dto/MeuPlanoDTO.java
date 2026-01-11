package com.pitstop.saas.dto;

import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO completo para a página "Meu Plano" das oficinas.
 * Contém informações do plano atual, todos os planos disponíveis, e features habilitadas.
 */
public record MeuPlanoDTO(
    // Informações do plano atual
    PlanoInfo planoAtual,

    // Todos os planos disponíveis para comparação
    List<PlanoInfo> todosPlanos,

    // Features habilitadas (com detalhes)
    List<FeatureInfo> featuresHabilitadas,

    // Features do próximo plano (para upsell)
    PlanoInfo proximoPlano,
    List<FeatureInfo> featuresProximoPlano,

    // Resumo
    int totalFeaturesHabilitadas,
    int totalFeaturesDisponiveis
) {

    /**
     * Informações do plano
     */
    public record PlanoInfo(
        String codigo,
        String nome,
        String descricao,
        BigDecimal valorMensal,
        StatusOficina status,
        LocalDate dataVencimento,
        Integer diasRestantesTrial,
        // Limites do plano
        Integer maxUsuarios,
        Integer maxOrdensServico,
        Integer maxClientes,
        boolean usuariosIlimitados,
        // Features do plano
        boolean emiteNotaFiscal,
        boolean whatsappAutomatizado,
        boolean manutencaoPreventiva,
        boolean anexoImagensDocumentos
    ) {}

    /**
     * Informações de uma feature
     */
    public record FeatureInfo(
        String codigo,
        String nome,
        String descricao,
        String categoria,
        boolean habilitada,
        // Para features não habilitadas, indica qual plano libera
        String disponivelNoPlano
    ) {}
}
