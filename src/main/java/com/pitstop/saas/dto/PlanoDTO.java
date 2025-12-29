package com.pitstop.saas.dto;

import com.pitstop.saas.domain.Plano;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Plano entity - Full representation for SUPER_ADMIN.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanoDTO {

    private UUID id;

    // Basic Info
    private String codigo;
    private String nome;
    private String descricao;

    // Pricing
    private BigDecimal valorMensal;
    private BigDecimal valorAnual;
    private Integer trialDias;
    private BigDecimal descontoAnual;  // Calculated field

    // Limits
    private Integer limiteUsuarios;
    private Integer limiteOsMes;
    private Integer limiteClientes;
    private Long limiteEspacoMb;
    private Integer limiteApiCalls;
    private Integer limiteWhatsappMensagens;
    private Integer limiteEmailsMes;

    // Features
    private Map<String, Boolean> features;

    // Display & Marketing
    private Boolean ativo;
    private Boolean visivel;
    private Boolean recomendado;
    private String corDestaque;
    private String tagPromocao;
    private Integer ordemExibicao;

    // Computed
    private Boolean precoSobConsulta;
    private Boolean usuariosIlimitados;
    private Boolean espacoIlimitado;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Create DTO from entity.
     */
    public static PlanoDTO fromEntity(Plano plano) {
        if (plano == null) {
            return null;
        }

        return PlanoDTO.builder()
                .id(plano.getId())
                .codigo(plano.getCodigo())
                .nome(plano.getNome())
                .descricao(plano.getDescricao())
                .valorMensal(plano.getValorMensal())
                .valorAnual(plano.getValorAnual())
                .trialDias(plano.getTrialDias())
                .descontoAnual(plano.getDescontoAnual())
                .limiteUsuarios(plano.getLimiteUsuarios())
                .limiteOsMes(plano.getLimiteOsMes())
                .limiteClientes(plano.getLimiteClientes())
                .limiteEspacoMb(plano.getLimiteEspacoMb())
                .limiteApiCalls(plano.getLimiteApiCalls())
                .limiteWhatsappMensagens(plano.getLimiteWhatsappMensagens())
                .limiteEmailsMes(plano.getLimiteEmailsMes())
                .features(plano.getFeatures())
                .ativo(plano.getAtivo())
                .visivel(plano.getVisivel())
                .recomendado(plano.getRecomendado())
                .corDestaque(plano.getCorDestaque())
                .tagPromocao(plano.getTagPromocao())
                .ordemExibicao(plano.getOrdemExibicao())
                .precoSobConsulta(plano.isPrecoSobConsulta())
                .usuariosIlimitados(plano.isUsuariosIlimitados())
                .espacoIlimitado(plano.isEspacoIlimitado())
                .createdAt(plano.getCreatedAt())
                .updatedAt(plano.getUpdatedAt())
                .build();
    }
}
