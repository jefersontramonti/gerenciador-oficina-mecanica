package com.pitstop.saas.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.usuario.domain.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "comunicados_leitura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComunicadoLeitura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comunicado_id", nullable = false)
    private Comunicado comunicado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    @Builder.Default
    private Boolean visualizado = false;

    @Column(name = "data_visualizacao")
    private OffsetDateTime dataVisualizacao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean confirmado = false;

    @Column(name = "data_confirmacao")
    private OffsetDateTime dataConfirmacao;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // Métodos auxiliares
    public void marcarComoVisualizado(Usuario usuario) {
        if (!this.visualizado) {
            this.visualizado = true;
            this.dataVisualizacao = OffsetDateTime.now();
            this.usuario = usuario;
        }
    }

    public void confirmarLeitura() {
        if (!this.confirmado) {
            this.confirmado = true;
            this.dataConfirmacao = OffsetDateTime.now();
        }
    }
}
