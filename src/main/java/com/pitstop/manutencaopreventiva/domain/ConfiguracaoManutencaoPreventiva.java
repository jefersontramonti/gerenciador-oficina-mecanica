package com.pitstop.manutencaopreventiva.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Configurações de manutenção preventiva por oficina.
 */
@Entity
@Table(name = "configuracoes_manutencao_preventiva")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "alertasHabilitados"})
public class ConfiguracaoManutencaoPreventiva implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false, unique = true)
    private Oficina oficina;

    @Column(name = "alertas_habilitados", nullable = false)
    @Builder.Default
    private Boolean alertasHabilitados = true;

    @Column(name = "antecedencia_dias_padrao")
    @Builder.Default
    private Integer antecedenciaDiasPadrao = 15;

    @Column(name = "antecedencia_km_padrao")
    @Builder.Default
    private Integer antecedenciaKmPadrao = 1000;

    /** Canais padrão de notificação */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "canais_padrao", columnDefinition = "varchar(100)[]")
    @Builder.Default
    private List<String> canaisPadrao = List.of("WHATSAPP", "EMAIL");

    @Column(name = "horario_envio_alertas")
    @Builder.Default
    private LocalTime horarioEnvioAlertas = LocalTime.of(9, 0);

    @Column(name = "horario_envio_lembretes")
    @Builder.Default
    private LocalTime horarioEnvioLembretes = LocalTime.of(7, 0);

    /** Dias da semana para envio (1=Seg, 7=Dom) */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dias_semana_envio", columnDefinition = "integer[]")
    @Builder.Default
    private List<Integer> diasSemanaEnvio = List.of(1, 2, 3, 4, 5);  // Seg a Sex

    @Column(name = "max_tentativas_alerta")
    @Builder.Default
    private Integer maxTentativasAlerta = 3;

    @Column(name = "intervalo_retry_minutos")
    @Builder.Default
    private Integer intervaloRetryMinutos = 30;

    @Column(name = "token_expira_horas")
    @Builder.Default
    private Integer tokenExpiraHoras = 72;

    /** Templates personalizados para sobrescrever os globais */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "templates_personalizados", columnDefinition = "jsonb")
    private Map<String, String> templatesPersonalizados;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Verifica se pode enviar no dia e horário atual.
     */
    public boolean podeEnviarAgora() {
        if (!alertasHabilitados) {
            return false;
        }

        LocalDateTime agora = LocalDateTime.now();
        int diaSemana = agora.getDayOfWeek().getValue();

        return diasSemanaEnvio.contains(diaSemana);
    }

    /**
     * Retorna o template personalizado ou null se usar padrão.
     */
    public String getTemplatePersonalizado(String tipoTemplate) {
        if (templatesPersonalizados == null) {
            return null;
        }
        return templatesPersonalizados.get(tipoTemplate);
    }
}
