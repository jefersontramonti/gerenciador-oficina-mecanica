package com.pitstop.oficina.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

/**
 * Value Object representing operational information about the workshop.
 *
 * <p>Contains details about business hours, capacity, and staff.</p>
 *
 * @since 1.0.0
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class InformacoesOperacionais implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Business hours information (e.g., "Seg-Sex: 8h-18h | Sáb: 8h-12h").
     */
    @Column(name = "horario_funcionamento", length = 100)
    @Size(max = 100, message = "Horário de funcionamento deve ter no máximo 100 caracteres")
    private String horarioFuncionamento;

    /**
     * Number of vehicles that can be serviced simultaneously.
     */
    @Column(name = "capacidade_simultanea")
    @Min(value = 1, message = "Capacidade simultânea deve ser no mínimo 1")
    private Integer capacidadeSimultanea;

    /**
     * Total number of employees.
     */
    @Column(name = "numero_funcionarios")
    @Min(value = 0, message = "Número de funcionários não pode ser negativo")
    private Integer numeroFuncionarios;

    /**
     * Number of mechanics specifically.
     */
    @Column(name = "numero_mecanicos")
    @Min(value = 0, message = "Número de mecânicos não pode ser negativo")
    private Integer numeroMecanicos;

    /**
     * Checks if operational information is complete.
     *
     * @return true if at least horarioFuncionamento is filled
     */
    public boolean isCompleto() {
        return horarioFuncionamento != null && !horarioFuncionamento.isBlank();
    }

    /**
     * Checks if staff information is available.
     *
     * @return true if numeroFuncionarios or numeroMecanicos is set
     */
    public boolean temInformacoesPessoal() {
        return (numeroFuncionarios != null && numeroFuncionarios > 0) ||
               (numeroMecanicos != null && numeroMecanicos > 0);
    }
}
