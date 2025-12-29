package com.pitstop.saas.dto;

import com.pitstop.saas.domain.Fatura;
import com.pitstop.saas.domain.StatusFatura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Complete DTO for invoice details.
 */
public record FaturaDTO(
    UUID id,
    String numero,

    // Workshop info
    UUID oficinaId,
    String oficinaNome,
    String oficinaCnpj,
    String oficinaEmail,

    // Plan info
    String planoCodigo,

    // Status
    StatusFatura status,
    String statusLabel,
    String statusCor,

    // Values
    BigDecimal valorBase,
    BigDecimal valorDesconto,
    BigDecimal valorAcrescimos,
    BigDecimal valorTotal,

    // Dates
    LocalDate mesReferencia,
    String mesReferenciaFormatado,
    LocalDate dataEmissao,
    LocalDate dataVencimento,
    LocalDateTime dataPagamento,
    Long diasAteVencimento,

    // Payment info
    String metodoPagamento,
    String transacaoId,
    String qrCodePix,
    String linkPagamento,

    // Notes
    String observacao,
    Integer tentativasCobranca,

    // Items
    List<ItemFaturaDTO> itens,

    // Flags
    boolean vencida,
    boolean pagavel,
    boolean cancelavel,

    // Audit
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Convert entity to DTO.
     */
    public static FaturaDTO fromEntity(Fatura fatura) {
        return new FaturaDTO(
            fatura.getId(),
            fatura.getNumero(),
            fatura.getOficina().getId(),
            fatura.getOficina().getNomeFantasia(),
            fatura.getOficina().getCnpjCpf(),
            fatura.getOficina().getContato() != null ? fatura.getOficina().getContato().getEmail() : null,
            fatura.getPlanoCodigo(),
            fatura.getStatus(),
            fatura.getStatus().getLabel(),
            fatura.getStatus().getCor(),
            fatura.getValorBase(),
            fatura.getValorDesconto(),
            fatura.getValorAcrescimos(),
            fatura.getValorTotal(),
            fatura.getMesReferencia(),
            fatura.getMesReferenciaFormatado(),
            fatura.getDataEmissao(),
            fatura.getDataVencimento(),
            fatura.getDataPagamento(),
            fatura.getDiasAteVencimento(),
            fatura.getMetodoPagamento(),
            fatura.getTransacaoId(),
            fatura.getQrCodePix(),
            fatura.getLinkPagamento(),
            fatura.getObservacao(),
            fatura.getTentativasCobranca(),
            fatura.getItens().stream().map(ItemFaturaDTO::fromEntity).toList(),
            fatura.isVencida(),
            fatura.isPagavel(),
            fatura.isCancelavel(),
            fatura.getCreatedAt(),
            fatura.getUpdatedAt()
        );
    }
}
