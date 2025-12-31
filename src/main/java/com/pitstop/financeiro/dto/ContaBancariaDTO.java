package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.TipoChavePix;
import com.pitstop.oficina.domain.TipoConta;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para representar conta bancária.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaBancariaDTO {
    private UUID id;
    private String nome;
    private String codigoBanco;
    private String banco;
    private String agencia;
    private String digitoAgencia;
    private String conta;
    private String digitoConta;
    private TipoConta tipoConta;
    private String tipoContaDescricao;
    private String titular;
    private String cpfCnpjTitular;

    // PIX
    private TipoChavePix tipoChavePix;
    private String tipoChavePixDescricao;
    private String chavePix;
    private String nomeBeneficiarioPix;
    private String cidadeBeneficiarioPix;

    // Configurações
    private Boolean padrao;
    private Boolean ativo;
    private String finalidade;
    private String observacoes;

    // Dados formatados
    private String dadosFormatados;
    private Boolean temPix;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
