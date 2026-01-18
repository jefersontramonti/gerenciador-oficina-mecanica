package com.pitstop.cliente.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

/**
 * Value Object representando um endereço brasileiro completo.
 * Implementado como {@link Embeddable} para ser incorporado em outras entidades.
 *
 * <p>Este objeto é imutável após criação e deve ser substituído por completo
 * ao invés de modificar seus campos individualmente (conceito de Value Object do DDD).</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Endereco implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Nome da rua, avenida, travessa, etc.
     */
    @Column(name = "logradouro", length = 200)
    @Size(max = 200, message = "Logradouro deve ter no máximo 200 caracteres")
    private String logradouro;

    /**
     * Número do imóvel (ou "S/N" para sem número).
     */
    @Column(name = "numero", length = 10)
    @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
    private String numero;

    /**
     * Informações complementares (apartamento, bloco, sala, etc.).
     */
    @Column(name = "complemento", length = 100)
    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    private String complemento;

    /**
     * Bairro ou distrito.
     */
    @Column(name = "bairro", length = 100)
    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    private String bairro;

    /**
     * Cidade/município.
     */
    @Column(name = "cidade", length = 100)
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    private String cidade;

    /**
     * Sigla do estado brasileiro (UF) - 2 caracteres maiúsculos.
     * Exemplo: SP, RJ, MG
     */
    @Column(name = "estado", length = 2)
    @Pattern(regexp = "^[A-Z]{2}$", message = "Estado deve ser a sigla da UF com 2 letras maiúsculas (ex: SP, RJ)")
    private String estado;

    /**
     * Código de Endereçamento Postal brasileiro no formato 00000-000.
     */
    @Column(name = "cep", length = 9)
    @Pattern(regexp = "^\\d{5}-\\d{3}$", message = "CEP deve estar no formato 00000-000")
    private String cep;

    /**
     * Verifica se o endereço está completo (todos os campos obrigatórios preenchidos).
     *
     * @return true se logradouro, número, bairro, cidade, estado e CEP estão preenchidos
     */
    @JsonIgnore
    public boolean isCompleto() {
        return logradouro != null && !logradouro.isBlank() &&
               numero != null && !numero.isBlank() &&
               bairro != null && !bairro.isBlank() &&
               cidade != null && !cidade.isBlank() &&
               estado != null && !estado.isBlank() &&
               cep != null && !cep.isBlank();
    }

    /**
     * Retorna o endereço formatado em uma única linha.
     * Formato: "Logradouro, Número - Bairro, Cidade/UF, CEP"
     *
     * @return endereço formatado ou string vazia se endereço incompleto
     */
    @JsonIgnore
    public String getEnderecoFormatado() {
        if (!isCompleto()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(logradouro).append(", ").append(numero);

        if (complemento != null && !complemento.isBlank()) {
            sb.append(" - ").append(complemento);
        }

        sb.append(" - ").append(bairro)
          .append(", ").append(cidade)
          .append("/").append(estado)
          .append(", CEP: ").append(cep);

        return sb.toString();
    }
}
