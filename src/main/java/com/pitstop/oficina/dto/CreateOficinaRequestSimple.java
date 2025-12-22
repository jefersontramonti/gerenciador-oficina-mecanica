package com.pitstop.oficina.dto;

import com.pitstop.cliente.domain.Endereco;
import com.pitstop.oficina.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO simplificado para criação de oficina (campos básicos obrigatórios).
 */
public record CreateOficinaRequestSimple(
    @NotBlank String nomeFantasia,
    @NotBlank String razaoSocial,
    @NotBlank @Pattern(regexp = "\\d{11,18}") String cnpjCpf,
    @NotNull TipoPessoa tipoPessoa,
    @NotBlank String nomeResponsavel,
    @NotBlank @Email String email,
    @NotBlank String telefone,
    @Valid @NotNull Endereco endereco,
    @NotNull PlanoAssinatura plano,
    RegimeTributario regimeTributario
) {
}
