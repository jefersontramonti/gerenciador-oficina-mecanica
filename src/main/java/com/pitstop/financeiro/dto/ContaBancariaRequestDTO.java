package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.TipoChavePix;
import com.pitstop.oficina.domain.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para criar/atualizar conta bancária.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaBancariaRequestDTO {

    @NotBlank(message = "Nome da conta é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Size(max = 5, message = "Código do banco deve ter no máximo 5 caracteres")
    private String codigoBanco;

    @NotBlank(message = "Nome do banco é obrigatório")
    @Size(max = 100, message = "Nome do banco deve ter no máximo 100 caracteres")
    private String banco;

    @Size(max = 10, message = "Agência deve ter no máximo 10 caracteres")
    private String agencia;

    @Size(max = 2, message = "Dígito da agência deve ter no máximo 2 caracteres")
    private String digitoAgencia;

    @Size(max = 20, message = "Conta deve ter no máximo 20 caracteres")
    private String conta;

    @Size(max = 2, message = "Dígito da conta deve ter no máximo 2 caracteres")
    private String digitoConta;

    private TipoConta tipoConta;

    @Size(max = 200, message = "Titular deve ter no máximo 200 caracteres")
    private String titular;

    @Size(max = 20, message = "CPF/CNPJ deve ter no máximo 20 caracteres")
    private String cpfCnpjTitular;

    // PIX
    private TipoChavePix tipoChavePix;

    @Size(max = 100, message = "Chave PIX deve ter no máximo 100 caracteres")
    private String chavePix;

    @Size(max = 200)
    private String nomeBeneficiarioPix;

    @Size(max = 100)
    private String cidadeBeneficiarioPix;

    // Configurações
    private Boolean padrao;
    private Boolean ativo;
    private String finalidade;
    private String observacoes;
}
