package com.pitstop.veiculo.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.validation.PlacaVeiculo;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um veículo vinculado a um cliente no sistema PitStop.
 *
 * <p>Veículos são cadastrados sob propriedade de clientes e podem ter múltiplas ordens de serviço.
 * A placa é única no sistema e normalizada (uppercase, sem hífen) automaticamente.</p>
 *
 * <p><strong>Regras de Negócio:</strong></p>
 * <ul>
 *   <li>Placa deve ser única no sistema (formato BR antigo ABC1234 ou Mercosul ABC1D23)</li>
 *   <li>Placa é normalizada automaticamente: uppercase e sem hífen</li>
 *   <li>Ano deve estar entre 1900 e ano atual + 1 (permite 0km do próximo ano)</li>
 *   <li>Quilometragem não pode ser negativa</li>
 *   <li>Veículo deve estar vinculado a um cliente (relacionamento obrigatório)</li>
 *   <li>Chassi quando informado deve ter exatamente 17 caracteres (padrão VIN)</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Entity
@Table(
    name = "veiculos",
    indexes = {
        @Index(name = "idx_veiculos_cliente_id", columnList = "cliente_id"),
        @Index(name = "idx_veiculos_placa", columnList = "placa"),
        @Index(name = "idx_veiculos_marca_modelo", columnList = "marca, modelo"),
        @Index(name = "idx_veiculos_created_at", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "placa", "marca", "modelo", "ano"})
public class Veiculo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador único do veículo (UUID v4).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Oficina à qual este veículo pertence (multi-tenant).
     * Preenchida automaticamente via TenantContext no @PrePersist.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    /**
     * Identificador do cliente proprietário do veículo (relacionamento Many-to-One).
     */
    @Column(name = "cliente_id", nullable = false)
    @NotNull(message = "Cliente é obrigatório")
    private UUID clienteId;

    /**
     * Placa do veículo sem hífen (7 caracteres).
     * Formatos aceitos:
     * - BR antigo: ABC1234
     * - Mercosul: ABC1D23
     *
     * <p>Placa é normalizada automaticamente no @PrePersist/@PreUpdate
     * (convertida para uppercase e hífen removido se presente).</p>
     */
    @Column(name = "placa", nullable = false, unique = true, length = 7)
    @NotBlank(message = "Placa é obrigatória")
    @PlacaVeiculo // Custom validator que aceita com ou sem hífen
    private String placa;

    /**
     * Marca do veículo (ex: Volkswagen, Fiat, Toyota).
     */
    @Column(name = "marca", nullable = false, length = 50)
    @NotBlank(message = "Marca é obrigatória")
    @Size(min = 2, max = 50, message = "Marca deve ter entre 2 e 50 caracteres")
    private String marca;

    /**
     * Modelo do veículo (ex: Gol, Uno, Corolla).
     */
    @Column(name = "modelo", nullable = false, length = 100)
    @NotBlank(message = "Modelo é obrigatório")
    @Size(min = 2, max = 100, message = "Modelo deve ter entre 2 e 100 caracteres")
    private String modelo;

    /**
     * Ano de fabricação do veículo.
     * Deve estar entre 1900 e ano atual + 1 (permite cadastrar veículos 0km do próximo ano).
     */
    @Column(name = "ano", nullable = false)
    @NotNull(message = "Ano é obrigatório")
    @Min(value = 1900, message = "Ano deve ser igual ou superior a 1900")
    @Max(value = 2100, message = "Ano inválido") // Validação customizada no @PrePersist usa ano atual + 1
    private Integer ano;

    /**
     * Cor do veículo (opcional).
     */
    @Column(name = "cor", length = 30)
    @Size(max = 30, message = "Cor deve ter no máximo 30 caracteres")
    private String cor;

    /**
     * Número do chassi (VIN - 17 caracteres).
     * Opcional, mas quando informado deve ter exatamente 17 caracteres.
     */
    @Column(name = "chassi", length = 17)
    @Size(min = 17, max = 17, message = "Chassi deve ter exatamente 17 caracteres")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "Chassi inválido (deve conter apenas letras e números, exceto I, O, Q)")
    private String chassi;

    /**
     * Quilometragem atual do veículo.
     * Atualizada a cada ordem de serviço.
     */
    @Column(name = "quilometragem")
    @Min(value = 0, message = "Quilometragem não pode ser negativa")
    @Builder.Default
    private Integer quilometragem = 0;

    /**
     * Data e hora de criação do registro (auditoria).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data e hora da última atualização do registro (auditoria).
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Normaliza placa, valida regras de negócio e configura multi-tenancy antes de persistir.
     */
    @PrePersist
    @PreUpdate
    private void validarENormalizarDados() {
        // Set oficina from TenantContext if not explicitly set (multi-tenancy)
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }

        // Normalizar placa: uppercase e remover hífen
        if (placa != null) {
            this.placa = placa.trim().toUpperCase().replace("-", "");
        }

        // Normalizar chassi: uppercase
        if (chassi != null && !chassi.isBlank()) {
            this.chassi = chassi.trim().toUpperCase();
        }

        // Validar ano (deve estar entre 1900 e ano atual + 1)
        if (ano != null) {
            int anoMaximo = java.time.LocalDate.now().getYear() + 1;
            if (ano < 1900 || ano > anoMaximo) {
                throw new IllegalStateException(
                    String.format("Ano deve estar entre 1900 e %d (ano atual + 1)", anoMaximo)
                );
            }
        }

        // Validar quilometragem
        if (quilometragem != null && quilometragem < 0) {
            throw new IllegalStateException("Quilometragem não pode ser negativa");
        }
    }

    /**
     * Retorna descrição completa do veículo (marca modelo ano).
     *
     * @return string formatada "Marca Modelo Ano" (ex: "Volkswagen Gol 2020")
     */
    public String getDescricaoCompleta() {
        return String.format("%s %s %d", marca, modelo, ano);
    }

    /**
     * Retorna placa formatada para exibição (com hífen).
     * - BR antigo: ABC-1234
     * - Mercosul: ABC-1D23
     *
     * @return placa formatada
     */
    public String getPlacaFormatada() {
        if (placa == null || placa.length() != 7) {
            return placa;
        }
        return placa.substring(0, 3) + "-" + placa.substring(3);
    }

    /**
     * Atualiza quilometragem do veículo.
     * Não permite redução de quilometragem (apenas incremento).
     *
     * @param novaQuilometragem nova quilometragem
     * @throws IllegalArgumentException se nova quilometragem for menor que a atual
     */
    public void atualizarQuilometragem(Integer novaQuilometragem) {
        if (novaQuilometragem == null || novaQuilometragem < 0) {
            throw new IllegalArgumentException("Quilometragem deve ser um valor não negativo");
        }

        if (this.quilometragem != null && novaQuilometragem < this.quilometragem) {
            throw new IllegalArgumentException(
                String.format("Nova quilometragem (%d) não pode ser menor que a atual (%d)",
                    novaQuilometragem, this.quilometragem)
            );
        }

        this.quilometragem = novaQuilometragem;
    }
}
