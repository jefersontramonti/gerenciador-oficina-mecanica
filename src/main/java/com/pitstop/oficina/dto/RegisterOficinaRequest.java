package com.pitstop.oficina.dto;

import com.pitstop.oficina.domain.TipoPessoa;
import jakarta.validation.constraints.*;

/**
 * Request DTO for oficina registration.
 * Contains all necessary data to register a new workshop including:
 * - Company/owner data (CNPJ/CPF, business names)
 * - Contact information
 * - Address details
 * - First admin user credentials
 *
 * This is used in the public registration endpoint (/api/public/oficinas/register)
 *
 * @param cnpjCpf Company CNPJ (14 digits) or owner CPF (11 digits) - must be unique
 * @param tipoPessoa Type of person: FISICA (individual) or JURIDICA (company)
 * @param razaoSocial Legal company name or full individual name
 * @param nomeFantasia Trade name or display name
 * @param inscricaoEstadual State registration number (optional, only for companies)
 * @param inscricaoMunicipal Municipal registration number (optional, only for companies)
 * @param contatoNome Primary contact person name
 * @param contatoTelefone Primary contact phone number (format: (XX) XXXXX-XXXX or (XX) XXXX-XXXX)
 * @param contatoEmail Primary contact email address
 * @param contatoCargo Primary contact job title (optional)
 * @param logradouro Street address
 * @param numero Street number
 * @param complemento Address complement (apartment, suite, etc.) - optional
 * @param bairro Neighborhood/district
 * @param cidade City
 * @param estado State (2-letter code: SP, RJ, etc.)
 * @param cep Postal code (format: XXXXX-XXX)
 * @param adminNome First admin user full name
 * @param adminEmail First admin user email (will be used for login)
 * @param adminSenha First admin user password (minimum 8 characters)
 *
 * @author PitStop Development Team
 * @version 1.0
 * @since 2025-12-21
 */
public record RegisterOficinaRequest(

    // Oficina business data
    @NotBlank(message = "CNPJ/CPF é obrigatório")
    @Pattern(
        regexp = "^\\d{11}$|^\\d{14}$",
        message = "CNPJ/CPF deve conter 11 dígitos (CPF) ou 14 dígitos (CNPJ), apenas números"
    )
    String cnpjCpf,

    @NotNull(message = "Tipo de pessoa é obrigatório")
    TipoPessoa tipoPessoa,

    @NotBlank(message = "Razão social é obrigatória")
    @Size(min = 3, max = 200, message = "Razão social deve ter entre 3 e 200 caracteres")
    String razaoSocial,

    @NotBlank(message = "Nome fantasia é obrigatório")
    @Size(min = 3, max = 100, message = "Nome fantasia deve ter entre 3 e 100 caracteres")
    String nomeFantasia,

    @Size(max = 20, message = "Inscrição estadual deve ter no máximo 20 caracteres")
    String inscricaoEstadual,

    @Size(max = 20, message = "Inscrição municipal deve ter no máximo 20 caracteres")
    String inscricaoMunicipal,

    // Contact information
    @NotBlank(message = "Nome do contato é obrigatório")
    @Size(min = 3, max = 150, message = "Nome do contato deve ter entre 3 e 150 caracteres")
    String contatoNome,

    @NotBlank(message = "Telefone do contato é obrigatório")
    @Pattern(
        regexp = "^\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$",
        message = "Telefone inválido. Use o formato: (XX) XXXXX-XXXX ou (XX) XXXX-XXXX"
    )
    String contatoTelefone,

    @NotBlank(message = "Email do contato é obrigatório")
    @Email(message = "Email do contato inválido")
    @Size(max = 150, message = "Email do contato deve ter no máximo 150 caracteres")
    String contatoEmail,

    @Size(max = 100, message = "Cargo do contato deve ter no máximo 100 caracteres")
    String contatoCargo,

    // Address information
    @NotBlank(message = "Logradouro é obrigatório")
    @Size(min = 3, max = 200, message = "Logradouro deve ter entre 3 e 200 caracteres")
    String logradouro,

    @NotBlank(message = "Número é obrigatório")
    @Size(min = 1, max = 10, message = "Número deve ter entre 1 e 10 caracteres")
    String numero,

    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    String complemento,

    @NotBlank(message = "Bairro é obrigatório")
    @Size(min = 2, max = 100, message = "Bairro deve ter entre 2 e 100 caracteres")
    String bairro,

    @NotBlank(message = "Cidade é obrigatória")
    @Size(min = 2, max = 100, message = "Cidade deve ter entre 2 e 100 caracteres")
    String cidade,

    @NotBlank(message = "Estado é obrigatório")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Estado deve ter 2 letras maiúsculas (ex: SP, RJ)")
    String estado,

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido. Use o formato: XXXXX-XXX")
    String cep,

    // Admin user credentials
    @NotBlank(message = "Nome do administrador é obrigatório")
    @Size(min = 3, max = 150, message = "Nome do administrador deve ter entre 3 e 150 caracteres")
    String adminNome,

    @NotBlank(message = "Email do administrador é obrigatório")
    @Email(message = "Email do administrador inválido")
    @Size(max = 150, message = "Email do administrador deve ter no máximo 150 caracteres")
    String adminEmail,

    @NotBlank(message = "Senha do administrador é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    String adminSenha
) {
}
